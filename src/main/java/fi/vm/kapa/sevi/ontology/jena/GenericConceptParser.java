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
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static fi.vm.kapa.sevi.sparql.SparqlQueryBuilder.urify;


public class GenericConceptParser extends BaseConceptParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericConceptParser.class);

    public GenericConceptParser(ConceptType conceptType) {
        super(conceptType);
    }

    @Override
    protected Stream<ConceptDTO> executeNoInferenceQuery(Query query) {
        String serviceURI = "http://" + fusekiHost + ":" + fusekiPort + "/all_no_inference/sparql";
        LOGGER.debug("Execute query for concept type " + conceptType + ", from " + serviceURI + " with query: " + query.toString());

        // Note: This is closed when the stream is closed. Must not be closed before.
        QueryExecution qexec = QueryExecutionFactory.sparqlService(serviceURI, query);
                qexec.setTimeout(120, TimeUnit.MINUTES);
        ResultSet results = qexec.execSelect();

        return getResultStream(results, qexec).filter(
                c -> !c.getScheme().endsWith("/aggregateconceptscheme") &&
                !c.getScheme().endsWith("/deprecatedconceptscheme"));
    }

    @Override
    protected Stream<ConceptDTO> executeQuery(Query query) {
        String serviceURI = "http://" + fusekiHost + ":" + fusekiPort + "/all/sparql";
        LOGGER.debug("Execute query for concept type " + conceptType + ", from " + serviceURI + " with query: " + query.toString());

        // Note: This is closed when the stream is closed. Must not be closed before.
        QueryExecution qexec = QueryExecutionFactory.sparqlService(serviceURI, query);
        qexec.setTimeout(10, TimeUnit.MINUTES);
        ResultSet results = qexec.execSelect();
        return getResultStream(results, qexec).filter(
                c -> !c.getScheme().endsWith("/aggregateconceptscheme") &&
                !c.getScheme().endsWith("/deprecatedconceptscheme"));
    }
    
    @Override
    protected String buildQueryAllConcepts() {
        String query = new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri", "(str(?prefLabel) AS ?label)", "?scheme", "?notation")
                .where("{ VALUES ?scheme { <" + conceptType.getInScheme() + "> } }")
                .filter("NOT EXISTS { ?uri a skosext:DeprecatedConcept }")
                .where("?uri", "skos:inScheme", "?scheme")
                .where("?uri", "a", "skos:Concept")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                .where("OPTIONAL { ?uri skos:notation ?notation }")
                .filter("(lang(?prefLabel) = '" + lang + "')").build();

        LOGGER.debug("builds {} query all Concepts: \n{}", conceptType, query);

        return query;
    }

    @Override
    protected String buildQueryAllTopLevelConcepts() {
        String query = new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri", "(str(?prefLabel) AS ?label)", "?scheme", "?notation")
                .where("{ VALUES ?scheme { <" + conceptType.getInScheme() + "> } }")
                .filter("NOT EXISTS { ?uri a skosext:DeprecatedConcept }")
                .where("?uri", "skos:inScheme", "?scheme")
                .where("?uri", "a", "skos:Concept")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                .where("?uri", "skos:topConceptOf", "<" + conceptType.getInScheme() + ">")
                .where("OPTIONAL { ?uri skos:notation ?notation }")
                .filter("(lang(?prefLabel) = '" + lang + "')").build();

        LOGGER.debug("builds {} query all top-level Concepts: \n{}", conceptType, query);

        return query;
    }

    @Override
    protected String buildQueryFindConceptByUri(String uri) {
        String query = new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("(str(" + urify(uri) + ") AS ?uri)",
                        "(str(?prefLabel) AS ?label)",
                        "?scheme", "?notation")
                .where("{ VALUES ?scheme { <" + conceptType.getInScheme() + "> } }")
                .filter("NOT EXISTS { " + urify(uri) + " a skosext:DeprecatedConcept }")
                .where(urify(uri), "skos:inScheme", "?scheme")
                .where(urify(uri), "a", "skos:Concept")
                .where(urify(uri), "skos:prefLabel", "?prefLabel")
                .where("OPTIONAL { " + urify(uri) + " skos:notation ?notation }")
                .filter("(lang(?prefLabel) = '" + lang + "')")
                .build();

        return query;
    }

    @Override
    public String buildBroaderQuery(String uri) {
        return new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri", "(str(?prefLabel) AS ?label)", "?scheme", "?notation")
                .where("{ VALUES ?scheme { <" + conceptType.getInScheme() + "> } }")
                .where(urify(uri), "skos:inScheme", "?scheme")
                .where(urify(uri), "skos:broaderTransitive", "?uri")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                .filter("NOT EXISTS { ?uri a skosext:DeprecatedConcept }")
                // This is used to sort the results.
                .where("OPTIONAL { ?uri skos:broaderTransitive ?evenBroader }")
                .where("OPTIONAL { ?uri skos:notation ?notation }")
                .filter("(lang(?prefLabel) = 'fi')")
                .groupBy("?uri")
                .groupBy("?scheme")
                .groupBy("?label")
                .groupBy("?notation")
                .orderBy("ASC(COUNT(?evenBroader))")
                .build();
    }

}
