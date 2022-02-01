package utils;

import org.apache.commons.validator.routines.UrlValidator;

public class PrepareObjectForQuery {
    /** Given an object string, obtained from a .toString() method, that may for example
     * be in the form "something"@en if string Litera, "114"^^xsd:int if integer,
     * or a url, checks if it is a url. If it is a URL, it returns it surrounded by <...>,
     * ready to be used for example in a SPARQL query. Otherwise, it returns it as it is*/
    public static String prepareObjectStringForQuery(String obj) {
        UrlValidator urlValidator = new UrlValidator();
        if(urlValidator.isValid(obj)) { // in case the object is a resource
            return "<" + obj + ">";
        }
        else
            return obj;
    }
}
