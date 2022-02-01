package utils;

public class StripObjectFromDatatype {

    /** to add things in an rdf4j Model, we need to divide the elements
     * <p>
     *     NB: this method defines how strings are inserted in the cache. It is therefore of utmost importance that the
     *     conversion is correct, otherwise the data in the cache will be different than the data in the database,
     *     and thus the queries will not work/they will return different results.
     * </p>
     *
     * */
    public static String[] stripObjectFromDatatype(String obj) {
        if(obj.contains("@")) {
            String content = obj.split("@")[0].replace("\"", "");
            String language = obj.split("@")[1];
            if(language.equals(""))
                language = "en";
            return new String[] {content, language, "@"};
        } else if(obj.contains("^^")) {
            // a general type of datatype
            String content = obj.split("\\^\\^")[0].replace("\"", "");
            String datatype = obj.split("\\^\\^")[1].replace("\"", "");
            // we deal with a certain set of datatype. Maybe you'll need to deal with other datatypes here
            if(datatype.contains("integer"))
                return new String[] {content, datatype, "integer"};
            else if(datatype.contains("double"))
                return new String[] {content, datatype, "double"};
            else if(datatype.contains("float"))
                return new String[] {content, datatype, "float"};
            else if(datatype.contains("date") && !datatype.contains("dateTime") )
                return new String[] {content, datatype, "date"};
            else if(datatype.contains("dateTime"))
                return new String[] {content, datatype, "dateTime"};
            else if(datatype.contains("nonNegativeInteger"))
                return new String[] {content, datatype, "nonNegativeInteger"};
            else if(datatype.contains("gYear"))
                return new String[] {content, datatype, "gYear"};
            else if(datatype.contains("gMonthDay"))
                return new String[] {content, datatype, "gMonthDay"};
            else if(datatype.contains("dbpedia.org/datatype")) {
                // custom datatype of dbPedia
                String[] parts = datatype.split("/");
                String value = parts[parts.length-1];
                return new String[] {content, datatype, "custom", "http://dbpedia.org/datatype/", value};
            } else if(datatype.contains("XMLSchema")){
                String[] parts = datatype.split("#");
                String value = parts[parts.length-1];
                return new String[] {content, datatype, "XMLSchema", parts[0], value};
            } else if(datatype.contains("http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/")) {
                String[] parts = datatype.split("/");
                String value = parts[parts.length-1];
                return new String[] {content, datatype, "custom", "http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/", value};
            }
            else {
                System.err.println("[WARNING!!!] a new datatype found in DBpedia," + datatype);
                return new String[] {content, datatype, "unknown"};
            }

        } else if(obj.startsWith("\"") && obj.endsWith("\"")) {
            // simply a literal, return it as it is
            String content = obj.replaceAll("\"", "");
            return new String[] {content, null, "plain"};
        }
        return null;
    }
}
