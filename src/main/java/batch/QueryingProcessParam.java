package batch;

import com.beust.jcommander.Parameter;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import properties.ProjectPaths;
import properties.ProjectValues;
import threads.DealWithCapOnTheCacheBashThread;
import threads.LineageComputationThread;
import threads.QueryWholeDBThread;
import utils.*;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** Class that represents a generic process of querying.
 * It contains methods and fields that are used many times.
 * This version of the class uses parameters passed in-line thus to be
 * used more easily with SLURM
 *
 * */
public class QueryingProcessParam extends QueryVault {

    /** Use this file writer to write down the times obtained from the whole DB without the use
     * of any optimization by our parts
     * -- this field is NOT initialized in the constructor of this class
     * */
    protected FileWriter wholeDbFw;

    /** File Writer used to write the results obtained by the experiments
     * performed on the cache -- this value is NOT initialized in the constructor of this class*/
    protected FileWriter cacheFw;

    /** File Writer used to write the time required to compute
     * the lineage of a query (i.e., the corresponding construct query)
     * -- this value is NOT initialized in the constructor of this class
     * */
    protected FileWriter constructFw;

    /** File writer used to write the time required to update
     * the supporting relational database at the end of each epoch
     * -- NB this field is NOT initialized in the constructor of this class */
    protected FileWriter updateRDBFw;

    /** File writer used to write down the time required to
     * compute the cooldown strategy */
    protected FileWriter coolDownWriter;

    /** The current epoch where the process is finding itself
     * */
    public int epoch;

    /** Each operation of insertion/update of a tuple will have this integer, representing
     * the "moment" when we did the operation. This helps in some operations on the RDB
     * */
    public int insertionToken;

    /** The timeframe where the operation is currently located
     * */
    public int timeframe;

    /** path of the file containing the values property file */
    @Parameter(
            names = {"--values_path", "-VP"},
            arity = 1,
            required = true,
            description = "path of the file containing the values property file"
    )
    public String valuesPath;

    /** Main directory where we are operating.
     * */
    @Parameter(
            names = {"--master_directory", "-MD"},
            arity = 1,
            required = true,
            description = "Main directory where we are operating. It is used to build the other paths used in the execution"
    )
    public String masterDirectory;

    /** A string, like '1' or '2' used to build the name of the files that we are using. It is used together
     * with masterDirectory
     * */
    @Parameter(
            names = {"--query_class", "-QC"},
            description = "A string, like '1' or '2' used to build the name of the files that we are using. It is used together\n" + "with masterDirectory",
            arity = 1,
            required = true
    )
    public String queryClass;


    /** A string that represents the schema of the support relational database that we want to use
     * */
    @Parameter(
            names = {"--schema", "-S"},
            description = "A string that represents the schema of the support relational database that we want to use",
            arity = 1
    )
    public String db_schema;



    /** Empty constructor. First we use the input parameters, then we use the {@link setup} method to initialize the parameters
     * .
     * */
    public QueryingProcessParam()  {
        // damn you, log4j -- this line is used to make log4j shut up with its nasty Debug prints
        SilenceLog4J.silence();
    }

    /** After we parsed the input parameters, we obtained the necessary information. We initialized the required fields
     * */
    public void init() {

        ProjectPaths.masterInit(this.masterDirectory, this.queryClass);
        ProjectValues.init(this.valuesPath);

        // open repository to the relational database
        try {
            rdbConnection = PostgreHandler.getConnection(ProjectValues.produceJdbcString(), this.getClass().getName());
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
            System.err.println("[DEBUG]: need to check those values to connect to the database!");
        }
        // open connection to the main triple store database
        repositoryConnection = TripleStoreHandler.getConnection(ProjectPaths.databaseIndexDirectory, TripleStoreHandler.DB);
    }


    /** Runs the select query of the field selectQuery on the whole database.
     * The information about how this execution went is saved in the ReturnBox
     * */
    public ReturnBox runQueryOnWholeDB() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = null;

