package query_generation.templates;

public class BSBMQuery3 {

    public static String prefix = "PREFIX bsbm-inst: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n ";

    public static String select = prefix +
            "SELECT ?product ?label\n" +
            "WHERE {\n" +
            " ?product rdfs:label ?label .\n" +
            " ?product a %s .\n" + // param1 - 1
            "?product bsbm:productFeature %s .\n" + // param2 - 2
            "?product bsbm:productPropertyNumeric1 ?p1 .\n" +
            "FILTER ( ?p1 > 100 ) \n" +
            "?product bsbm:productPropertyNumeric3 ?p3 .\n" +
            "FILTER (?p3 < 300 )\n" +
            " OPTIONAL { \n" +
            " ?product bsbm:productFeature %s .\n" + // param 3 - 3
            " ?product rdfs:label ?testVar }\n" +
//            " FILTER (!bound(?testVar)) \n" + // this command makes the query unfeasible also for small DBs
            "}\n" +
            "ORDER BY ?label\n" +
            "LIMIT 10";

    public static String construct = prefix +
            "CONSTRUCT {\n" +
            "    ?product rdfs:label ?label .\n" +
            "    ?product a %s .\n" + // param1 - 1
            "    ?product bsbm:productFeature %s .\n" + // param2 - 2
            "    ?product bsbm:productPropertyNumeric1 ?p1 .\n" +
            "    ?product bsbm:productPropertyNumeric3 ?p3 .\n" +
            "     ?product bsbm:productFeature %s .\n" + // param3 - 3
            "     ?product rdfs:label ?testVar .\n" +
            "} WHERE {\n" +
            " ?product rdfs:label ?label .\n" +
            " ?product a %s .\n" + // param1 - 4
            "    ?product bsbm:productFeature %s .\n" + // param2 - 5
            "    ?product bsbm:productPropertyNumeric1 ?p1 .\n" +
            "    FILTER ( ?p1 > 100 ) \n" +
            "    ?product bsbm:productPropertyNumeric3 ?p3 .\n" +
            "    FILTER ( ?p3 < 300 )\n" +
            "    OPTIONAL { \n" +
            "\t ?product bsbm:productFeature %s .\n" + // param3 - 5
            "     ?product rdfs:label ?testVar\n" +
            "     }\n" +
            "}\n" +
            "ORDER BY ?label\n" +
            "LIMIT 70";

    public static String building_values_query =
            "PREFIX bsbm-inst: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "\n" +
            "SELECT distinct ?product_type ?product_feature_1 ?product_feature_2\n" +
            "WHERE {\n" +
            "?product rdfs:label ?label .\n" +
            "?product a ?product_type .\n" +
            "?product bsbm:productFeature ?product_feature_1 .\n" +
            "?product bsbm:productPropertyNumeric1 ?p1 .\n" +
            "FILTER ( ?p1 > 100 )\n" +
            "?product bsbm:productPropertyNumeric3 ?p3 .\n" +
            "FILTER ( ?p3 < 300 )\n" +
            "OPTIONAL {\n" +
            "?product bsbm:productFeature ?product_feature_2 .\n" +
            "?product rdfs:label ?testVar\n" +
            "FILTER (?product_feature_1 != ?product_feature_2)}\n" +
            "}\n" +
            "LIMIT 1000";
}
