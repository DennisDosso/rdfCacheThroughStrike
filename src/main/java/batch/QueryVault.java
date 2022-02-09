package batch;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import properties.ProjectValues;
import utils.ReturnBox;
import utils.SqlStrings;
import utils.TripleStoreHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**A generic class containing values and methods useful for other sub-classes
 * that represent a generic querying process */
public class QueryVault {

    /** The connection to the main RDF database*/
    public RepositoryConnection repositoryConnection;

    public Connection rdbConnection;

    /** The queries that we use. There is a select version and a construct version*/
    public String selectQuery, constructQuery;

    /** number of the query in the list of queries in the file that we use that we want to execute*/
    public int queryNumber;


    /** Number that represents the execution at which we are now.*/
    public int executionTime;

    /** Each operation of insertion/update of a tuple will have this integer, representing
     * the "moment" when we did the operation. This helps in some operations on the RDB
     * */
    public int insertionToken;

    /** The timeframe where the operation is currently located
     * */
    public int timeframe;

    /** File writer used to write down the time required to
     * compute the cooldown strategy */
    protected FileWriter coolDownWriter;

    /** Connection to a cache. This value is NOT initialized in the constructor */
    public RepositoryConnection cacheRepositoryConnection;

    /** Given a triple in the form of a string array made of a subject, a predicate
     * and an object, checks in the RDB if this triple is already present or not.
     * */
    public ReturnBox checkTriplePresence(String[] st) throws SQLException {
        ReturnBox b = new ReturnBox();
        String subject = st[0];
        String predicate = st[1];
        String object = st[2];
        // prepares the query to ask the RDB if the triple is already present
        String check_query = String.format(SqlStrings.CHECK_TRIPLE_PRESENCE, ProjectValues.schema);
        PreparedStatement check_stmt = this.rdbConnection.prepareStatement(check_query);
        check_stmt.setString(1, subject);
        check_stmt.setString(2, predicate);
        check_stmt.setString(3, object);

        ResultSet check_rs = check_stmt.executeQuery();
        if(check_rs.next()) { // if the triple is present, we return the number of strikes
            b.present = true;
            b.strikes = check_rs.getInt(2);
        } else { // the triple was not already in the RDB, return false and a strike count of 0
            b.present = false;
            b.strikes = 0;
        }
        return b;
    }

    /** We found a triple already present in the RDB. Therefore, we update its strike count, the timeframe an the
     * information
     * */
    public void dealWithAlreadyPresentTriple(String s, String s1, String s2, PreparedStatement update_stmt) throws SQLException {
        // update triple in the triples table
        update_stmt.setInt(1, 1); // strikes++
        update_stmt.setInt(2, this.insertionToken);
        update_stmt.setInt(3, this.timeframe);
        update_stmt.setString(4, s);// subject
        update_stmt.setString(5, s1); // predicate
        update_stmt.setString(6, s2); // object

        update_stmt.addBatch();

        // deal with the timeframe table
        this.dealWithUpdateInTimeFramesTable(s, s1, s2);
    }

    /** Inserts a completely new triple in the relational database */
    public void dealWithNewTriple(String s, String s1, String s2, PreparedStatement insert_stmt) throws SQLException {
        insert_stmt.setString(1, s); // subject
        insert_stmt.setString(2, s1); // predicate
        insert_stmt.setString(3, s2); // object
        insert_stmt.setInt(4, this.insertionToken);
        insert_stmt.setInt(5, this.timeframe);

        insert_stmt.executeUpdate();

        // now get the id of this new triple (this id was created on the fly by the database as an autoincrement)
        String sql = String.format(SqlStrings.FIND_TRIPLE_ID, ProjectValues.schema);
        PreparedStatement ps = this.rdbConnection.prepareStatement(sql);
        ps.setString(1, s);
        ps.setString(2, s1);
        ps.setString(3, s2);
        ResultSet rs = ps.executeQuery();
        if(rs.next()) {
            int tripleId = rs.getInt(1);

            // now create a new timeframe triple ad add it to the corresponding table
            sql = String.format(SqlStrings.INSERT_NEW_TIMEFRAME_TRIPLE, ProjectValues.schema);
            ps.close();
            ps = this.rdbConnection.prepareStatement(sql);
            ps.setInt(1, tripleId);
            ps.setInt(2, this.timeframe);
            ps.setInt(3, 1);
            ps.executeUpdate();
        }
//        ConnectionHandler.getConnection().commit();
        ps.close();
        rs.close();
    }