        future = executor.submit(new QueryWholeDBThread(this));
        ReturnBox result = new ReturnBox();
        long start = System.currentTimeMillis();
        try{
            result = future.get(ProjectValues.timeoutSelectQueries, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            result.queryTime = System.currentTimeMillis() - start;
            result.inTime = false;
            e.printStackTrace();
        } finally {
            if(!future.isDone())
                future.cancel(true);
            if(!executor.isTerminated())
                executor.shutdownNow();
        }

        return result;
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

    /** This method saves the value of the field constructQuery in a file specified by the property
     * supportMemoryFile*/
    protected void saveConstructQuery() {
        // print the query on the support file
        try {
            FileWriter constructQueryWriter = new FileWriter(ProjectPaths.supportTextFile, true);
            constructQueryWriter.write(this.constructQuery + "\n");
            constructQueryWriter.flush();
            constructQueryWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** Computes the lineage of the query currently contained in the field constructQuery
     *
     * */
    protected ReturnBox computeLineage() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = null;
        ReturnBox box = new ReturnBox();

        long start = System.currentTimeMillis();
        // ask to another thread. It will take the time. We also take the time in this method
        // in order to have something to say if we go into timeout exception
        future = executor.submit(new LineageComputationThread(this));
        try {
            box = future.get(ProjectValues.timeoutConstructQueries, TimeUnit.MILLISECONDS);
        }  catch (ExecutionException | InterruptedException | TimeoutException e) {
            box.foundSomething = false;
            box.queryTime = System.currentTimeMillis() - start;
        } finally { // in any case, either we completed or not, the process is closed
            if(!future.isDone())
                future.cancel(true);
            if(!executor.isTerminated())
                executor.shutdownNow();
        }

        return box;
    }

    /** Given a ReturnBox with the results of the execution of a provenance computation,
     * writes the result in the file identified by the constructQueryFile property
     * */
    protected void writeDownResultsAboutLineage(long queryTime) {
        try {
            this.constructFw.write(queryTime + "\n");
            this.constructFw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Given a Return Box containing the results of a query executed on the cache (and eventually on
     * the whole DB), it prints these data on the file whose path is specified in the cacheTimesFile property
     * */
    protected void printResultsFromCacheExperiments(ReturnBox result) throws IOException {

        if(this.executionTime == 0) {
            // at the first execution, print a header indicating that we started a new query
            this.cacheFw.write("# QUERYNO " + this.queryNumber + "\n");
        }

        if(!result.inTime) { // in case the cache required more than the timeout, the whole DB will require even more
            // we just print the timeout
            this.cacheFw.write( "timeout," + result.queryTime + ",-,-,-\n");
            return;
        }

        // if we already found the solution with the cache, write now the result:
        if(result.foundSomething) {
            if(this.executionTime == 0)
                this.cacheFw.write( "hit," + result.queryTime + ",-,-," + result.resultSetSize + "\n");
            else
                this.cacheFw.write( "hit," + result.queryTime + ",-,-,-\n");


        } else {
            // we did not have a hit on the cache. It is therefore necessary to ask to the whole DB
            ReturnBox wholeDbBox = this.runQueryOnWholeDB();
            long totalTime = result.queryTime + wholeDbBox.queryTime;

            if(!result.inTime) {
                this.cacheFw.write("timeout," + totalTime + ",-,-,-\n");
            }

            // in any case, write the result of the computation. In this case, it is a miss
            // We write the fact that this is a miss, the total time required, the time required to access the cache, the time to access the DB, and the
            // dimension of the result set
            if(this.executionTime == 0) // we write the size of the result set only the first time
                this.cacheFw.write("miss," + totalTime + "," + result.queryTime +  "," + wholeDbBox.queryTime + "," + wholeDbBox.resultSetSize + "\n");
            else
                this.cacheFw.write("miss," + totalTime + "," + result.queryTime +  "," + wholeDbBox.queryTime + ",-" + "\n");
        }
    }

    /** Given the construct queries written in the file whose path can be found in the property
     * supportTextFile, it computes the provenances (lineage) of those queries, and
     * put them as lists of strings in a list provided as input parameter
     *
     * */
    protected void computeLineages(List<List<String[]>> lineageBuffer) {
        // open the file that we used as "database" to store the queries
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(ProjectPaths.supportTextFile))) {
            String line = ""; // string that will contain the query
            while( (line = reader.readLine()) != null ) { // read each construct query

                if(line.length() < 1) // a little check just in case
                    continue;

                // set the current construct query
                this.constructQuery = line;
                // see if we already have the lineage of this query in the support RDB
                long startLookupOnDb = System.currentTimeMillis();
                List<String[]> lineage = this.checkIfWeAlreadyComputedTheLineageOfThisQuery();
                if(lineage.size() > 0) {
                    long elapsed = System.currentTimeMillis() - startLookupOnDb;
                    this.writeDownResultsAboutLineage(elapsed);
                    lineageBuffer.add(lineage);
                } else {
                    // first time we see this query
                    // compute the lineage and the time required to do so
                    ReturnBox constructBox = this.computeLineage();
                    // check if all the triples of this lineage actually exist (deal with the OPTIONAL clause)
                    if(ProjectValues.existenceCheck) {
                        this.checkIfLineageTriplesActuallyExistInDatabaseOrAreAProductOfOptional(constructBox);
                    }
                    // write down the time required to compute this lineage
                    this.writeDownResultsAboutLineage(constructBox.queryTime);
                    // add the lineage to the buffer
                    if(constructBox.foundSomething ) {
                        lineageBuffer.add(constructBox.lineage);
                        if(ProjectValues.useSupportLineageCache) // update the cache containing the lineages
                            this.addLineageToRDBCacheForNextTime(constructBox.lineage);
                    }
                }
            } // end of the queries
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("error with file "
                    + ProjectPaths.supportTextFile);
            System.exit(1);
        }

        // as last action, we need to delete the supportTextFile, since now we do not need its values anymore
        try {
            Files.deleteIfExists(Paths.get(ProjectPaths.supportTextFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** In this project we considered some SPARQL SELECT queries with OPTIONAL Clause. When converted
     * in their CONSTRUCT version, these queries may generate an "imperfect" lineage,
     * in the sense that this lineage contains triples that do not actually belong to the database,
     * but which were created by the CONSTRUCT query even if they were not present. This method
     * checks if these triples actually exist. If not, it deletes them from the lineage
     * */
    private void checkIfLineageTriplesActuallyExistInDatabaseOrAreAProductOfOptional(ReturnBox constructBox) {
        List<String[]> originalLineage = constructBox.lineage;
        List<String[]> amendedLineage = new ArrayList<>();

        for(String[] statement : originalLineage) {
            // prepare the strings for the ASK queries. Subject and predicate are always URLs, the object may change
            String subject = "<" + statement[0] + ">";
            String predicate = "<" + statement[1] + ">";
            String object = PrepareObjectForQuery.prepareObjectStringForQuery(statement[2]);

            String askQuery = String.format(SPARQLStrings.ASK_IF_TRIPLE_IS_PRESENT, subject, predicate, object);
            try {
                BooleanQuery q = this.repositoryConnection.prepareBooleanQuery(askQuery);
                boolean result = q.evaluate();// it can be tre (present) or false (absent)
                if(result) {
                    // the triple is actually present
                    amendedLineage.add(statement);
                } // otherwise, we do not do anything
            } catch(MalformedQueryException e) {
                amendedLineage.add(statement);
            }
        }
        // switcheroo -- change the old lineage with the updated one
        constructBox.lineage = amendedLineage;
    }

    /** Having a lineage of the current query in the this.constructQuery field,
     * it adds the triples of this query into the relational supporting database,
     * in order to be quicker next time.
     * */
    private void addLineageToRDBCacheForNextTime(List<String[]> lineage) {
        String queryHash = "";
        // convert the construct query in its hashed version
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
            mDigest.update(this.constructQuery.getBytes());
            queryHash = new String(mDigest.digest());
            queryHash = queryHash.replaceAll("\\u0000", ""); // remove the null characters from our strings
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No such algorithm as SHA-256 here");
            e.printStackTrace();
        }

        String query = String.format(SqlStrings.INSERT_CACHED_LINEAGE, ProjectValues.schema);
        try (PreparedStatement ps = this.rdbConnection.prepareStatement(query)) {
            for(String[] l : lineage) { // for each triple in the lineage, add one tuple in the support table
                ps.setString(1, queryHash);
                ps.setString(2, l[0]);
                ps.setString(3, l[1]);
                ps.setString(4, l[2]);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            throwables.getNextException();
        }


    }

    /** It considers the construct query that we are using and it sees if we already have its lineage
     * saved in memory
     * */
    private List<String[]> checkIfWeAlreadyComputedTheLineageOfThisQuery() {
        List<String[]> lineage = new ArrayList<>();// buffer where we will save the lineage
        String queryHash = "";
        if(!ProjectValues.useSupportLineageCache)
            return lineage; // in case we do not want to lose time/space using the cache, return an empty answer

        // convert the construct query in its hashed version
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
            mDigest.update(this.constructQuery.getBytes());
            queryHash = new String(mDigest.digest());
            queryHash = queryHash.replaceAll("\\u0000", ""); // remove the null characters from our strings

        } catch (NoSuchAlgorithmException e) {
            System.err.println("No such algorithm as SHA-256 here");
            e.printStackTrace();
        }

        String query = String.format(SqlStrings.FIND_CACHED_LINEAGE, ProjectValues.schema);

        try(PreparedStatement ps = this.rdbConnection.prepareStatement(query)) {
            ps.setString(1, queryHash);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                String[] lin = new String[] {rs.getString("subject"), rs.getString("predicate"), rs.getString("object")};
                lineage.add(lin);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lineage;

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

    /** Updates the cache by enforcing the size specified by the cap.
     * */
    protected boolean dealWithTheCap() {
        ReturnBox rb2 = new ReturnBox();
        if(ProjectValues.capRequired ) {
            // code with timeout
            rb2 = this.dealWithTheCapOnTheCacheWithThread();
        } else { // let's just pretend we completed this task in time, since it is not required
            rb2.foundSomething = true;
        }

        // return a boolean that tells us if we did it or not
        return rb2.foundSomething;
    }

    private ReturnBox dealWithTheCapOnTheCacheWithThread() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = null;
        ReturnBox rb = new ReturnBox();

        future = executor.submit(new DealWithCapOnTheCacheBashThread(this));
        try {
            rb = future.get(ProjectValues.timeoutUpdateRDB, TimeUnit.MILLISECONDS);
            rb.foundSomething = true;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        } finally {
            if(!future.isDone())
                future.cancel(true);
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
        return rb;
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
}
