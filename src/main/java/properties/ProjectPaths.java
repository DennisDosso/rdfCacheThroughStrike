package properties;

import utils.ReadPropertyFile;

import java.util.Map;

/** This class contains the paths used in the project.
 *
 * Run the init() method to let this class read the file paths.proporties and initialize each field
 * with a corresponding value
 *
 * */
public class ProjectPaths {

    /** path of the file with the properties to be read.
     * If nothing is provided, the default value id "properties/paths.properties"*/
    public static String propertiesFilePath = "properties/paths.properties";

    /** path of the file containing the rdf database in some format, e.g. turtle
     * */
    public static String rdfFilePath;
    /** Directory containing the .ttl files with the information to add to the RDF database
     * */
    public static String ttlFilesDirectory;
    /** Path of the triplestore we are using*/
    public static String databaseIndexDirectory;
    /** Path of the file containing the values used to build a query*/
    public static String queryBuildingValuesFile;
    /** Path of the file where to write the select queries*/
    public static String selectQueryFile;
    /** Path of the file where to write the construct queries*/
    public static String constructQueryFile;
    /** Path of the file where to write */
    public static String updateRDBTimesFile;
    /** Path of the file where to write the times required to perform the cool down strategy */
    public static String coolDownTimesFile;
    /** Where we db working as cache is stored */
    public static String cacheDirectory;
    /** The path of the file where we write the times that are result of queries performed on the DB without cache*/
    public static String wholeDbTimesFile;
    /** Where we write the times that are results of queries performed on the cache */
    public static String cacheTimesFile;
    /** Path of the file where we write the times required to compute the construct queries, i.e. compute the provenances */
    public static String constructTimesFile;
    /** A file that we use as support. We write things here (such as construct queries) that we will need
     * when doing operations of update that are not executed at each query*/
    public static String supportTextFile;

    public static void init() {
        Map<String, String> map = ReadPropertyFile.doIt(propertiesFilePath);

        rdfFilePath = map.get("rdfFilePath");
        ttlFilesDirectory = map.get("ttlFilesDirectory");
        databaseIndexDirectory = map.get("databaseIndexDirectory");
        queryBuildingValuesFile = map.get("queryBuildingValuesFile");
        selectQueryFile = map.get("selectQueryFile");
        constructQueryFile =map.get("constructQueryFile");
        cacheDirectory = map.get("cacheDirectory");
        wholeDbTimesFile = map.get("wholeDbTimesFile");
        cacheTimesFile = map.get("cacheTimesFile");
        supportTextFile = map.get("supportTextFile");
        constructTimesFile = map.get("constructTimesFile");
        coolDownTimesFile = map.get("coolDownTimesFile");
        updateRDBTimesFile = map.get("updateRDBTimesFile");
    }

    public static void init(String[] args) {
        if(args.length > 0) {
            propertiesFilePath = args[0];
            init();
        } else {
            propertiesFilePath = "properties/paths.properties";
            init();
        }
    }
}
