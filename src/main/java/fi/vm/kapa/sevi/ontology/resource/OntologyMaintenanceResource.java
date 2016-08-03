/**
 * The MIT License
 * Copyright (c) 2015 Population Register Centre
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.sevi.ontology.resource;

import fi.vm.kapa.sevi.service.commons.ConceptType;
import fi.vm.kapa.sevi.ontology.exception.OntologyException;
import fi.vm.kapa.sevi.ontology.service.OntologyService;
import fi.vm.kapa.sevi.ontology.service.indexing.IndexMappingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@Api("Maintenance")
@Path("/v1")
public class OntologyMaintenanceResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyMaintenanceResource.class);

    @Inject
    private OntologyService ontologyService;

    @Inject
    private IndexMappingService indexMappingService;

    private ExecutorService executor = Executors.newFixedThreadPool(1);
    
    @PUT
    @Path("/mappings")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates the ontology-related Elasticsearch mappings", response = Boolean.class)
    public boolean putMappings(String mappingJson) {
        return indexMappingService.putMappings(mappingJson);
    }

    @PUT
    @Path("/index")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Indexes the concepts, views and toplevels to Elasticsearch", response = Void.class)
    public void indexConcepts(@Suspended final AsyncResponse asyncResp,
            // The parameters are mainly useful for debugging and testing.
            @DefaultValue("true") @QueryParam("indexAllConcepts") final Boolean indexAllConcepts,
            @DefaultValue("true") @QueryParam("indexTopLevelConcepts") final Boolean indexTopLevelConcepts,
            @DefaultValue("true") @QueryParam("indexViews") final Boolean indexViews) {
        LOGGER.debug("Indexing ontologies...");
        runUpdateAsync(asyncResp, () -> ontologyService.indexConcepts(indexAllConcepts, indexTopLevelConcepts, indexViews));
    }

    @PUT
    @Path("/index/{conceptType}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Indexes the concepts of a specific type to Elasticsearch", response = Void.class)
    public void indexConcepts(@Suspended final AsyncResponse asyncResp,
                              @PathParam("conceptType") String conceptType) {
        LOGGER.debug("Indexing ontologies...");
        ConceptType type = ConceptType.of(conceptType);
        runUpdateAsync(asyncResp, () -> ontologyService.indexConcepts(type));
    }

    @PUT
    @Path("/index/ptvl/toplevels")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Indexes the toplevels of a specific type to Elasticsearch", response = Void.class)
    public void indexPtvlToplevels(@Suspended final AsyncResponse asyncResp) {
        LOGGER.debug("Indexing ptvl toplevels ...");
        runUpdateAsync(asyncResp, () -> ontologyService.indexPtvlToplevels());
    }

    @PUT
    @Path("/index/ptvl/views")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Indexes the views of a specific type to Elasticsearch", response = Void.class)
    public void indexPtvlViews(@Suspended final AsyncResponse asyncResp) {
        LOGGER.debug("Indexing ptvl views ...");
        runUpdateAsync(asyncResp, () -> ontologyService.indexPtvlViews());
    }

    @PUT
    @Path("/fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetches all the ontologies and stores them to Fuseki", response = Void.class)
    public void fetchConcepts(@Suspended final AsyncResponse asyncResp) {
        LOGGER.debug("Fetching ontologies...");
        runUpdateAsync(asyncResp, ontologyService::fetchConcepts);
    }

    @PUT
    @Path("/evict")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Evicts all caches", response = Void.class)
    public void evictCache() {
        LOGGER.debug("Cache eviction requested.");
        ontologyService.evictCaches();
    }

    @PUT
    @Path("/import/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetches all the ontologies, stores them to Fuseki and indexes them to Elasticsearch",
        response = Void.class)
    public void fetchAndIndexConcepts(@Suspended final AsyncResponse asyncResp) {
        LOGGER.debug("Fetching and indexing ontologies...");
        runUpdateAsync(asyncResp, ontologyService::fetchAndIndexConcepts);
    }

    @DELETE
    @Path("/delete/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Deletes all the concepts from Fuseki", response = Void.class)
    public void deleteConcepts(@Suspended final AsyncResponse asyncResp) {
        LOGGER.debug("Deleting all ontologies...");
        runUpdateAsync(asyncResp, ontologyService::deleteOntologies);
    }

    private void runUpdateAsync(AsyncResponse asyncResponse, BooleanSupplier cmd) {
        if (ontologyService.isUpdateOngoing()) {
            LOGGER.info("Cannot process request, an update is already ongoing");
            asyncResponse.resume(Response.status(Response.Status.CONFLICT)
                    .entity(new StatusResponse("An update is already ongoing")).build());
        } else {
            asyncResponse.setTimeoutHandler(resp -> resp.resume(Response.status(Response.Status.ACCEPTED)
                    .entity(new StatusResponse("Operation started")).build()));
            asyncResponse.setTimeout(5, TimeUnit.SECONDS);
            final Runnable runnable = () -> {
                try {
                    final boolean asBoolean = cmd.getAsBoolean();
                    asyncResponse.resume(asBoolean);
                } catch (OntologyException ex) {
                    LOGGER.error("Could not run updateAsync", ex);
                    asyncResponse.resume(ex);
                }
            };
            final RunnableFuture<Void> futureTask = new FutureTask<>(runnable, null);
            executor.execute(futureTask);
        }
    }

    private class StatusResponse {
        @SuppressWarnings("unused")
        private final String status;
        StatusResponse(String status) {
            this.status = status;
        }
    }

}
