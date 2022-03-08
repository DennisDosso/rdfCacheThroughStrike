package utils;

/** A class that contains the SQL queries used in this project.
 * <p>The table triplestimeframe is used to keep the strike count of each triple
 * at each timeframe. This is necessary to be able to update the
 * strike count of one triple when we remove one whole timeframe
 *
 * </p>
 * */
public class SqlStrings {
    /** inserts a new triple in the 'triples' table. The field strikes is the strike count. The field
     * insertiontime indicates the "moment" when this triple was inserted, the "timeframe"
     * indicates the timeframe when we inserted this triple.
     * */
    public static final String INSERT_TRIPLE = "INSERT INTO %s.triples\n" +
            "(subject, predicate, \"object\", strikes, insertiontime, timeframe)\n" +
            "VALUES(?, ?, ?, 1, ?, ?);";


    public static final String updateHits = "UPDATE %s.triples\n" +
            "SET strikes = strikes + ?,  insertiontime = ?, timeframe = ? \n" +
            "WHERE subject=? AND predicate=? AND \"object\"=?;";


    /** checks in the table triples if a triple (a combination of subject, predicate and object)
     * is already present. It also returns the current number of strikes (if the triple is present)
     *
     * */
    public static final  String CHECK_TRIPLE_PRESENCE =
            "SELECT subject, strikes FROM %s.triples where subject = ? AND predicate = ? AND object = ?";

    /** A query to find the ID of a triple from the table triples, given the subject, predicate
     * and object composing the query
     * */
    public static final String FIND_TRIPLE_ID =
            "SELECT tripleid FROM %s.triples WHERE subject = ? AND predicate = ? AND object = ?;";

    /** Given the tripleID of one triple, finds the corresponding timeframe of when that triple was inserted
     * from the table triplestimeframes
     * */
    public static final String FIND_TIMEFRAMES =
            "SELECT timeframe FROM %s.triplestimeframes where tripleid = ?";

    /** Updates the strike number of one triple in a specific timeframe*/
    public static final String UPDATE_TIMEFRAME_TRIPLE =
            "update %s.triplestimeframes " +
                    "set strikes = strikes + 1 " +
                    "where tripleid = ? and timeframe = ?";

    /** Inserts a new tripe in the table triplestimeframe with the current timeframe and strike count 1*/
    public static final String INSERT_NEW_TIMEFRAME_TRIPLE =
            "INSERT INTO %s.triplestimeframes( " +
                    "tripleid, timeframe, strikes) " +
                    " VALUES (?, ?, ?);";

    public static final String CHECK_HOW_MANY_TRIPLES =
            "SELECT COUNT(*) FROM %s.triplestimeframes where timeframe = ?";

    public static final String CHECK_HOW_MANY_TRIPLES_ABOVE_THRESHOLD =
            "SELECT COUNT(*) FROM %s.triplestimeframes where timeframe = ? and strikes > ?";

    public static final String FIND_OLDEST_TRIPLES_IN_TIMEFRAME =
            "SELECT min(t.insertiontime) FROM %s.triples as t JOIN %s.triplestimeframes AS tf on t.tripleid = tf.tripleid" +
                    " WHERE tf.timeframe = ?";

    public static final String GET_TRIPLES_WITH_THIS_INSERTIONTIME =
            "SELECT subject, predicate, object, tripleid FROM %s.triples WHERE insertiontime = ?";

    public static final String GET_TIMEFRAME_TRIPLE_STRIKES =
            "SELECT strikes FROM %s.triplestimeframes WHERE tripleid = ? and timeframe = ?";

    /** Given a certain strike value, reduce the strike count in table triples for a tripleID
     * */
    public static final String REDUCE_STRIKE_COUNT =
            "UPDATE %s.triples set strikes = strikes - ? where tripleid = ?";

    /** Delete a triole from a timeframe in the triplestimeframes table
     * */
    public static final String DELETE_TRIPLE_FROM_TIMEFRAME =
            "DELETE FROM %s.triplestimeframes where tripleid = ? AND timeframe = ?";

    public static final String GET_TRIPLE_STRIKES =
            "SELECT subject, predicate, object, strikes FROM %s.triples WHERE tripleid = ?";

    /** Gets the triples belonging to a certain timeframe, together with their strike count.
     * This information is used to delete those triples from the timeframe,
     * and also reduce the strike count in the main triples table*/
    public static final String GET_DELENDUM_TIMEFRAME_TRIPLES =
            "SELECT tripleid, strikes FROM %s.triplestimeframes WHERE timeframe = ?";

    public static final String COUNT_TRIPLES_IN_CACHE =
            "SELECT COUNT(*) FROM %s.triples WHERE ln(strikes + 1) > %s ;";

    public static final String FIND_CACHED_LINEAGE =
            "SELECT subject, predicate, object from %s.lineage_cache where query = ?";

    /** Used to insert a triple of a lineage into the support relational table*/
    public static final String INSERT_CACHED_LINEAGE =
            "INSERT INTO %s.lineage_cache (query, subject, predicate, object) VALUES (?, ?, ?, ?)";

    public static final String GET_BASELINE_ANSWER =
            "SELECT value from %s.baselinecache WHERE query_hash = ?;";

    public static final String ADD_TUPLE_TO_RDB_CACHE =
            "INSERT INTO %s.baselinecache(\n" +
                    "\tquery_number, query_hash, value)\n" +
                    "\tVALUES (?, ?, ?);";

    public static final String CHECK_HOW_MANY_TRIPLES_IN_RDB_CACHE =
            "SELECT COUNT(*) FROM %s.baselinecache";

    /** Used to find the query number of the query that was last used in the rdb cache*/
    public static final String FIND_OLDEST_QUERY_NUMBER_IN_BASELINECACHE =
            "SELECT min(query_number) FROM %s.baselinecache";

    public static final String DELETE_LEAST_RECENTLY_USED_QUERY_IN_BASELINECACHE =
            "DELETE FROM %s.baselinecache where query_number = ?";

    public static final String UPDATE_RECENTLY_USED_QUERY_NUMBER =
            "UPDATE %s.baselinecache set query_number = ? where query_hash = ?";

}
