package query_generation.templates;

/** Note that we put limits both on the select and construct queries, also in the other classes of this type.
 * We did this because if we do not put a limit in the select, the execution time becames too long. 
 * We also need to put  alimit in the construct query for the same reason. Obviously, 
 * this second limit has to be larger, indicatively equal to <number of triples of an answer> * value of LIMIT 
 * in the corresponding select query. This is necessary in order to capture the whole graph that produces the output of the
 * select query. */
public class BSBMQuery1 {

	public static String prefix = "PREFIX bsbm-inst: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/>\n" +
			"PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";

	public static String select_query = prefix +
			"\n" +
			"SELECT DISTINCT ?product ?label " +
			"\n" +
			"WHERE { \n" +
			" ?product rdfs:label ?label .\n" +
			" ?product a %s .\n" + // productType
			" ?product bsbm:productFeature %s . \n" + // productFeature1
			" ?product bsbm:productFeature %s . \n" + // productFeature2
			"?product bsbm:productPropertyNumeric1 ?value1 . \n" +
			"	FILTER (?value1 > 50) \n" + // decided to go with 50 as the value
			"	}\n" +
			"ORDER BY ?label\n" + // in a large database this operation may make the query unfeasible
			"LIMIT 10";


	public static String construct_query = prefix +
			"CONSTRUCT { " +
			" ?product rdfs:label ?label .\n" +
			" ?product a %s .\n" +
			" ?product bsbm:productFeature %s . \n" +
			" ?product bsbm:productFeature %s . \n" +
			" ?product bsbm:productPropertyNumeric1 ?value1 . \n" +
			" }" +
			"WHERE { \n" +
			" ?product rdfs:label ?label .\n" +
			" ?product a %s .\n" +
			" ?product bsbm:productFeature %s . \n" +
			" ?product bsbm:productFeature %s . \n" +
			" ?product bsbm:productPropertyNumeric1 ?value1 . \n" +
			"	FILTER (?value1 > 50) \n" +
			"	}" +
			"ORDER BY ?label\n" +
			"LIMIT 50" ;

	public static String building_values_query =
			"PREFIX bsbm-inst: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/>\n" +
					"PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
					"\n" +
					"SELECT DISTINCT ?prodType ?prodFeat1 ?prodFeat2\n" +
					"WHERE {\n" +
					" ?product rdfs:label ?label .\n" +
					" ?product a ?prodType.\n" +
					" ?product bsbm:productFeature ?prodFeat1 .\n" +
					" ?product bsbm:productFeature ?prodFeat2 .\n" +
					"?product bsbm:productPropertyNumeric1 ?value1 .\n" +
					"\tFILTER (?value1 > 50)\n" +
					"    FILTER (?prodFeat1 != ?prodFeat2)\n" +
					"\t}\n" +
					"# ORDER BY ?label\n" +
					"LIMIT 1000";

}