    /** Create a new triple in the current timeframe, or update it if necessary.
     * The timeframe is kept in the RDB and it is recognize through its number,
     * an increasing integer
     * */
    private void dealWithUpdateInTimeFramesTable(String s, String s1, String s2) throws SQLException {
        // first, find the latest timeframe in which this triple was used
        String sql = String.format(SqlStrings.FIND_TRIPLE_ID, ProjectValues.schema);
        PreparedStatement ps = this.rdbConnection.prepareStatement(sql);
        ps.setString(1, s);
        ps.setString(2, s1);
        ps.setString(3, s2);
        ResultSet rs = ps.executeQuery();
        if(rs.next()) {
            // get the id of this triple
            int tripleID = rs.getInt(1);
            /* it seems there is a feature jdbc: when I perform a MAX(...) query, even if the
             * result set is empty, the MAX operator returns 0. I was counting on the return of a null value instead.
             * Thus, here, I first need to check if there is at least one triple with the tripleID among the triplestimeframes
             * tuples. Then, if present, I compute the maximum timeframe*/
            sql = String.format(SqlStrings.FIND_TIMEFRAMES, ProjectValues.schema);
            ps.close();
            ps = this.rdbConnection.prepareStatement(sql);
            ps.setInt(1, tripleID);
            rs.close();
            rs = ps.executeQuery();
            if(rs.next()) {
                // now that we know that there is at least one tuple created during the passed timeframes representing the presence of the triple
                // at at least one different timeframe during the execution.
                // check if we already inserted this triple during this timeframe
                List<Integer> tmfrms = new ArrayList<>();
                tmfrms.add(rs.getInt(1));
                while(rs.next())
                    tmfrms.add(rs.getInt(1));

                if(tmfrms.contains(this.timeframe)) { // we already met this triple during this timeframe
                    // update the current triple with a +1 on the strike count
                    sql =String.format(SqlStrings.UPDATE_TIMEFRAME_TRIPLE, ProjectValues.schema);
                    ps.close();
                    ps = this.rdbConnection.prepareStatement(sql);
                    ps.setInt(1, tripleID);
                    ps.setInt(2, this.timeframe);
                    ps.executeUpdate();
                } else { // this triple was seen in previous timeframes, but it is the first time we see it in this timeframe
                    // insert a new triple in the triplesTimeframes table with the current timeframe
                    sql = String.format(SqlStrings.INSERT_NEW_TIMEFRAME_TRIPLE, ProjectValues.schema);
                    ps.close();
                    ps = this.rdbConnection.prepareStatement(sql);
                    ps.setInt(1, tripleID);
                    ps.setInt(2, this.timeframe);
                    ps.setInt(3, 1);
                    ps.executeUpdate();
                }
            } else {
                // the triple is not present among the timeframe
                // (it was never seen before, or it was removed in the past by the cache budget control algorithms),
                // thus we need to create a new triple
                // insert a new triple in the triplesTimeframes table with the current timeframe
                sql = String.format(SqlStrings.INSERT_NEW_TIMEFRAME_TRIPLE, ProjectValues.schema);
                ps.close();
                ps = this.rdbConnection.prepareStatement(sql);
                ps.setInt(1, tripleID);
                ps.setInt(2, this.timeframe);
                ps.setInt(3, 1);
                ps.executeUpdate();
            }
        }
//        ConnectionHandler.getConnection().commit();
        rs.close();
        ps.close();
    }

    /** Given a construct query in the string parameter, it uses the index indicated in the path of the
     * field repositoryConnection.
     *
     * @return a list of "triples" composing the lineage set. Each triple here is represented as a String array.
     * Each array contains three strings.
     *
     * todo possible improvements on this method are:
     * <ul>
     *     <li>Implement some sort of caching of the lineages using a supporting relational table. - done, control it with the  </li>
     *     <li>Implement the control of the triples composing the lineage to make sure that eventual triples
     *     created due to the OPTIONAL clause being present in the SELECT query and not in the CONSTRUCT,
     *     are not contained in the lineage. - done, control it with the existenceCheck property</li>
     * </ul>
     * */
    public List<String[]> computeQueryLineage(String query) {
        // list containing the lineages
        List<String[]> lineage = new ArrayList<>();

        try{
            GraphQuery gQ = this.repositoryConnection.prepareGraphQuery(query); // prepare the query
            try(GraphQueryResult gQr = gQ.evaluate()) { // execute the query (close it at the end)
                for (Statement st : gQr) {
                    //build the new triple as an array of 3 strings containing subject, predicate and object
                    String[] lin = new String[] {st.getSubject().stringValue(), st.getPredicate().stringValue(), st.getObject().toString()};
                    lineage.add(lin); // add this lineage triple to the whole lineage set
                }
            }
        } catch(MalformedQueryException e) {
            e.printStackTrace();
            System.exit(2);
        }

        return lineage;
    }

