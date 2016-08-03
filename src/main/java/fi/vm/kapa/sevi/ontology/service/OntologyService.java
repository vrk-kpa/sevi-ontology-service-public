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
import fi.vm.kapa.sevi.ontology.dto.ViewDTO;
import fi.vm.kapa.sevi.ontology.exception.ConceptNotFoundException;
import fi.vm.kapa.sevi.ontology.exception.OntologyServerException;
import fi.vm.kapa.sevi.ontology.exception.ParserNotFoundException;
import fi.vm.kapa.sevi.ontology.jena.AllConceptParser;
import fi.vm.kapa.sevi.ontology.jena.JenaConceptParser;
import fi.vm.kapa.sevi.ontology.jena.JenaOntologyFetcher;
import fi.vm.kapa.sevi.ontology.jena.ontology.*;
import fi.vm.kapa.sevi.ontology.jena.vocabulary.LifesituationParser;
import fi.vm.kapa.sevi.ontology.jena.vocabulary.PtvlClassificationParser;
import fi.vm.kapa.sevi.ontology.jena.vocabulary.TargetGroupParser;
import fi.vm.kapa.sevi.ontology.service.indexing.IndexingService;
import fi.vm.kapa.sevi.service.commons.ConceptType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OntologyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyService.class);

    // TODO: move cache and update state from service to Redis. PAL-1917
    protected static final AtomicBoolean updateOngoing = new AtomicBoolean(false);

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    @Autowired
    private AllConceptParser allConceptParser;
    
    @Autowired
    private JuhoConceptParser juhoConceptParser;
    
    @Autowired
    private JupoConceptParser jupoConceptParser;

    @Autowired
    private LiitoConceptParser liitoConceptParser;
    
    @Autowired
    private TeroConceptParser teroConceptParser;

    @Autowired
    private TsrConceptParser tsrConceptParser;
    
    @Autowired
    private YsoConceptParser ysoConceptParser;
    
    @Autowired
    private TargetGroupParser targetGroupParser;

    @Autowired
    private PtvlClassificationParser ptvlClassificationParser;

    @Autowired
    private LifesituationParser lifesituationParser;

    @Autowired
    private IndexingService indexingService;

    @Autowired 
    private JenaOntologyFetcher jenaOntologyFetcher;

    // This is package private to mock this in tests.
    List<JenaConceptParser> getParsers() {
        List<JenaConceptParser> parsers = new ArrayList<>();
        parsers.addAll(getClassificationParsers());
        parsers.addAll(getConceptParsers());
        return parsers;
    }

    private List<JenaConceptParser> getClassificationParsers() {
        return Arrays.asList(
                lifesituationParser,
                targetGroupParser,
                ptvlClassificationParser
        );
    }

    private List<JenaConceptParser> getConceptParsers() {
        return Arrays.asList(
                juhoConceptParser,
                jupoConceptParser,
                liitoConceptParser,
                teroConceptParser,
                tsrConceptParser,
                ysoConceptParser
        );
    }

    @Cacheable("findConcepts")
    public List<ConceptDTO> findConcepts() {
        try (Stream<ConceptDTO> stream = queryAllConcepts(Arrays.asList(allConceptParser))) {
            return stream.collect(Collectors.toList());
        }
    }

    @Cacheable("findViews")
    private List<ViewDTO> findViews() {
        try (Stream<ConceptDTO> stream = getClassificationParsers().stream()
                .flatMap(p -> p.getTopLevelConcepts()).map(c -> (ConceptDTO) c)) {
            return new ArrayList<>(allConceptParser.getViewsForTopLevelConcepts(stream));
        }
    }

    @Cacheable("findConceptsByType")
    public List<ConceptDTO> findConceptsByType(ConceptType type) {
        try (Stream<ConceptDTO> stream = getParserForType(type).getConcepts()) {
            return stream.collect(Collectors.toList());
        }
    }

    @Cacheable("findTopLevelConceptsByType")
    public List<ConceptDTO> findTopLevelConceptsByType(ConceptType type) {
        try (Stream<ConceptDTO> stream = getParserForType(type).getTopLevelConcepts()) {
            return stream.collect(Collectors.toList());
        }
    }

    @Cacheable("getConcept")
    public ConceptDTO getConcept(String uri) {
        ConceptDTO concept = allConceptParser.getConceptByUri(uri);
        if (concept == null) {
            throw new ConceptNotFoundException(uri);
        }
        return concept;
    }

    /**
     * @deprecated
     */
    @Cacheable("getConceptsByLabel")
    @Deprecated
    public ConceptDTO getConceptByLabel(String label) {
        ConceptDTO concept = allConceptParser.getConceptByLabel(label);
        if (concept == null) {
            throw new ConceptNotFoundException(label);
        }
        return concept;
    }

    @Cacheable("findBroaderConcepts")
    public List<ConceptDTO> findBroaderConcepts(String uri) {
        try (Stream<ConceptDTO> stream = allConceptParser.getBroaderConceptsByUri(uri)) {
            List<ConceptDTO> res = stream.collect(Collectors.toList());
            return res;
        }
    }

    @Cacheable("findNarrowerConcepts")
    public List<ConceptDTO> findNarrowerConcepts(String uri) {
        try (Stream<ConceptDTO> stream = allConceptParser.getNarrowerConceptsByUri(uri)) {
            return stream.collect(Collectors.toList());
        }
    }

    public Boolean fetchConcepts() {
        return runWithOngoingFlag(this::doFetchConcepts);
    }

    public Boolean fetchAndIndexConcepts() {
        return runWithOngoingFlag(this::doFetchAndIndexOntologies);
    }

    public Boolean indexPtvlToplevels() {
        return runWithOngoingFlag(this::doIndexPtvlToplevels);
    }

    public Boolean indexPtvlViews() {
        return runWithOngoingFlag(this::doIndexPtvlViews);
    }

    private void doIndexPtvlToplevels() {
        long now = System.currentTimeMillis();
        LOGGER.info("Querying PTVL toplevel concepts");
        Stream<Object> tlConcepts = getClassificationParsers().stream()
                .flatMap(p -> p.getTopLevelConcepts());
        LOGGER.info("PTVL toplevel concepts queried");
        LOGGER.info("Indexing PTVL toplevel concepts");
        indexingService.reIndexTopLevelConcepts(tlConcepts);
        LOGGER.info("PTVL toplevel concepts indexed in {}ms", System.currentTimeMillis() - now);
    }

    private void doIndexPtvlViews() {
        long now = System.currentTimeMillis();
        Collection<ViewDTO> views = findViews();
        LOGGER.info("Views queried");
        LOGGER.info("Indexing views");
        indexingService.reIndexViews(views);
        LOGGER.info("Views indexed in {}ms", System.currentTimeMillis() - now);
    }

    public Boolean indexConcepts(boolean indexAll, boolean indexTopLevel, boolean indexViews) {
        return runWithOngoingFlag(() -> doIndexConcepts(indexAll, indexTopLevel, indexViews));
    }

    public Boolean indexConcepts(ConceptType type) {
        return runWithOngoingFlag(() -> doIndexConcepts(type));
    }

    private void doFetchConcepts() {
        LOGGER.info("Delete old ontologies");
        deleteAllFusekiDatasets();
        LOGGER.info("Download ontologies");
        createAllFusekiDatasets();
        LOGGER.info("Ontologies stored");
        LOGGER.info("Querying ontologies");
        evictCaches();
    }

    private void doIndexConcepts(ConceptType type) {
        long now = System.currentTimeMillis();
        indexingService.reIndexConcepts(getParserForType(type).getConcepts());
        LOGGER.info("{} concepts indexed in {}ms", type, System.currentTimeMillis() - now);
    }

    private void doIndexConcepts(boolean indexAll, boolean indexTopLevel, boolean indexViews) {
        if (indexTopLevel) {
            doIndexPtvlToplevels();
            LOGGER.info("Querying top level concepts");
            Stream<Object> tlConcepts = getClassificationParsers().stream()
                    .flatMap(p -> p.getTopLevelConcepts());
            LOGGER.info("Top level concepts queried");
            LOGGER.info("Indexing top level concepts");
            indexingService.reIndexTopLevelConcepts(tlConcepts);
            LOGGER.info("Top level concepts indexed");
        }

        if (indexViews) {
            doIndexPtvlViews();
        }

        if (indexAll) {
            long now = System.currentTimeMillis();
            LOGGER.info("Indexing concepts");
            getConceptParsers().stream().forEach(p -> indexingService.reIndexConcepts(p.getConcepts()));
            LOGGER.info("Concepts indexed in {}ms", System.currentTimeMillis() - now);
        }
    }
    
    private void doFetchAndIndexOntologies() {
        doFetchConcepts();
        doIndexConcepts(true, true, true);
    }

    /**
     * This method is public for test use.
     */
    @CacheEvict(value = {"findConcepts","findViews","findConceptsByType","findTopLevelConceptsByType",
            "getConcept","getConceptsByLabel","findBroaderConcepts","findNarrowerConcepts"}, allEntries=true)
    public void evictCaches() {
        // This method is intentionally empty. It evicts the caches through annotated aspects.
        LOGGER.info("Evicting caches.");
    }
    
    public boolean isUpdateOngoing() {
        return updateOngoing.get();
    }

    public Boolean deleteOntologies()  {
        return runWithOngoingFlag(this::deleteAllFusekiDatasets);
    }

    private boolean runWithOngoingFlag(Runnable target) {
        if (updateOngoing.compareAndSet(false, true)) {
            try {
                executor.execute(target);
                return true;
            } catch(RuntimeException e) {
                LOGGER.info("Operation failed with exception" , e);
                return false;
            }
            finally {
                LOGGER.info("Now clearing updateOngoing flag");
                updateOngoing.getAndSet(false);
            }
        } else {
            LOGGER.info("Ontology updating already ongoing");
            return false;
        }
    }

    protected JenaConceptParser getParserForType(ConceptType type) {
        return getParsers()
                .stream()
                .filter(p -> p.getConceptType().equals(type))
                .findFirst()
                .orElseThrow(() -> new ParserNotFoundException(type));
    }

    void deleteAllFusekiDatasets() throws OntologyServerException {
        jenaOntologyFetcher.deleteFusekiTDBDataset();
        evictCaches();
    }

    private void createAllFusekiDatasets() throws OntologyServerException {
        jenaOntologyFetcher.createFusekiTDBDataset();
    }
    
    private Stream<ConceptDTO> queryAllConcepts(List<JenaConceptParser> parsers) {
        return parsers.stream()
                .map(JenaConceptParser::getConcepts).flatMap(c -> c);
    }

    // Used in tests to set synchronous executor
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
