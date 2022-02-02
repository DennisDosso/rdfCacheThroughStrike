package query_generation.templates;

public class BSBMQuery8 {

    public static String select = "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "PREFIX rev: <http://purl.org/stuff/rev#>\n" +
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
            + "SELECT ?title ?text ?reviewDate ?reviewer ?reviewerName ?rating1 ?rating2 ?rating3 ?rating4 \n" +
            "WHERE { \n" +
            "	?review bsbm:reviewFor %s .\n" + // param1
            "	?review dc:title ?title .\n" +
            "	?review rev:text ?text .\n" +
            "	FILTER langMatches( lang(?text), \"EN\" ) \n" +
            "	?review bsbm:reviewDate ?reviewDate .\n" +
            "	?review rev:reviewer ?reviewer .\n" +
            "	?reviewer foaf:name ?reviewerName .\n" +
            "	OPTIONAL { ?review bsbm:rating1 ?rating1 . }\n" +
            "	OPTIONAL { ?review bsbm:rating2 ?rating2 . }\n" +
            "	OPTIONAL { ?review bsbm:rating3 ?rating3 . }\n" +
            "	OPTIONAL { ?review bsbm:rating4 ?rating4 . }\n" +
            "}\n" +
			"ORDER BY DESC(?reviewDate)\n" +
            "LIMIT 5"
            ;

    public static String construct = "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "PREFIX rev: <http://purl.org/stuff/rev#>\n" +
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
            "\n" +
            "CONSTRUCT {?review bsbm:reviewFor %s .\n" + // param1 - 1
            "	?review dc:title ?title .\n" +
            "	?review rev:text ?text .\n" +
            "	?review bsbm:reviewDate ?reviewDate .\n" +
            "	?review rev:reviewer ?reviewer .\n" +
            "	?reviewer foaf:name ?reviewerName .\n" +
            "    ?review bsbm:rating1 ?rating1 .\n" +
            "    ?review bsbm:rating2 ?rating2 .\n" +
            "    ?review bsbm:rating3 ?rating3 .\n" +
            "    ?review bsbm:rating4 ?rating4 .\n" +
            "}\n" +
            "WHERE { \n" +
            "	?review bsbm:reviewFor %s .\n" + // param1 - 1
            "	?review dc:title ?title .\n" +
            "	?review rev:text ?text .\n" +
            "	FILTER langMatches( lang(?text), \"EN\" ) \n" +
            "	?review bsbm:reviewDate ?reviewDate .\n" +
            "	?review rev:reviewer ?reviewer .\n" +
            "	?reviewer foaf:name ?reviewerName .\n" +
            "	OPTIONAL { ?review bsbm:rating1 ?rating1 . }\n" +
            "	OPTIONAL { ?review bsbm:rating2 ?rating2 . }\n" +
            "	OPTIONAL { ?review bsbm:rating3 ?rating3 . }\n" +
            "	OPTIONAL { ?review bsbm:rating4 ?rating4 . }\n" +
            "}\n" +
			"ORDER BY DESC(?reviewDate)" +
            "LIMIT 50";

}