    /** Reduces the size of the current timeframe removing the oldest triples in that timeframe
     *
     * @param timeFrameSize the dimension of the current timeframe
     * @param timeframeCap the maximum size allowed for one timeframe
     * */
    public void reduceTimeFrameSize(int timeFrameSize, int timeframeCap) throws SQLException {
        int currentSize = timeFrameSize;

        // remove triples from the timeframe in bunch as long as we need
        do {
            int lowest_insertiontime =  this.getLowestInsertiontime();
            int deletedRows = this.removeTriplesFromThisInsertiontime(lowest_insertiontime);
            currentSize -= deletedRows;
        } while (currentSize > timeframeCap);
    }

    /** Gets  the oldest triples (the oldest lineage block) in the current timeframe */
    private int getLowestInsertiontime() throws SQLException {
        String sq = String.format(SqlStrings.FIND_OLDEST_TRIPLES_IN_TIMEFRAME, ProjectValues.schema, ProjectValues.schema);
        PreparedStatement min = this.rdbConnection.prepareStatement(sq);
        min.setInt(1, this.timeframe);
        ResultSet rs = min.executeQuery();
        if(rs.next()) {
            int res = rs.getInt(1); rs.close(); min.close();
            return  res;
        }
        rs.close(); min.close();
        return -1;
    }

    /** Removes from the current timeframe the triples corresponding to the indicated insertion time*/
    private int removeTriplesFromThisInsertiontime(int insertion_time) throws SQLException {

        // first, find triples with the id corresponding to this insertion time
        List<Integer> triplesIDs = this.findTriplesWithThisInsertionTime(insertion_time);

        // remove them from the current timeframe
        return this.removeTheseTriplesFromCurrentTimeframe(triplesIDs);
    }

