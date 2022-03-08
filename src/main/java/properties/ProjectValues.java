package properties;

import utils.ReadPropertyFile;

import java.util.Map;

/** Class containing the values (integers, strings, etc.) used in the project.
 * Use the method init() to read the values.properties and initialize the fields with the
 * corresponding values found in that property file.
 *
 * */
public class ProjectValues {
    /** How many queries we want to create when we are creating queries. Used in GenerateQueries.java */
    public static int queriesToCreate;
    /** The value used to compute the standard deviation of the gaussian distribution used to build queries.
     * The bigger, the more concentrated around the mean the gaussian is. Used in GenerateQueries.java */
    public static int alpha;

    /** Time available to compute a select query. Default at 30 seconds*/
    public static int timeoutSelectQueries;
    /** Available time to compute a construct query (i.e., the lineage of a query)*/
    public static int timeoutConstructQueries;
    /** Available time to update the support RDB*/
    public static int timeoutUpdateRDB;

    /** The length of an epoch. At the end of each epoch the update process is executed */
    public static int epochLength;

    /** We need to execute each query more than one time and take the average times, in order to
     * obtain an average value that is representative of the "real" execution time.
     * This value is the number of times we execute each query. NB: in the code this value
     * will be the one you set on the property file - 1. I.e., if you set 10, the value during the
     * execution will be 9.
     * */
    public static int timesOneQueryIsExecuted;

    /** named used for our named graph */
    public static String namedGraphName;

    public static int creditThreshold;

    public static boolean capRequired;

    /** The maximum number of triples allowed in the cache*/
    public static int cap = 10000;
    /** Number of timeframes that we consider. Default at 1 */
    public static int timeframes = 1;

    /** Length (in queries considered) of a timeframe. It means that a timeframe "covers"
     * this number of queries before being considered exhausted. In other implementations
     * you may want to consider a measure of time rather than a measure of considered queries
     * */
    public static int timeframeLenght = 1000;

    public static boolean timeframesRequired = false;

    /** string containing the list of indexes used to create/open the RDF database*/
    public static String indexes = "spoc";

    /** a string (e.g., ONE, TWO, etc.) representing the query type that we are generating
     * Used in GenerateQueries.java
     * */
    public static String whichQueryTypeToCreate;

    /** Set this value to true if we want to print the dimension of the cache. Default is true*/
    public static boolean printCacheSize = true;

    public static boolean existenceCheck = false;

    /** We may uise a relational table to keep the triples of the lineages that we already computed.
     * If this table becomes too big (or for some other reason you do not want to use it),
     * set this to false
     * */
    public static boolean useSupportLineageCache = false;

    /** A query that can be used to generate values used to build other queries
     * */
    public static String sparql_tuples_query = "";

    public static boolean cleanCache = true;

    /** a number representing the total number of queries executed during an experiment.
     * Used to compute statistics.
     * */
    public static int queriesToCheck;

    /** Data to access the PostgreSQL DB */
    public static String host = "localhost";
    public static String port = "5432";
    public static String user = "postgres";
    public static String database = "bsbm100k";
    public static String password = "Ulisse92";
    public static String schema = "public";

    /** Path of the property file. By default, the value is properties/values.properties
     * */
    public static String propertyPath = "properties/values.properties";

    public static void init() {
        Map<String, String> map = ReadPropertyFile.doIt(propertyPath);

        try{
            queriesToCreate = Integer.parseInt(map.get("queriesToCreate"));
            alpha = Integer.parseInt(map.get("alpha"));
            timeoutSelectQueries = Integer.parseInt(map.get("timeoutSelectQueries"));
            timeoutConstructQueries=Integer.parseInt(map.get("timeoutConstructQueries"));
            epochLength=Integer.parseInt(map.get("epochLength"));
            timesOneQueryIsExecuted=Integer.parseInt(map.get("timesOneQueryIsExecuted")) - 1;
            creditThreshold=Integer.parseInt(map.get("creditThreshold"));
            timeoutUpdateRDB=Integer.parseInt(map.get("timeoutUpdateRDB"));
            capRequired=Boolean.parseBoolean(map.get("capRequired"));
            cap=Integer.parseInt(map.get("cap"));
            timeframes = Integer.parseInt(map.get("timeframes"));
            timeframeLenght = Integer.parseInt(map.get("timeframeLenght"));
            timeframesRequired = Boolean.parseBoolean(map.get("timeframesRequired"));
            indexes = map.get("indexes");
            whichQueryTypeToCreate = map.get("whichQueryTypeToCreate");
            printCacheSize = Boolean.parseBoolean(map.get("printCacheSize"));
            existenceCheck = Boolean.parseBoolean(map.get("existenceCheck"));
            useSupportLineageCache = Boolean.parseBoolean(map.get("useSupportLineageCache"));
            cleanCache = Boolean.parseBoolean(map.get("cleanCache"));
            queriesToCheck = Integer.parseInt(map.get("queriesToCheck"));

            if(!timeframesRequired)
                timeframes = 1;
        } catch (Exception e) {
            System.err.println("[ERROR] check the values.properties file. Something is probably missing or wrong");
            queriesToCreate = 10;
            alpha = 20;
            timeoutSelectQueries=30000;
            timeoutConstructQueries=30000;
            epochLength=20;
            timesOneQueryIsExecuted=9;
            creditThreshold=0;
            timeoutUpdateRDB=30000;
            capRequired=false;
            cap=10000;
            timeframes=1;
            timeframeLenght=1;
            timeframesRequired=false;
            indexes = "spoc";
            whichQueryTypeToCreate = "ONE";
            printCacheSize = true;
            existenceCheck = false;
            useSupportLineageCache = false;
            cleanCache = true;
            e.printStackTrace();
        }

        namedGraphName = map.get("namedGraphName");

        host = map.get("host");
        port = map.get("port");
        user= map.get("user");
        database = map.get("database");
        password = map.get("password");
        schema = map.get("schema");
    }

    public static void init(String arg) {
        if(arg != null) {
            propertyPath = arg;
        }
        init();
    }

    /** Use it to build the string necessary to connect to the PostgreSQL.
     * it needs to be invoked AFTER the init() method, or you will get a useless string
     * with a lot of null values.
     *
     * */
    public static String produceJdbcString() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database + "?user=" + user + "&password=" + password;
    }



}
