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

import com.google.common.util.concurrent.MoreExecutors;
import fi.vm.kapa.sevi.Application;
import fi.vm.kapa.sevi.ontology.dto.ConceptDTO;
import fi.vm.kapa.sevi.ontology.service.OntologyService;
import fi.vm.kapa.sevi.service.commons.ConceptType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest({"server.port=0", "management.port=0"})
public class OntologyResourceIT {

    private static final String PARAM_URI = "uri";
    private static final String SERVICE_BASE = "/sevi-ontology-service/ontology/v1/";

    private String baseUri;

    private static boolean initialized = false;


    @Value("${local.server.port}")
    private int ontologyServicePort;

    @Value("${local.server.host:localhost}")
    private String ontologyServiceHost;

    @Autowired
    private OntologyService service;

    @Before
    public void before() throws InterruptedException {
        baseUri = "http://" + ontologyServiceHost + ":" + ontologyServicePort + SERVICE_BASE;
        service.evictCaches();
        if (!initialized) {
            service.setExecutor(MoreExecutors.newDirectExecutorService());
            service.fetchConcepts();
            initialized = true;
        }
    }

    // UTILS ---------------

    private ConceptDTO findConceptByUri(String conceptUri) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "concept").queryParam(PARAM_URI, conceptUri);
        final URI uri = builder.build().encode().toUri();
        return new RestTemplate().getForObject(uri, ConceptDTO.class);
    }

    private void retrieveAndVerify(URI uri, String ontologyUri, String label) {
        RestTemplate restTemplate = new RestTemplate();
        final ConceptDTO[] ontologies = restTemplate.getForObject(uri, ConceptDTO[].class);
        final List<ConceptDTO> jenaOntologyDTOs = Arrays.asList(ontologies);
        assertTrue("Some results should be found for " + label, jenaOntologyDTOs.size() > 0);
        assertTrue(ontologyUri + " should be a broader concept", jenaOntologyDTOs.stream().anyMatch(dto -> dto.getId().equals(ontologyUri)));
    }

    private List<ConceptDTO> findConcepts(URI uri, ConceptType conceptType, int conceptCount) throws URISyntaxException {
        final ConceptDTO[] ontologies = new RestTemplate().getForObject(uri, ConceptDTO[].class);
        final List<ConceptDTO> jenaOntologyDTOs = Arrays.asList(ontologies);
        assertEquals("There should be " + conceptCount + " " + conceptType.toString() + " concepts from " + uri.toString(), conceptCount, jenaOntologyDTOs.size());
        jenaOntologyDTOs.forEach(dto -> {
            assertNotNull(dto.getLabel());
            assertNotNull(dto.getId());
            assertNotNull(dto.getNotation());
            assertEquals("Concept type should match " + conceptType, conceptType, dto.getConceptType());
        });
        return jenaOntologyDTOs;
    }

    // TESTS --------

    @Test
    public void getsOntologyTypes() throws Exception {
        final URI uri = new URI(baseUri + "types");
        RestTemplate restTemplate = new RestTemplate();
        @SuppressWarnings("unchecked")
        List<ConceptType> types = (List<ConceptType>) restTemplate.getForObject(uri, List.class);
        assertEquals("the number of ontology types should match", types.size(), ConceptType.values().length);
        assertTrue("should include " + ConceptType.JUPO + " type", types.contains(ConceptType.JUPO.toString()));
        assertTrue("should include " + ConceptType.TERO + " type", types.contains(ConceptType.TERO.toString()));
        assertTrue("should include " + ConceptType.TSR + " type", types.contains(ConceptType.TSR.toString()));
        assertTrue("should include " + ConceptType.YSO + " type", types.contains(ConceptType.YSO.toString()));
        assertTrue("should include " + ConceptType.LIITO + " type", types.contains(ConceptType.LIITO.toString()));
        assertTrue("should include " + ConceptType.TARGETGROUP + " type", types.contains(ConceptType.TARGETGROUP.toString()));
        assertTrue("should include " + ConceptType.LIFESITUATION + " type", types.contains(ConceptType.LIFESITUATION.toString()));
        assertTrue("should include " + ConceptType.PTVL + " type", types.contains(ConceptType.PTVL.toString()));
    }

    @Test
    public void getNotFound() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri).queryParam(PARAM_URI, "http://www.yso.fi/onto/jupo/NotRealId");
        final URI uri = builder.build().encode().toUri();
        final ResponseEntity<String> response = new TestRestTemplate().getForEntity(uri, String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void getBroaderConceptsShouldReturnAnEmptyArrayForNonExistentConcept() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "concept/broader").queryParam(PARAM_URI, "NotRealId");
        final URI uri = builder.build().encode().toUri();
        final ResponseEntity<ConceptDTO[]> response = new TestRestTemplate().getForEntity(uri, ConceptDTO[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().length);
    }

    @Test
    public void getBroaderShouldReturnAnEmptyArrayForATopLevelConcept() {
        final String toplevelUri = "http://urn.fi/URN:NBN:fi:au:ptvl:KE4";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "concept/broader").queryParam(PARAM_URI, toplevelUri);
        final URI uri = builder.build().encode().toUri();
        final ResponseEntity<ConceptDTO[]> response = new TestRestTemplate().getForEntity(uri, ConceptDTO[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().length);
    }

    // PTVL -----------------------

    @Test
    public void testFindPtvlConceptByUri() {
        final String originalUri = "http://urn.fi/URN:NBN:fi:au:ptvl:KE4.1";
        final ConceptDTO ontology = findConceptByUri(originalUri);
        assertEquals(originalUri + " should be found", originalUri, ontology.getId());
    }

    @Test
    public void testFindBroaderPtvlConceptsByUri() {
        final String originalUri = "http://urn.fi/URN:NBN:fi:au:ptvl:KE4.1";
        final String broaderUri = "http://urn.fi/URN:NBN:fi:au:ptvl:KE4";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "concept/broader").queryParam(PARAM_URI, originalUri);
        final URI uri = builder.build().encode().toUri();
        retrieveAndVerify(uri, broaderUri, originalUri);
    }

    @Test
    public void testFindNarrowerPtvlConceptsByUri() {
        final String originalUri = "http://urn.fi/URN:NBN:fi:au:ptvl:KE4";
        final String narrowerUri = "http://urn.fi/URN:NBN:fi:au:ptvl:KE4.1";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "concept/narrower").queryParam(PARAM_URI, originalUri);
        final URI uri = builder.build().encode().toUri();
        retrieveAndVerify(uri, narrowerUri, originalUri);
    }

    @Test
    public void testGetPtvlClassifications() throws URISyntaxException {
        final URI uri = new URI(baseUri + "concepts/" + ConceptType.PTVL);
        final int conceptCount = 221;
        findConcepts(uri, ConceptType.PTVL, conceptCount);
    }

    @Test
    public void testGetPtvlServiceClassificationTopLevels() throws URISyntaxException {
        final URI uri = new URI(baseUri + "concepts/" + ConceptType.PTVL + "/toplevel");
        // See this: http://finto.fi/ptvl/fi/page/?uri=http://urn.fi/URN:NBN:fi:au:ptvl:
        // There are 27 top level classifications for the service classes.
        final int conceptCount = 27;
        findConcepts(uri, ConceptType.PTVL, conceptCount);
    }

    @Test
    public void testGetPtvlLifeSituationTopLevels() throws URISyntaxException {
        final URI uri = new URI(baseUri + "concepts/" + ConceptType.LIFESITUATION + "/toplevel");
        // See this: http://finto.fi/ptvl/fi/page/?uri=http://urn.fi/URN:NBN:fi:au:ptvl:KE
        // There are 14 top level classifications for the life situations.
        final int conceptCount = 14;
        findConcepts(uri, ConceptType.LIFESITUATION, conceptCount);
    }

    @Test
    public void testGetPtvlTargetGroupTopLevels() throws URISyntaxException {
        final URI uri = new URI(baseUri + "concepts/" + ConceptType.TARGETGROUP + "/toplevel");
        // See this: http://finto.fi/ptvl/fi/page/?uri=http://urn.fi/URN:NBN:fi:au:ptvl:KR
        // There are 3 top level classifications for the target group.
        final int conceptCount = 3;
        findConcepts(uri, ConceptType.TARGETGROUP, conceptCount);
    }

    // JUPO -----------------------

    @Test
    public void testFindJupoConceptByUri() {
        final String originalUri = "http://www.yso.fi/onto/jupo/p1162";
        final ConceptDTO ontology = findConceptByUri(originalUri);
        assertEquals(originalUri + " should be found", originalUri, ontology.getId());
    }

    @Test
    public void testFindBroaderJupoConceptsByUri() {
        final String originalUri = "http://www.yso.fi/onto/jupo/p1162";
        final String broaderUri = "http://www.yso.fi/onto/jupo/p304";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "concept/broader").queryParam(PARAM_URI, originalUri);
        final URI uri = builder.build().encode().toUri();
        retrieveAndVerify(uri, broaderUri, originalUri);
    }

    @Test
    public void testFindNarrowerJupoConceptsByUri() {
        final String originalUri = "http://www.yso.fi/onto/jupo/p1162";
        final String narrowerUri = "http://www.yso.fi/onto/jupo/p2246";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "concept/narrower").queryParam(PARAM_URI, originalUri);
        final URI uri = builder.build().encode().toUri();
        retrieveAndVerify(uri, narrowerUri, originalUri);
    }

    @Test
    public void testGetJupoConcepts() throws URISyntaxException {
        final URI uri = new URI(baseUri + "concepts/" + ConceptType.JUPO);
        final int conceptCount = 25964;
        findConcepts(uri, ConceptType.JUPO, conceptCount);
    }

    @Test
    public void testGetJupoToplevelConcepts() throws URISyntaxException {
        final URI uri = new URI(baseUri + "concepts/" + ConceptType.JUPO + "/toplevel");
        final int conceptCount = 23;
        findConcepts(uri, ConceptType.JUPO, conceptCount);
    }

}
