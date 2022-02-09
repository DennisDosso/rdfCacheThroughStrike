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

/** Class that represents a general process of querying.
 * It contains methods and fields that are used many times*/
public class QueryingProcess extends QueryVault {


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

    /** The current epoch where the process is finding itself
     * */
    public int epoch;


    /** Constructor that can take different types of parameters
     *
     * */
    public QueryingProcess(String[] args) {
        // damn you, log4j -- this line is used to make log4j shut up with its nasty Debug prints
        SilenceLog4J.silence();

        if(args.length >= 4) {
            this.queryNumber =  Integer.parseInt(args[0]);
            this.executionTime = Integer.parseInt(args[1]);
            ProjectPaths.init(args[2]);
            ProjectValues.init(args[3]);
        } else {
            ProjectPaths.init();
            ProjectValues.init();
        }

        // open repository to the relational database
        try {
            rdbConnection = PostgreHandler.getConnection(ProjectValues.produceJdbcString(), this.getClass().getName());
        } catch (SQLException e) {
            e.printStackTrace();
//            System.exit(1);
            System.err.println("[DEBUG]: need to check those values to connect to the database!");
        }
        // open connection to the main triple store database
        repositoryConnection = TripleStoreHandler.getConnection(ProjectPaths.databaseIndexDirectory, TripleStoreHandler.DB);
    }

    public QueryingProcess()  {
        // damn you, log4j -- this line is used to make log4j shut up with its nasty Debug prints
        SilenceLog4J.silence();

        ProjectPaths.init();
        ProjectValues.init();

        // open repository to the relational database
        try {
            rdbConnection = PostgreHandler.getConnection(ProjectValues.produceJdbcString(), this.getClass().getName());
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
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
        long start = System.nanoTime();
        try{
            result = future.get(ProjectValues.timeoutSelectQueries, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            result.queryTime = System.nanoTime() - start;
            result.inTime = false;
        } finally {
            if(!future.isDone())
                future.cancel(true);
            if(!executor.isTerminated())
                executor.shutdownNow();
        }

        return result;
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
            box.nanoTime = System.currentTimeMillis() - start;
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
                    if(constructBox.foundSomething) {
                        lineageBuffer.add(constructBox.lineage);
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




}
