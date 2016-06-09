/**
 * The MIT License
 * Copyright (c) 2015 Population Register Centre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.sevi.ontology.resource;

import fi.vm.kapa.sevi.Application;
import fi.vm.kapa.sevi.ontology.dto.ConceptDTO;
import fi.vm.kapa.sevi.ontology.service.OntologyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest({ "server.port=0", "management.port=0" })
public class OntologyResourceDeleteIT {

    private static final String SERVICE_BASE = "/sevi-ontology-service/ontology/v1/";

    @Value("${local.server.port}")
    int port;

    @Autowired
    OntologyService service;
    
    @Before
    public void before() {
        service.fetchConcepts();
    }

    @Test(expected = HttpClientErrorException.class)
    public void deleteAll() throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();

        String baseUri = "http://localhost:" + port + SERVICE_BASE;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri).queryParam("id", "http://www.yso.fi/onto/jupo/p1409");
        URI uri = builder.build().encode().toUri();

        assertNotNull("Some result should be found", restTemplate.getForObject(uri, ConceptDTO.class));
        
        // delete all ontologies
        Boolean value = restTemplate.getForObject(new URI(baseUri + "deleteAll"), Boolean.class);
        assertTrue("Should be true", value);

        // find item, should not found
        builder = UriComponentsBuilder.fromHttpUrl(baseUri).queryParam("id", "http://www.yso.fi/onto/jupo/p1409");
        uri = builder.build().encode().toUri();

        assertNull(restTemplate.getForObject(uri, ConceptDTO.class));
    }
}
