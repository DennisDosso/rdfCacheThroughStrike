package query_generation.templates;

public class BSBMQuery6 {

    public static final String select = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "\n" +
            "SELECT ?product ?label\n" +
            "WHERE {\n" +
            "?product rdfs:label ?label .\n" +
            " ?product rdf:type bsbm:Product .\n" +
            "FILTER regex(?label, \"%s\")\n" + // param1
            "}"; // no limit for now

    public static final String construct = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "\n" +
            "CONSTRUCT {" +
            "	?product rdfs:label \"%s\" .\n" + //param1 - 1
            " ?product rdf:type bsbm:Product .\n" +
            "}"+
            "WHERE { \n" +
            "	?product rdfs:label \"%s\" .\n" + //param1 - 2
            " ?product rdf:type bsbm:Product .\n" +
            "}"
            + " LIMIT 100"; // decided to contain the output in case there are too many triples

    public static final String building_values_query =
            "SELECT distinct ?label\n" +
            "WHERE {\n" +
            "?product rdfs:label ?label .\n" +
            "?product rdf:type bsbm:Product .\n" +
            "}\n" +
            "LIMIT 1000";
}
