package query_generation.templates;

public class BSBMQuery2 {

    public static String prefix = "PREFIX bsbm-inst: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n";

    public static String select = prefix +
            "SELECT ?label ?comment ?producer ?productFeature ?propertyTextual1 ?propertyTextual2 ?propertyTextual3\n" +
            " ?propertyNumeric1 ?propertyNumeric2 ?propertyTextual4 ?propertyTextual5 ?propertyNumeric4 \n" +
            "WHERE {\n" +
            "   %s rdfs:label ?label .\n" + // 1
            "	%s rdfs:comment ?comment .\n" + // 2
            "	%s bsbm:producer ?p .\n" + // 3
            "	?p rdfs:label ?producer .\n" +
            " %s dc:publisher ?p . \n" + // 4
            "	%s bsbm:productFeature ?f .\n" + // 5
            "	?f rdfs:label ?productFeature .\n" +
            "	%s bsbm:productPropertyTextual1 ?propertyTextual1 .\n" + // 6
            "	%s bsbm:productPropertyTextual2 ?propertyTextual2 .\n" + // 7
            "   %s bsbm:productPropertyTextual3 ?propertyTextual3 .\n" + // 8
            "	%s bsbm:productPropertyNumeric1 ?propertyNumeric1 .\n" + // 9
            "	%s bsbm:productPropertyNumeric2 ?propertyNumeric2 .\n" + // 10
            "	OPTIONAL { %s bsbm:productPropertyTextual4 ?propertyTextual4 }\n" + // 11
            " OPTIONAL { %s bsbm:productPropertyTextual5 ?propertyTextual5 }\n" + // 12
            " OPTIONAL { %s bsbm:productPropertyNumeric4 ?propertyNumeric4 }\n" + // 13
            "} ";

    public static String construct = "PREFIX bsbm-inst: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/>\n" +
            "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "\n" +
            "CONSTRUCT{ " +
            " %s rdfs:label ?label .\n" + //1
            "	%s rdfs:comment ?comment .\n" + //2
            "	%s bsbm:producer ?p .\n" +  //3
            "	?p rdfs:label ?producer .\n" +
            " %s dc:publisher ?p . \n" +  //4
            "	%s bsbm:productFeature ?f .\n" + //5
            "	?f rdfs:label ?productFeature .\n" +
            "	%s bsbm:productPropertyTextual1 ?propertyTextual1 .\n" + // 6
            "	%s bsbm:productPropertyTextual2 ?propertyTextual2 .\n" + // 7
            " %s bsbm:productPropertyTextual3 ?propertyTextual3 .\n" + // 8
            "	%s bsbm:productPropertyNumeric1 ?propertyNumeric1 .\n" + // 9
            "	%s bsbm:productPropertyNumeric2 ?propertyNumeric2 .\n" + // 10
            "	%s bsbm:productPropertyTextual4 ?propertyTextual4 .\n" + // 11
            " 	%s bsbm:productPropertyTextual5 ?propertyTextual5 .\n" + // 12
            " 	%s bsbm:productPropertyNumeric4 ?propertyNumeric4 .\n" +  //13
            "}" +
            "WHERE {\n" +
            " %s rdfs:label ?label .\n" + //14
            "	%s rdfs:comment ?comment .\n" + //15
            "	%s bsbm:producer ?p .\n" + //16
            "	?p rdfs:label ?producer .\n" +
            " %s dc:publisher ?p . \n" + //17
            "	%s bsbm:productFeature ?f .\n" + //18
            "	?f rdfs:label ?productFeature .\n" +
            "	%s bsbm:productPropertyTextual1 ?propertyTextual1 .\n" + //19
            "	%s bsbm:productPropertyTextual2 ?propertyTextual2 .\n" + // 20
            " %s bsbm:productPropertyTextual3 ?propertyTextual3 .\n" + // 21
            "	%s bsbm:productPropertyNumeric1 ?propertyNumeric1 .\n" + // 22
            "	%s bsbm:productPropertyNumeric2 ?propertyNumeric2 .\n" +  // 23
            "	OPTIONAL { %s bsbm:productPropertyTextual4 ?propertyTextual4 }\n" + //24
            " OPTIONAL { %s bsbm:productPropertyTextual5 ?propertyTextual5 }\n" + //25
            " OPTIONAL { %s bsbm:productPropertyNumeric4 ?propertyNumeric4 }\n" + //26
            "} LIMIT 100";
}
