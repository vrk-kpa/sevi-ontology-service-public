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

import fi.vm.kapa.sevi.ontology.dto.ConceptDTO;
import fi.vm.kapa.sevi.service.commons.ConceptType;
import fi.vm.kapa.sevi.ontology.service.OntologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Api("Ontology")
@Path("/v1")
public class OntologyResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyResource.class);

    @Inject
    private OntologyService ontologyService;

    @GET
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Lists all the allowed concept types", response = List.class)
    public List<String> getConceptTypes() {
        return Arrays.stream(ConceptType.values()).map(ConceptType::toString).collect(Collectors.toList());
    }

    @GET
    @Path("/concept")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns the concept for a specific URI", response = ConceptDTO.class)
    public ConceptDTO findConcept(@QueryParam("uri") final String uri) {
        LOGGER.debug("Finding concept {}", uri);
        return ontologyService.getConcept(uri);
    }

    /**
     * @deprecated
     * @param label
     * @return
     */
    @GET
    @Path("/concept/label")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns the concept for a specific label", response = ConceptDTO.class)
    // Use search-service API, it is more efficient.
    @Deprecated
    public ConceptDTO findConceptByLabel(@QueryParam("label") final String label) {
        LOGGER.debug("Finding concept {}", label);
        return ontologyService.getConceptByLabel(label);
    }

    @GET
    @Path("/concept/broader")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns all the concepts in the same scheme, which are broader than the given concept",
        response = List.class)
    public List<ConceptDTO> findBroaderConcepts(@QueryParam("uri") final String uri) {
        LOGGER.debug("Finding broader concepts for: {}", uri);
        return ontologyService.findBroaderConcepts(uri);
    }

    @GET
    @Path("/concept/narrower")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns all the concepts in the same scheme, which are one level narrower than the given concept",
        response = List.class)
    public List<ConceptDTO> findNarrowerConcepts(@QueryParam("uri") final String uri) {
        LOGGER.debug("Finding narrower concepts for: {}",  uri);
        return ontologyService.findNarrowerConcepts(uri);
    }

    @GET
    @Path("/concepts/{conceptType}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns all the concepts with the given concept type",
        response = List.class)
    public List<ConceptDTO> findByConceptType(@PathParam("conceptType") String conceptType) {
        ConceptType type = ConceptType.of(conceptType);
        switch (type) {
            case ALL:
                // We won't support fetching all the concepts. This makes the difference between requiring 256 MB of memory against 1024 MB of memory.
                // Fetching all concepts not supported. Use Fuseki directly.
                return null;
            default:
                LOGGER.debug("Finding concepts with type {}", type);
                return ontologyService.findConceptsByType(type);
        }
    }

    @GET
    @Path("/concepts/{conceptType}/toplevel")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns all the top level concept for the given concept type",
        response = List.class)
    public List<ConceptDTO> findConceptTypeToplevels(@PathParam("conceptType") String conceptType) {
        ConceptType type = ConceptType.of(conceptType);
        LOGGER.debug("Finding toplevel concepts with type {}", type);
        return ontologyService.findTopLevelConceptsByType(type);
    }
}
