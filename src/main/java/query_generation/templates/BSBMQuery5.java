package query_generation.templates;

public class BSBMQuery5 {

    public static final String select = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "\n" +
            "SELECT DISTINCT ??product ?productLabel\n" +
            "WHERE { \n" +
            "?product rdfs:label ?productLabel .\n" +
            "    FILTER (%s != ?product)\n" + // param1
            "%s bsbm:productFeature ?prodFeature .\n" + // param 1- 2
            "?product bsbm:productFeature ?prodFeature .\n" +
            "%s bsbm:productPropertyNumeric1 ?origProperty1 .\n" + //param1 - 3
            "?product bsbm:productPropertyNumeric1 ?simProperty1 .\n" +
            "FILTER (?simProperty1 < (?origProperty1 + 120) && ?simProperty1 > (?origProperty1 - 120))\n" +
            "%s bsbm:productPropertyNumeric2 ?origProperty2 .\n" + // param1 - 4
            "?product bsbm:productPropertyNumeric2 ?simProperty2 .\n" +
            "FILTER (?simProperty2 < (?origProperty2 + 170) && ?simProperty2 > (?origProperty2 - 170))\n" +
            "}\n" +
            "ORDER BY ?productLabel " +
            "LIMIT 5";

    public static final String construct = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "\n" +
            "CONSTRUCT{ " +
            "?product rdfs:label ?productLabel .\n" +
            "%s bsbm:productFeature ?prodFeature .\n" + // param1 - 1
            "%s bsbm:productFeature ?prodFeature .\n" + // param 1- 2
            "?product bsbm:productFeature ?prodFeature .\n" +
            "%s bsbm:productPropertyNumeric1 ?origProperty1 .\n" + //param1 - 3
            "?product bsbm:productPropertyNumeric1 ?simProperty1 .\n" +
            "%s bsbm:productPropertyNumeric2 ?origProperty2 .\n" + // param1 - 4
            "?product bsbm:productPropertyNumeric2 ?simProperty2 .\n" +
            "}" +
            "WHERE { \n" +
            "?product rdfs:label ?productLabel .\n" +
            "    FILTER (%s != ?product)\n" + // param1 - 5
            "%s bsbm:productFeature ?prodFeature .\n" + // param 1- 6
            "?product bsbm:productFeature ?prodFeature .\n" +
            "%s bsbm:productPropertyNumeric1 ?origProperty1 .\n" + //param1 - 7
            "?product bsbm:productPropertyNumeric1 ?simProperty1 .\n" +
            "FILTER (?simProperty1 < (?origProperty1 + 120) && ?simProperty1 > (?origProperty1 - 120))\n" +
            "%s bsbm:productPropertyNumeric2 ?origProperty2 .\n" + // param1 - 8
            "?product bsbm:productPropertyNumeric2 ?simProperty2 .\n" +
            "FILTER (?simProperty2 < (?origProperty2 + 170) && ?simProperty2 > (?origProperty2 - 170))\n" +
            "}\n" +
            "ORDER BY ?productLabel " +
            "LIMIT 40";


    public static final String building_values_query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "\n" +
            "SELECT DISTINCT ?pxyz\n" +
            "WHERE {\n" +
            "?product rdfs:label ?productLabel .\n" +
            "FILTER (?pxyz != ?product)\n" +
            "?pxyz bsbm:productFeature ?prodFeature .\n" +
            "?product bsbm:productFeature ?prodFeature .\n" +
            "?pxyz bsbm:productPropertyNumeric1 ?origProperty1 .\n" +
            "?product bsbm:productPropertyNumeric1 ?simProperty1 .\n" +
            "FILTER (?simProperty1 < (?origProperty1 + 120) && ?simProperty1 > (?origProperty1 - 120))\n" +
            "?pxyz bsbm:productPropertyNumeric2 ?origProperty2 .\n" +
            "?product bsbm:productPropertyNumeric2 ?simProperty2 .\n" +
            "FILTER (?simProperty2 < (?origProperty2 + 170) && ?simProperty2 > (?origProperty2 - 170))\n" +
            "}\n" +
            "LIMIT 1000";
}
