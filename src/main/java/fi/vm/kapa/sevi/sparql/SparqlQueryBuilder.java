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


import java.util.*;

public class SparqlQueryBuilder {

    private Map<String, String> prefixes = new HashMap<>();
    private List<String> selectVariables = new ArrayList<>();
    private List<Triple> whereClauses = new ArrayList<>();
    private List<String> wholeWhereRows = new ArrayList<>();
    private List<String> filters = new ArrayList<>();
    private String orderBy = null;
    private List<String> groupBys = new ArrayList<>();


    public static String variable(String s) {
        return "?"+s;
    }

    public static String urify(String s) {
        return "<" + s + ">";
    }

    public SparqlQueryBuilder prefix(String prefix, String uri) {
        prefixes.put(prefix, uri);
        return this;
    }

    public SparqlQueryBuilder select(String... vars) {
        Collections.addAll(selectVariables, vars);
        return this;
    }

    public SparqlQueryBuilder where(String subject, String predicate, String object) {
        whereClauses.add(new Triple(subject, predicate, object));
        return this;
    }

    public SparqlQueryBuilder where(String whereRow) {
        wholeWhereRows.add(whereRow);
        return this;
    }

    public SparqlQueryBuilder filter(String clause) {
        filters.add(clause);
        return this;
    }

    public SparqlQueryBuilder groupBy(String groupBy) {
        this.groupBys.add(groupBy);
        return this;
    }
    
    public SparqlQueryBuilder orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        addPrefixes(sb);
        if (!selectVariables.isEmpty()) {
            sb.append("SELECT");
            for (String var: selectVariables) {
                sb.append(" ").append(var);
            }
            sb.append("\n");
        }
        addWherePart(sb);
        addGroupBy(sb);
        addOrderBy(sb);
        return sb.toString();
    }

    private void addPrefixes(StringBuilder sb) {
        for (Map.Entry<String, String> stringStringEntry : prefixes.entrySet()) {
            sb.append("PREFIX ").append(stringStringEntry.getKey()).append(": <").append(stringStringEntry.getValue()).append("#>\n");
        }
    }

    private void addWherePart(StringBuilder sb) {
        if (!whereClauses.isEmpty()) {
            sb.append("WHERE {\n");
            addWhereClauses(sb);
            addFilters(sb);
            sb.append("}\n");
        }
    }

    private void addFilters(StringBuilder sb) {
        for (String filter: filters) {
            sb.append("\tFILTER ").append(filter).append("\n");
        }
    }

    private void addWhereClauses(StringBuilder sb) {
        for (Triple clause: whereClauses) {
            sb.append("\t").append(clause.getSubject()).append(" ").append(clause.getPredicate()).append(" ").append(clause.getObject()).append(" .\n");
        }
        for (String row: wholeWhereRows) {
            sb.append("\t").append(row).append(" .\n");
        }
    }

    private void addOrderBy(StringBuilder sb) {
        if (orderBy != null) {
            sb.append("\tORDER BY ").append(orderBy).append("\n");
        }
    }

    private void addGroupBy(StringBuilder sb) {
        if (!groupBys.isEmpty()) {
            sb.append("\tGROUP BY");
            for (String groupBy : groupBys) {
                sb.append(" " + groupBy);
            }
            sb.append("\n");
        }
    }

    private class Triple {

        public Triple(String s, String p, String o) {
            subject = s;
            predicate = p;
            object = o;
        }

        public String getSubject() {
            return subject;
        }

        public String getPredicate() {
            return predicate;
        }


        public String getObject() {
            return object;
        }

        private String subject;
        private String predicate;
        private String object;

    }
}
