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
import fi.vm.kapa.sevi.service.commons.ConceptType;
import fi.vm.kapa.sevi.sparql.SparqlQueryBuilder;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static fi.vm.kapa.sevi.sparql.SparqlQueryBuilder.urify;


public abstract class BaseConceptParser implements JenaConceptParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseConceptParser.class);

    /**
     * The default language is Finnish.
     */
    protected String lang = "fi";

    protected ConceptType conceptType;

    // Fuseki settings
    @Value("${environment.fuseki.host}")
    protected String fusekiHost;
    @Value("${environment.fuseki.port}")
    protected String fusekiPort;

    protected abstract  String buildQueryAllConcepts();

    protected abstract  String buildQueryAllTopLevelConcepts();

    protected abstract String buildQueryFindConceptByUri(String uri);

    protected abstract Stream<ConceptDTO> executeQuery(Query query);
    protected abstract Stream<ConceptDTO> executeNoInferenceQuery(Query query);

    public BaseConceptParser(ConceptType conceptType) {
        this.conceptType = conceptType;
    }

    @Override
    public ConceptType getConceptType() {
        return conceptType;
    }

    @Override
    public ConceptDTO getConceptByUri(String uri) {
        LOGGER.info("Find concept type " + conceptType + " by uri " + uri);
        Stream<ConceptDTO> concepts = executeQuery(QueryFactory.create(buildQueryFindConceptByUri(uri)));
        return concepts.findFirst().orElse(null);
    }

    @Override
    public Stream<ConceptDTO> getConcepts() {
        LOGGER.info("Find all concepts of type " + conceptType);
        return executeNoInferenceQuery(QueryFactory.create(buildQueryAllConcepts()));
    }

    @Override
    public Stream<ConceptDTO> getTopLevelConcepts() {
        LOGGER.info("Find all top-level concepts of type " + conceptType);
        return executeQuery(QueryFactory.create(buildQueryAllTopLevelConcepts()));
    }


    @Override
    public final Stream<ConceptDTO> getBroaderConceptsByUri(String uri) {
        return queryModelForBroader(uri);
    }

    @Override
    public final Stream<ConceptDTO> getNarrowerConceptsByUri(String uri) {
        return queryModelForNarrower(uri);
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public final Stream<ConceptDTO> getConceptsByLabel(String label) {
        return queryModelByLabel(label, "fi");
    }

    /**
     * Note: This is public for the tests' sake.
     */
    public Stream<ConceptDTO> queryModelForBroader(String uri) {
        String serviceURI = getFusekiSparqlUri();
        LOGGER.info("QUERY broaderUri: " + uri + ", from " + serviceURI);
        String queryString = buildBroaderQueryString(uri);
        LOGGER.info("QUERY STRING: " + queryString);
        Query query = QueryFactory.create(queryString);
        return queryModelFor(serviceURI, query);
    }

    private Stream<ConceptDTO> queryModelForNarrower(String uri) {
        String serviceURI = getFusekiSparqlUri();
        LOGGER.info("QUERY narrowerUri: " + uri + ", from " + serviceURI);
        String queryString = buildNarrowerQueryString(uri);
        Query query = QueryFactory.create(queryString);
        return queryModelFor(serviceURI, query);
    }

    protected Stream<ConceptDTO> getResultStream(ResultSet results, QueryExecution qexec) {
        Iterable<ConceptDTO> resultIterator = new ResultIterable(results);
        // Filtering out the null results here.
        return StreamSupport.stream(resultIterator.spliterator(), false)
            .filter(c -> !("".equals(c.getId()))).onClose(() -> qexec.close());
    }
    
    private Stream<ConceptDTO> queryModelFor(String serviceURI, Query query) {
        // Note: This is closed when the stream is closed. Must not be closed before.
        QueryExecution qexec = QueryExecutionFactory.sparqlService(serviceURI, query);
        ResultSet results = qexec.execSelect();
        Stream<ConceptDTO> concepts = getResultStream(results, qexec);
        return concepts;
    }

    private String buildBroaderQueryString(String uri) {
        return new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri (str(?prefLabel) AS ?label)", "?scheme", "?notation")
                .where(urify(uri), "skos:broaderTransitive", "?uri")
                .filter("NOT EXISTS { ?uri a skosext:DeprecatedConcept }")
                .where("?uri", "skos:inScheme", "?scheme")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                // This is used to sort the results.
                .where("OPTIONAL { ?uri skos:broaderTransitive " + "?evenBroader }")
                .where("OPTIONAL { ?uri skos:notation ?notation }")
                .filter("(lang(?prefLabel) = 'fi')")
                .groupBy("?uri")
                .groupBy("?scheme")
                .groupBy("?prefLabel")
                .groupBy("?notation")
                .orderBy("ASC(COUNT(?evenBroader))")
                .build();
    }

    private String buildNarrowerQueryString(String uri) {
        return new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri (str(?prefLabel) AS ?label)", "?scheme", "?notation")
                .filter("NOT EXISTS { ?uri a skosext:DeprecatedConcept }")
                // Note: Not skos:narrowerTransitive here, we don't want to return all the levels down, only the next one.
                .where(urify(uri), "skos:narrower", "?uri")
                .where("?uri", "skos:inScheme", "?scheme")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                .where("OPTIONAL { ?uri skos:notation ?notation }")
                .filter("(lang(?prefLabel) = 'fi')")
                .build();
    }

    /**
     * This is not really necessary. The caller should always use resource URIs which it has from somewhere else.
     * @deprecated
     */
    @Deprecated
    private Stream<ConceptDTO> queryModelByLabel(String label, String language) {
        String serviceURI = getFusekiSparqlUri();
        LOGGER.info("QUERY label: " + label + ", from " + serviceURI);
        String queryString = buildQueryStringByLabel(label, language);
        Query query = QueryFactory.create(queryString);

        // Note: This is closed when the stream is closed. Must not be closed before.
        QueryExecution qexec = QueryExecutionFactory.sparqlService(serviceURI, query);
        ResultSet results = qexec.execSelect();
        Stream<ConceptDTO> concepts = getResultStream(results, qexec);

        return concepts;
    }

    /**
     * This is not really necessary. The caller should always use resource URIs which it has from somewhere else.
     * This is really slow.
     * @deprecated
     */
    @Deprecated
    private String buildQueryStringByLabel(String label, String language) {
        return new SparqlQueryBuilder()
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri", "?label")
                .where("?uri", "skos:prefLabel", "?label")
                .where("OPTIONAL { ?subject skos:notation ?notation }")
                .filter("(?label=\"" + label + "\"@" + language + ")")
                .build();
    }

    protected String getFusekiServiceUri() {
        return "http://" + fusekiHost + ":" + fusekiPort + "/all";
    }

    private String getFusekiSparqlUri() {
        return getFusekiServiceUri() + "/sparql";
    }

}
