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


    /** File where results obtained from the whole DB are saved. Used when we compute statistics */
    public static String wholeDBresultFile;
    /** File where results obtained from the execution on cache are stored. Used to compute statistics */
    public static String cacheResultFile;

    /** Value added to deal with multiple executions.
     * The master directory is a starting directory from where we can read/write the things that we need*/
    public static String masterDirectory;

    /** a string representing the current query class.
     * Used to build the paths of this class*/
    public static String currentQueryClass;

    /** File containing the list of values of which we need to compute the average.
     * Used when computing statistics
     * */
    public static String averageResultFile;

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
        wholeDBresultFile = map.get("wholeDBresultFile");
        cacheResultFile = map.get("cacheResultFile");
        averageResultFile = map.get("averageResultFile");
    }

    public static void init(String args) {
        if(args != null) {
            propertiesFilePath = args;
            init();
        } else {
            propertiesFilePath = "properties/paths.properties";
            init();
        }
    }

    /** This init method uses only the base directory masterDir to build all the other paths.
     * You need however to follow the type of file system as described by the below description,
     * or it won't work.
     * */
    public static void masterInit(String masterDir, String cQC) {
//        propertiesFilePath = pathsPath;
        masterDirectory = masterDir;
        currentQueryClass = cQC;

        // first, get information from our properties
        rdfFilePath = masterDirectory + "/turtle/dataset.ttl";
        ttlFilesDirectory = masterDirectory + "/turtle/";
        databaseIndexDirectory = masterDirectory + "/db";
        queryBuildingValuesFile = masterDirectory + "/building_query_values";
        selectQueryFile = masterDirectory + "/queries/Q" + currentQueryClass + ".txt";
        constructQueryFile = masterDirectory + "/queries/construct_Q" + currentQueryClass + ".txt";
        cacheDirectory = masterDirectory + "/cache";
        wholeDbTimesFile = masterDirectory + "/results/whole_db/Q" + currentQueryClass +"_whole_db_results.txt";
        cacheTimesFile = masterDirectory + "/results/cache/Q" + currentQueryClass + "_cache_times.txt";
        supportTextFile = masterDirectory + "/results/cache/support_query_file.txt";
        constructTimesFile = masterDirectory + "/results/cache/Q" + currentQueryClass + "_construct_times.txt";;
        coolDownTimesFile = masterDirectory + "/results/cache/Q" + currentQueryClass + "_cool_down_times.txt";;
        updateRDBTimesFile = masterDirectory + "/results/cache/Q" + currentQueryClass + "_rdb_times.txt";;

        wholeDBresultFile = wholeDbTimesFile;
        cacheResultFile = updateRDBTimesFile;

    }
}
