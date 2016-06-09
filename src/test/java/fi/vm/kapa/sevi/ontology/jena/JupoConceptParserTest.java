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
package fi.vm.kapa.sevi.ontology.jena;

import fi.vm.kapa.sevi.ontology.dto.ConceptDTO;
import fi.vm.kapa.sevi.ontology.jena.ontology.JupoConceptParser;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({QueryExecutionFactory.class, ModelFactory.class, DatasetAccessorFactory.class,
                 UpdateFactory.class, UpdateExecutionFactory.class})
@PowerMockIgnore({"javax.xml.*", "org.apache.xerces.*", "org.xml.*"})
public class JupoConceptParserTest {
    
    private static final int RESULTSET_SIZE = 2;
    
    @InjectMocks
    private final static JupoConceptParser JUPO_CONCEPT_PARSER = new JupoConceptParser();
    
    @InjectMocks
    private final static JenaOntologyFetcher JENA_ONTOLOGY_FETCHER = new JenaOntologyFetcher();
    
    @Mock
    private DatasetAccessor accessor;
    
    @Mock
    private ResourceLoader resourceLoader;
    
    @Mock
    private QueryExecution qexec;
    
    @Mock
    private Model model;
    
    @Mock
    private UpdateProcessor updateProcessor;
    
    @BeforeClass
    public static void init() {
        ReflectionTestUtils.setField(JUPO_CONCEPT_PARSER, "fusekiHost", "testing.test");
        ReflectionTestUtils.setField(JUPO_CONCEPT_PARSER, "fusekiPort", "1111");
        ReflectionTestUtils.setField(JENA_ONTOLOGY_FETCHER, "fusekiHost", "testing.test");
        ReflectionTestUtils.setField(JENA_ONTOLOGY_FETCHER, "fusekiPort", "1111");
    }
    
    @Before
    public void setup() throws MalformedURLException {
    }

    @Test
    public void deleteFusekiTDBDataset() {
        PowerMockito.mockStatic(UpdateExecutionFactory.class);
        when(UpdateExecutionFactory.createRemote((UpdateRequest)anyObject(), eq("http://testing.test:1111/all/update"))).thenReturn(updateProcessor);
        when(UpdateExecutionFactory.createRemote((UpdateRequest)anyObject(), eq("http://testing.test:1111/all_no_inference/update"))).thenReturn(updateProcessor);
        doNothing().when(updateProcessor).execute();
        JENA_ONTOLOGY_FETCHER.deleteFusekiTDBDataset();
    }
    
    @Test
    public void testGetBroaderConcepts() {
        PowerMockito.mockStatic(QueryExecutionFactory.class);
        when(QueryExecutionFactory.sparqlService(anyString(), (Query)anyObject())).thenReturn(qexec);
        // return values also for 'broader' query. Thats why two thenReturn methods.
        when(qexec.execSelect()).thenReturn(createResultSet()).thenReturn(createResultSet());
        List<ConceptDTO> dtos = JUPO_CONCEPT_PARSER.getBroaderConceptsByUri("test").collect(Collectors.toList());
        Assert.assertEquals(RESULTSET_SIZE, dtos.size());
        assertEquals("http://urn.fi/URN:NBN:fi:au:ptvl:KE12", dtos.get(1).getId());
        assertEquals("Kuntoutus", dtos.get(1).getLabel());
    }

    @Test
    public void testGetConceptByUri() {
        basicMockSetup();        
        ConceptDTO dto = JUPO_CONCEPT_PARSER.getConceptByUri("http://www.yso.fi/onto/jupo/p1074");
        assertEquals("http://www.yso.fi/onto/jupo/p1074", dto.getId());
    }
    
    @Test
    public void testGetConcepts() {
        basicMockSetup();        
        List<ConceptDTO> dtos = JUPO_CONCEPT_PARSER.getConcepts().collect(Collectors.toList());
        Assert.assertEquals(RESULTSET_SIZE, dtos.size());
    }
    
    @Test
    public void testGetConceptsByLabel() {
        basicMockSetup();
        ConceptDTO dto = JUPO_CONCEPT_PARSER.getConceptByUri("http://www.yso.fi/onto/jupo/p1074");
        assertEquals("http://www.yso.fi/onto/jupo/p1074", dto.getId());
    }
    
    private void basicMockSetup () {
        PowerMockito.mockStatic(QueryExecutionFactory.class);
        when(QueryExecutionFactory.sparqlService(anyString(), (Query)anyObject())).thenReturn(qexec);
        when(qexec.execSelect()).thenReturn(createResultSet());
    }
    
    private ResultSet createResultSet() {
        String resultJSON = "{\n" +
                            "  \"head\": {\n" +
                            "   \"vars\": [ \"subject\" , \"predicate\" , \"object\" ]\n" +
                            "  } ,\n" +
                            "  \"results\": {\n" +
                            "    \"bindings\": [\n" +
                            "      {\n" +
                            "        \"uri\": { \"type\": \"uri\" , \"value\": \"http://www.yso.fi/onto/jupo/p1074\" } ,\n" +
                            "        \"label\": { \"type\": \"literal\" , \"value\": \"kuntouttava työtoiminta\" } ,\n" +
                            "        \"scheme\": { \"type\": \"literal\" , \"value\": \"http://www.yso.fi/onto/jupo/\" }\n" +
                            "      }," +
                            "      {\n" +
                            "        \"uri\": { \"type\": \"uri\" , \"value\": \"http://urn.fi/URN:NBN:fi:au:ptvl:KE12\" } ,\n" +
                            "        \"label\": { \"type\": \"literal\" , \"value\": \"Kuntoutus\" } ,\n" +
                            "        \"notation\": { \"type\": \"literal\" , \"value\": \"KE12\" } ,\n" +
                            "        \"scheme\": { \"type\": \"literal\" , \"value\": \"http://urn.fi/URN:NBN:fi:au:ptvl:\" }\n" +
                            "      }" +
                            "]\n" +
                            "  }\n" +
                            "}";
        InputStream stream = new ByteArrayInputStream(resultJSON.getBytes(StandardCharsets.UTF_8));
        return ResultSetFactory.fromJSON(stream);
    }

}
