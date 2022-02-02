package query_generation.templates;

public class BSBMQuery10 {
    public static String select = "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "\n" +
            "SELECT DISTINCT ?offer ?price\n" +
            "WHERE {\n" +
            "	?offer bsbm:product %s .\n" + // param1 - 1
            "	?offer bsbm:vendor ?vendor .\n" +
            " ?offer dc:publisher ?vendor .\n" +
            "	?vendor bsbm:country <http://downlode.org/rdf/iso-3166/countries#US> .\n" +
            "	?offer bsbm:deliveryDays ?deliveryDays .\n" +
            "	FILTER (?deliveryDays <= 3)\n" +
            "	?offer bsbm:price ?price .\n" +
            " ?offer bsbm:validTo ?date .\n" +
            " FILTER (?date > \"2008-02-10T00:00:00\"^^xsd:dateTime )\n" +
            "}\n" +
			"ORDER BY xsd:double(str(?price))\n" +
            "LIMIT 5";

    public static String construct = "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "PREFIX dataFromProducer3: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromProducer3/>\n" +
            "\n" +
            "CONSTRUCT{?offer bsbm:product %s .\n" + // param1 - 1
            "?offer bsbm:vendor ?vendor .\n" +
            " 	?offer dc:publisher ?vendor .\n" +
            "	?vendor bsbm:country <http://downlode.org/rdf/iso-3166/countries#US> .\n" +
            "	?offer bsbm:deliveryDays ?deliveryDays .\n" +
            "?offer bsbm:price ?price .\n" +
            " ?offer bsbm:validTo ?date .}\n" +
            "WHERE {\n" +
            "	?offer bsbm:product %s .\n" + // param1 - 2
            "	?offer bsbm:vendor ?vendor .\n" +
            " 	?offer dc:publisher ?vendor .\n" +
            "	?vendor bsbm:country <http://downlode.org/rdf/iso-3166/countries#US> .\n" +
            "	?offer bsbm:deliveryDays ?deliveryDays .\n" +
            "	FILTER (?deliveryDays <= 3)\n" +
            "	?offer bsbm:price ?price .\n" +
            " ?offer bsbm:validTo ?date .\n" +
            " FILTER (?date >= \"2008-02-10T00:00:00\"^^xsd:dateTime )\n" +
            "}\n" +
			"ORDER BY xsd:double(str(?price)) \n" +
            "LIMIT 70";
}