    private List<Integer> findTriplesWithThisInsertionTime(int insertion_time) throws SQLException {
        // first, get all the triples from the RDB with this insertion time, and remove them from the cache
        String q = String.format(SqlStrings.GET_TRIPLES_WITH_THIS_INSERTIONTIME, ProjectValues.schema);
        PreparedStatement ps = this.rdbConnection.prepareStatement(q);
        ps.setInt(1, insertion_time);
        List<Integer> tripleIDs = new ArrayList<>();

        // go through the triples and remove them from the cache too
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            tripleIDs.add(rs.getInt(4));
        }
        rs.close(); ps.close();
        return tripleIDs;
    }

    private int removeTheseTriplesFromCurrentTimeframe(List<Integer> triplesIDs) throws SQLException {
        TripleStoreHandler.initDeletion();
        // first, get the number of strikes on the triple in question
        String sql = String.format(SqlStrings.GET_TIMEFRAME_TRIPLE_STRIKES, ProjectValues.schema);
        PreparedStatement ps = this.rdbConnection.prepareStatement(sql);
        int deletedTriples = 0;
        ResultSet rs = null;
        for(int tripleID : triplesIDs) {
            ps.setInt(1, tripleID);
            ps.setInt(2, this.timeframe);
            rs = ps.executeQuery();
            if(rs.next()) {
                int strikes = rs.getInt(1);
                // now update the main table with this reduction of strikes and check if needs to be deleted from the cache
                this.reduceStrikesCountForThisTripleId(tripleID, strikes);
                this.checkIfThisTripleIdNeedsToBeDeletedFromCache(tripleID);
                // finally, delete the triple from the timeframe in the RDB
                deletedTriples += this.removeTripleFromThisTimeframe(tripleID, this.timeframe);
            }
        }
        ps.close(); rs.close();
        TripleStoreHandler.removeTriplesFromCacheUsingDeletionBuilder();
        return deletedTriples;
    }

    /** Given a triple ID and a certain quantity of strikes, reduces the number of strikes from that triple
     * in the main table triples.
     *
     * */
    private void reduceStrikesCountForThisTripleId(int tripleID, int strikes) throws SQLException {
        String sql = String.format(SqlStrings.REDUCE_STRIKE_COUNT, ProjectValues.schema);
        PreparedStatement ps = this.rdbConnection.prepareStatement(sql);
        ps.setInt(1, strikes);
        ps.setInt(2, tripleID);
        ps.executeUpdate();
        ps.close();
    }

    private int removeTripleFromThisTimeframe(int tripleID, int timeframe) throws SQLException {
        String sql = String.format(SqlStrings.DELETE_TRIPLE_FROM_TIMEFRAME, ProjectValues.schema);
        PreparedStatement ps = this.rdbConnection.prepareStatement(sql);
        ps.setInt(1, tripleID);
        ps.setInt(2, timeframe);
        int deleted = ps.executeUpdate();

        return deleted;
    }

    /** Controls if a given triple identified by its id needs to be deleted from the cache
     * (its strike count value went below the threshold).
     * We use the method {@link TripleStoreHandler addTripleToDeletion} as support.
     * We add the triples to a in-RAM database. When you are done using this method, call
     * TripleStoreHandler.removeTriplesFromCacheUsingDeletionBuilder*/
    private void checkIfThisTripleIdNeedsToBeDeletedFromCache(int tripleID) throws SQLException {
        // first, check the strike count of the triple
        String q = String.format(SqlStrings.GET_TRIPLE_STRIKES, ProjectValues.schema);
        PreparedStatement ps = this.rdbConnection.prepareStatement(q);
        ps.setInt(1, tripleID);
        ResultSet rs = ps.executeQuery();
        if(rs.next()) {
            String sub = rs.getString(1);
            String pred = rs.getString(2);
            String obj = rs.getString(3);
            int strikes = rs.getInt(4);
            if(Math.log(strikes + 1) <= ProjectValues.creditThreshold) {
                // it needs to be deleted from cache -- add it to the RAM cache that we will use later
                TripleStoreHandler.addTripleToDeletion(sub, pred, obj);
            }
        }
    }

    /** Implements one cool-down strategy. the strategy is performed when we reach the maximum number of allowed timeframes.
     * This means that, for example, if we set the maximum number of timeframes at 3,
     * at the end of the current timeframe, if we have 3 or more timeframes in memory,
     * we are reduced to the latest 2 and a third new one is started.
     *
     * */
    protected void dealWithTimeframes() {
        // first check if in this execution we need to update the timeframes
        // condition 1: we reached the end of the timeframe
        // condition 2: this is not the very first query
        // condition 3: this is the last time we execute the same query
        // condition 4: the user required to use the timeframe strategy
        if(this.queryNumber % ProjectValues.timeframeLenght == 0 && this.queryNumber > 0 && this.executionTime == ProjectValues.timesOneQueryIsExecuted && ProjectValues.timeframesRequired) {
            if(this.timeframe < ProjectValues.timeframes) {
                // also, in the first (ProjectValues.timeframes - 1) times we DO NOT need to execute the
                // cooldown strategy, we only limit ourselves to increase the timeframe count
                return;
            }

            // here we only implemented the time-based cooldown strategy
            long time =  this.timeBasedCoolDownStrategy();
            // write down the result
            try {
                coolDownWriter.write(this.timeframe + "," + time + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private long timeBasedCoolDownStrategy() {
        long start = System.nanoTime();
        // get the number of the oldest timeframe in the RDB and delete it
        try {
            int oldestTimeframe = (this.timeframe - ProjectValues.timeframes) + 1;
            this.removeOldestTimeframe(oldestTimeframe);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        long elapsed = System.nanoTime() - start;
        return elapsed;
    }

    /** The method deletes from the timeframe table all the tuples belonging to that timeframe.
     * As a consequence, their strike count is updated in the supporting RDB
     *
     * @param oldestTimeframe the number identiying the latest timeframe, which is the one to be deleted*/
    private void removeOldestTimeframe(int oldestTimeframe) throws SQLException {
        // prepare the TripleStoreHandler to held in RAM the triples to be later deleted from the cache
        TripleStoreHandler.initDeletion();

        // first, get triple ID and strike count of the triples that need to be deleted
        String sql = String.format(SqlStrings.GET_DELENDUM_TIMEFRAME_TRIPLES, ProjectValues.schema);
        PreparedStatement ps = this.rdbConnection.prepareStatement(sql);
        ps.setInt(1, oldestTimeframe);
        ResultSet rs = ps.executeQuery();

        while(rs.next()) { // for each triple to be deleted
            int tripleID = rs.getInt(1); // get its ID
            int strikes = rs.getInt(2); // get its strikes number

            // reduce these strikes from the main table
            this.reduceStrikesCountForThisTripleId(tripleID, strikes);
            // check if now this triple needs to be deleted from the cache (went below threshold)
            this.checkIfThisTripleIdNeedsToBeDeletedFromCache(tripleID);
            // finally remove it from the oldest timeframe
            this.removeTripleFromThisTimeframe(tripleID, oldestTimeframe);
        }

        // commit the deletion from the cache
        TripleStoreHandler.removeTriplesFromCacheUsingDeletionBuilder();
    }

    /** It computes the number of triples in the support database that have a strike count
     * > the threshold, i.e., the number of triples composing the cache
     *
     * */
    protected int checkCacheSize() {
        int cacheSize = 0;
        String sqlQuery = String.format(SqlStrings.COUNT_TRIPLES_IN_CACHE, ProjectValues.schema, ProjectValues.creditThreshold);
        try (PreparedStatement st = this.rdbConnection.prepareStatement(sqlQuery)) {
            ResultSet rs = st.executeQuery();
            if(rs.next())
                cacheSize = rs.getInt(1);
            rs.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return cacheSize;
    }
}
