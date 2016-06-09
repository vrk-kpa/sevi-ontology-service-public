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
import fi.vm.kapa.sevi.ontology.dto.ViewDTO;
import fi.vm.kapa.sevi.sparql.SparqlQueryBuilder;
import org.apache.jena.query.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static fi.vm.kapa.sevi.sparql.SparqlQueryBuilder.urify;

import java.util.HashMap;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
public class AllConceptParser extends GenericConceptParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllConceptParser.class);

    public AllConceptParser() {
        super(ConceptType.ALL);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public ConceptDTO getConceptByLabel(String label) {
        String queryByLabel = new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri", "(str(?prefLabel) AS ?label)", "?scheme", "?notation")
                .where("?uri", "a", "skos:Concept")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                .where("?uri", "skos:inScheme", "?scheme")
                .where("OPTIONAL { ?uri skos:notation ?notation }")
                .filter("(?prefLabel=\"" + label + "\"@" + lang + ")")
                .build();
        LOGGER.info("Find concept type " + conceptType + " by label " + label);
        Stream<ConceptDTO> concepts = executeNoInferenceQuery(QueryFactory.create(queryByLabel));
        
        return concepts.findFirst().orElse(null);
    }

    @Override
    protected String buildQueryAllConcepts() {
        String query = new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri", "(str(?prefLabel) AS ?label)", "?scheme", "?notation")
                .filter("NOT EXISTS { ?uri a skosext:DeprecatedConcept }")
                .where("?uri", "a", "skos:Concept")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                .where("?uri", "skos:inScheme", "?scheme")
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
                .filter("NOT EXISTS { ?uri a skosext:DeprecatedConcept }")
                .where("?uri", "a", "skos:Concept")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                .where("?uri", "skos:topConceptOf", "?scheme")
                .where("?uri", "skos:inScheme", "?scheme")
                .where("OPTIONAL { ?uri skos:notation ?notation }")
                .filter("(lang(?prefLabel) = '" + lang + "')").build();

        LOGGER.debug("builds {} query all Concepts: \n{}", conceptType, query);

        return query;
    }

    @Override
    protected String buildQueryFindConceptByUri(String uri) {
        return new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("(str(" + urify(uri) + ") AS ?uri)", "(str(?prefLabel) AS ?label)",
                        "?scheme", "?notation")
                .filter("NOT EXISTS { " + urify(uri) + " a skosext:DeprecatedConcept }")
                .where(urify(uri), "a", "skos:Concept")
                .where(urify(uri), "skos:prefLabel", "?prefLabel")
                .where(urify(uri), "skos:inScheme", "?scheme")
                .where("OPTIONAL { " + urify(uri) + " skos:notation ?notation }")
                .filter("(lang(?prefLabel) = '" + lang + "')")
                .build();
    }

    @Override
    public String buildBroaderQuery(String uri) {
        return new SparqlQueryBuilder()
                .prefix("skosext", "http://purl.org/finnonto/schema/skosext")
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("DISTINCT ?uri (str(?prefLabel) AS ?label)", "?scheme", "?notation")
                // This broader relation is transitive and returns all concepts that are one or more levels more general.
                .filter("NOT EXISTS { ?uri a skosext:DeprecatedConcept }")
                .where(urify(uri), "skos:broaderTransitive", "?uri")
                .where("?uri", "skos:prefLabel", "?prefLabel")
                .where("?uri", "skos:inScheme", "?scheme")
                // This is used to sort the results.
                .where("OPTIONAL { ?uri skos:broaderTransitive " + "?evenBroader }")
                .where("OPTIONAL { ?uri skos:notation ?notation }")
                .filter("(lang(?prefLabel) = '" + lang + "')")
                .groupBy("?uri")
                .groupBy("?scheme")
                .groupBy("?label")
                .groupBy("?notation")
                .orderBy("ASC(COUNT(?evenBroader))")
                .build();
    }

    /**
     * Fills the leaf nodes with narrower concepts.
     */
    private void fillViewLeaf(ViewDTO leaf) {
        Stream<ViewDTO> leafs = getNarrowerConceptsByUri(leaf.getId()).map(c -> new ViewDTO(c));
        List<ViewDTO> leafList = leafs.collect(Collectors.toList());
        leaf.setChildren(leafList);
        for (ViewDTO childLeaf : leafList) {
            fillViewLeaf(childLeaf);
        }
    }
    
    public Collection<ViewDTO> getViewsForTopLevelConcepts(Stream<ConceptDTO> topLevelConcepts) {
        Map<String, ViewDTO> viewMap = new HashMap<>();
        topLevelConcepts.forEach(topLevelConcept -> {
            String scheme = topLevelConcept.getScheme();
            ViewDTO rootNode;
            if (!viewMap.containsKey(scheme)) {
                // Adding a root node.
                ConceptType conceptType = ConceptType.ofScheme(scheme);
                rootNode = new ViewDTO(scheme, conceptType.toString(), conceptType);
                viewMap.put(scheme, rootNode);
            }
            rootNode = viewMap.get(scheme);
            ViewDTO leaf = new ViewDTO(topLevelConcept);
            rootNode.getChildren().add(leaf);
            fillViewLeaf(leaf);
        });
        return viewMap.values();
    }
}
