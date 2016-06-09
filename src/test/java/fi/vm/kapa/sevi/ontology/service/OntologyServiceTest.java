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
package fi.vm.kapa.sevi.ontology.service;

import fi.vm.kapa.sevi.ontology.dto.ConceptDTO;
import fi.vm.kapa.sevi.service.commons.ConceptType;
import fi.vm.kapa.sevi.ontology.exception.ConceptNotFoundException;
import fi.vm.kapa.sevi.ontology.jena.AllConceptParser;
import fi.vm.kapa.sevi.ontology.jena.JenaConceptParser;
import fi.vm.kapa.sevi.ontology.jena.JenaOntologyFetcher;
import fi.vm.kapa.sevi.ontology.jena.ontology.JupoConceptParser;
import fi.vm.kapa.sevi.ontology.jena.vocabulary.LifesituationParser;
import fi.vm.kapa.sevi.ontology.jena.vocabulary.PtvlClassificationParser;
import fi.vm.kapa.sevi.ontology.jena.vocabulary.TargetGroupParser;
import fi.vm.kapa.sevi.ontology.service.indexing.IndexingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OntologyServiceTest {

    @Mock
    private JupoConceptParser jupoConceptParser;

    @Mock
    private TargetGroupParser targetGroupParser;

    @Mock
    private PtvlClassificationParser ptvlClassificationParser;

    @Mock
    private LifesituationParser lifesituationParser;

    @Mock
    private AllConceptParser allConceptParser;

    @Mock
    private IndexingService indexingService;
    
    @Mock
    private JenaOntologyFetcher jenaOntologyFetcher;

    @InjectMocks
    private OntologyService ontologyService;
    
    private ConceptDTO first;
    private ConceptDTO second;
    private List<ConceptDTO> concepts;

    // When adding new parsers, also add to following two methods
    private List<JenaConceptParser> getParsers() {
        return Arrays.asList(
                lifesituationParser,
                targetGroupParser,
                ptvlClassificationParser,
                jupoConceptParser
        );
    }

    private void setupGetConceptType() {
        when(jupoConceptParser.getConceptType()).thenReturn(ConceptType.JUPO);
        when(targetGroupParser.getConceptType()).thenReturn(ConceptType.TARGETGROUP);
        when(ptvlClassificationParser.getConceptType()).thenReturn(ConceptType.PTVL);
        when(lifesituationParser.getConceptType()).thenReturn(ConceptType.LIFESITUATION);
        when(allConceptParser.getConceptType()).thenReturn(ConceptType.ALL);
    }

    @Before
    public void setup() {
        first = new ConceptDTO("http://localhost/dummy/1", "yksi", "", ConceptType.JUPO.getInScheme());
        second = new ConceptDTO("http://localhost/dummy/2", "kaksi", "", ConceptType.JUPO.getInScheme());
        concepts = Arrays.asList(first, second);
        this.ontologyService = Mockito.spy(this.ontologyService);
        Mockito.doAnswer(new Answer<List<JenaConceptParser>>() {

            @Override
            public List<JenaConceptParser> answer(InvocationOnMock invocation) throws Throwable {
                return getParsers();
            }
            
        }).when(ontologyService).getParsers();
        ontologyService.evictCaches();
    }

    @Test
    public void getParserForType() {
        setupGetConceptType();
        final JenaConceptParser parserForType = ontologyService.getParserForType(ConceptType.PTVL);
        assertEquals(ConceptType.PTVL, parserForType.getConceptType());
    }

    @Test
    public void deleteOntologies() throws Exception {
        doNothing().when(jenaOntologyFetcher).deleteFusekiTDBDataset();

        assertTrue(ontologyService.deleteOntologies());

        verify(jenaOntologyFetcher, times(1)).deleteFusekiTDBDataset();
    }

    @Test
    public void findConceptsEmpty() throws Exception {
        ArrayList<ConceptDTO> _concepts = new ArrayList<>();
        setupGetConcepts(_concepts);
        assertTrue(ontologyService.findConcepts().isEmpty());
    }

    @Test
    public void findConcepts() throws Exception {
        List<ConceptDTO> _concepts = this.concepts;
        setupGetConcepts(_concepts);

        List<ConceptDTO> ontologiesFromService = ontologyService.findConcepts();

        assertEquals(_concepts.size(), ontologiesFromService.size());
        assertTrue(ontologiesFromService.contains(first));
        assertTrue(ontologiesFromService.contains(second));
    }

    @Test
    public void findConceptsByTypeJupo() throws Exception {
        when(jupoConceptParser.getConcepts()).thenReturn(StreamSupport.stream(concepts.spliterator(), false));

        setupGetConceptType();

        List<ConceptDTO> ontologiesFromService = ontologyService.findConceptsByType(ConceptType.JUPO);

        verify(targetGroupParser, never()).getConcepts();
        verify(lifesituationParser, never()).getConcepts();
        assertEquals(concepts.size(), ontologiesFromService.size());
        assertTrue(ontologiesFromService.contains(first));
        assertTrue(ontologiesFromService.contains(second));
    }

    @Test
    public void findConceptsByTypeTargetGroup() throws Exception {
        when(targetGroupParser.getConcepts()).thenReturn(StreamSupport.stream(concepts.spliterator(), false));

        setupGetConceptType();

        List<ConceptDTO> ontologiesFromService = ontologyService.findConceptsByType(ConceptType.TARGETGROUP);

        verify(jupoConceptParser, never()).getConcepts();
        verify(lifesituationParser, never()).getConcepts();
        assertEquals(concepts.size(), ontologiesFromService.size());
        assertTrue(ontologiesFromService.contains(first));
        assertTrue(ontologiesFromService.contains(second));
    }

    @Test
    public void findConceptsByTypeToplevel() throws Exception {

        setupGetConceptType();

        when(ptvlClassificationParser.getConcepts()).thenReturn(StreamSupport.stream(concepts.spliterator(), false));

        List<ConceptDTO> ontologiesFromService = ontologyService.findConceptsByType(ConceptType.PTVL);

        verify(jupoConceptParser, never()).getConcepts();
        verify(targetGroupParser, never()).getConcepts();
        verify(lifesituationParser, never()).getConcepts();
        assertEquals(concepts.size(), ontologiesFromService.size());
        assertTrue(ontologiesFromService.contains(first));
        assertTrue(ontologiesFromService.contains(second));
    }

    @Test
    public void findConceptsByTypeLifesituation() throws Exception {
        when(lifesituationParser.getConcepts()).thenReturn(StreamSupport.stream(concepts.spliterator(), false));

        setupGetConceptType();

        List<ConceptDTO> ontologiesFromService = ontologyService.findConceptsByType(ConceptType.LIFESITUATION);

        verify(jupoConceptParser, never()).getConcepts();
        verify(targetGroupParser, never()).getConcepts();
        assertEquals(concepts.size(), ontologiesFromService.size());
        assertTrue(ontologiesFromService.contains(first));
        assertTrue(ontologiesFromService.contains(second));
    }

    @Test
    public void findBroaderConcepts() throws Exception {
        when(allConceptParser.queryModelForBroader(anyString())).thenReturn(StreamSupport.stream(concepts.spliterator(), false));

        List<ConceptDTO> ontologiesFromService = ontologyService.findBroaderConcepts("http://localhost/broader_uri");
        assertEquals(concepts.size(), ontologiesFromService.size());
        assertTrue(ontologiesFromService.contains(first));
        assertTrue(ontologiesFromService.contains(second));
    }

    @Test
    public void deleteOntology() throws InterruptedException {
        doNothing().when(jenaOntologyFetcher).deleteFusekiTDBDataset();
        setupGetConceptType();
        assertTrue(ontologyService.deleteOntologies());
        ontologyService.executor.awaitTermination(100, TimeUnit.SECONDS);
        verify(jenaOntologyFetcher).deleteFusekiTDBDataset();
    }

    @Test
    public void getOneFound() throws Exception {
        setupGetConceptByUriToNull();
        when(allConceptParser.getConceptByUri(anyString())).thenReturn(first);

        assertEquals(first, ontologyService.getConcept("uri"));
    }

    @Test(expected=ConceptNotFoundException.class)
    public void getNotFound() throws Exception {
        when(jupoConceptParser.getConceptByUri(anyString())).thenReturn(null);
        setupGetConceptByUriToNull();

        ontologyService.getConcept("uri");
    }

    private void setupGetConcepts(List<ConceptDTO> _concepts) {
        when(allConceptParser.getConcepts()).thenReturn(StreamSupport.stream(_concepts.spliterator(), false));
        getParsers().forEach(p -> when(p.getConcepts()).thenReturn(StreamSupport.stream(_concepts.spliterator(), false)));
    }

    private void setupGetConceptByUriToNull() {
        getParsers().forEach(p -> when(p.getConceptByUri(anyString())).thenReturn(null));
    }

}
