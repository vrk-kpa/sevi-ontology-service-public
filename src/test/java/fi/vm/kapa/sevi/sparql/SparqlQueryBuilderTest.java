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
package fi.vm.kapa.sevi.sparql;


import fi.vm.kapa.sevi.ontology.dto.Language;
import org.junit.Test;

import static fi.vm.kapa.sevi.sparql.SparqlQueryBuilder.urify;
import static fi.vm.kapa.sevi.sparql.SparqlQueryBuilder.variable;
import static org.junit.Assert.*;

public class SparqlQueryBuilderTest {

    @Test
    public void takesPrefixes() {
        String query = new SparqlQueryBuilder()
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .prefix("foks", "http://www.w4.org/foks")
                .build();

        assertEquals("PREFIX foks: <http://www.w4.org/foks#>\nPREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n", query);
    }

    @Test
    public void takesSelectClause() {
        String query = new SparqlQueryBuilder()
                .select("?uri", "?label")
                .build();

        assertEquals("SELECT ?uri ?label\n", query);
    }

    @Test
    public void takesWhereClauses() {
        String query = new SparqlQueryBuilder()
                .where("?uri", "skos:prefLabel", "?label")
                .build();

        assertEquals("WHERE {\n\t?uri skos:prefLabel ?label .\n}\n", query);
    }

    @Test
    public void takesFiltersWithWhereClauses() {
        String query = new SparqlQueryBuilder()
                .where("?uri", "skos:prefLabel", "?label")
                .filter("regex(?g, \"r\", \"i\")")
                .build();

        assertEquals("WHERE {\n\t?uri skos:prefLabel ?label .\n\tFILTER regex(?g, \"r\", \"i\")\n}\n", query);
    }

    @Test
    public void buildsSimpleQuery() {

        String query = new SparqlQueryBuilder()
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .prefix("vcard", "http://www.w4.org/foks")
                .select("?uri", "?label")
                .where("?uri", "skos:prefLabel", "?label")
                .where("?y", "vcard:Given", "?g")
                .filter("regex(?g, \"r\", \"i\")")
                .build();

        String expected =
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                        "PREFIX vcard: <http://www.w4.org/foks#>\n" +
                        "SELECT ?uri ?label\n" +
                        "WHERE {\n" +
                        "\t?uri skos:prefLabel ?label .\n" +
                        "\t?y vcard:Given ?g .\n" +
                        "\tFILTER regex(?g, \"r\", \"i\")\n" +
                        "}\n";

        assertEquals("query does not match expected", expected, query);
    }

    @Test
    public void buildsExampleQuery() {
        String label = "label";
        String language = "fi";

        String expected =
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "SELECT ?uri ?label\n" +
                "WHERE {\n" +
                "\t?uri skos:prefLabel ?label .\n" +
                "\tFILTER (?label=\"" + label + "\"@" + language + ")\n" +
                "}\n";

        String query = new SparqlQueryBuilder()
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("?uri", "?label")
                .where("?uri", "skos:prefLabel", "?label")
                .filter("(?label=\"" + label + "\"@" + language + ")")
                .build();

        assertEquals("query does not match expected", expected, query);

    }

    @Test
    public void buildsExampleWithTwoFilters() {

        String URI = "https://www.google.fi";
        String LABEL = "google";
        String queryString = "google";
        Language lang = Language.FI;

        String built = new SparqlQueryBuilder()
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("(?subject AS " + variable(URI) + ") (str(?prefLabel) AS " +variable(LABEL) + ")")
                .where("?subject", "?predicate", "skos:Concept")
                .where("?subject", "skos:prefLabel", "?prefLabel")
                .filter("(lang(?prefLabel) = 'fi')")
                .filter("regex(?prefLabel, \"^" + queryString + "\", \"i\")" )
                .build();

        String query =        "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n";
        query = query + "SELECT (?subject AS ?" + URI + ") (str(?prefLabel) AS ?" + LABEL + ")\n";
        query = query + "WHERE {\n" +
                "\t?subject ?predicate skos:Concept .\n" +
                "\t?subject skos:prefLabel ?prefLabel .\n";
        query = query + "\tFILTER (lang(?prefLabel) = '" + lang.toString() + "')\n" +
                "\tFILTER regex(?prefLabel, \"^" + queryString + "\", \"i\")\n" +
                "}\n";

        assertEquals("query does not match expected", query, built);

    }

    @Test
    public void buildsAnotherExample() {
        String uri = "http://goog.fi";
        String URI = "http://gofore.com";
        String LABEL = "label";
        String lang = "fi";

        String built = new SparqlQueryBuilder()
                .prefix("skos", "http://www.w3.org/2004/02/skos/core")
                .select("(str(" + urify(uri) + ") AS " + variable(URI) + ") (str(?prefLabel) AS " + variable(LABEL) + ")")
                .where(urify(uri), "?predicate", "skos:Concept")
                .where(urify(uri), "skos:prefLabel", "?prefLabel")
                .filter("(lang(?prefLabel) = '" + lang + "')")
                .build();



        String query        = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n";
        query = query + "SELECT (str(<" + uri + ">) AS ?" + URI + ") (str(?prefLabel) AS ?" + LABEL + ")\n";
        query = query + "WHERE {\n" +
                "\t<" + uri + "> ?predicate skos:Concept .\n" +
                "\t<" + uri + "> skos:prefLabel ?prefLabel .\n";
        query = query + "\tFILTER (lang(?prefLabel) = '" + lang + "')\n" +
                "}\n";

        assertEquals("query does not match expected", query, built);

    }

}
