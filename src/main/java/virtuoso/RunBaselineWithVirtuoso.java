package virtuoso;

import com.beust.jcommander.JCommander;
import properties.ProjectPaths;
import properties.ProjectValues;
import threads.*;
import utils.*;

import threads.virtuoso.QueryWholeDBAndSaveDataThread;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.*;

/** Use this to run queries on the whole database using an RDB table as cache, namely, baselinecache.
 * This table keeps the results of the queries as tuples (one tuple ofor each result of the
 * query in SPARQL). It also implements a maximum cap limit on the cache, using a LRU algorithm to reduce
 * the size.
 * */
public class RunBaselineWithVirtuoso extends VirtuosoQueryingProcess {

    public RunBaselineWithVirtuoso() {
        super();
    }


    public void init() {
        super.init();

        // open file writers
        try {
            cacheFw = new FileWriter(ProjectPaths.cacheTimesFile, true);
            updateRDBFw = new FileWriter(ProjectPaths.updateRDBTimesFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // degenerate case in which this is the very first query
        if(this.queryNumber == 0) {
            this.timeframe = 1;
            this.epoch = 1;
            return;
        }

        this.epoch = (int) Math.ceil((float) this.queryNumber / ProjectValues.epochLength);
        this.insertionToken = this.queryNumber - ProjectValues.epochLength;

        if(this.queryNumber % 10 == 0 && this.executionTime == 0)
            System.out.println("dealing with query " + this.queryNumber);

        /** Set the schema of the database that we are using
         * */
        if(this.db_schema != null && !this.db_schema.equals("")) {
            ProjectValues.schema = this.db_schema;
        }
    }

    /***/
    public void execution() {
        // open the files with the query (both select and construct)
        Path selectIn = Paths.get(ProjectPaths.selectQueryFile);

        // get the query to compute
        try(BufferedReader selectReader = Files.newBufferedReader(selectIn)) {
            this.getTheQueriesToPerform(selectReader, this.queryNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(this.selectQuery == null)
            System.exit(1);

        // if we are here, we can try the query on the cache
        ReturnBox box = this.runOneQuery();

        ReturnBox dbBox = null;
        if(!box.foundSomething) {
            // in case of cache miss, ask the whole database
            dbBox = this.runQueryOnWholeDBSavingData();
        }

        // now that we performed the query, print the results
        try {
            this.printResultsForBaselineMethod(box, dbBox);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // At the end of an epoch, and if this is the last time we wun a query, update the cache and check if it is too big
        if(this.queryNumber % ProjectValues.epochLength == 0 && this.queryNumber > 0 && this.executionTime >= ProjectValues.timesOneQueryIsExecuted) {
            // at the end of each epoch, we update the relational database with the last epoch of queries being seen
            long requiredTime = this.updateTheCache();
            long capRequiredTime = this.dealWithTheCapOnBaseline();

            // at the end, print the time required to update the database (time to add tuples and time to impose the cap)
            try {
                this.printTimeRequiredToUpdateRDB(requiredTime + capRequiredTime);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("impossible to write update time");
            }
        }
    }

    private long updateTheCache() {
        // first, identify the query to perform
        this.queryNumber = this.queryNumber - ProjectValues.epochLength;
        long elapsed = 0;

        // update the cache with each query we saw so far
        for(int i = 0; i < ProjectValues.epochLength; i++) {
            // now, for each of the queries we saw so far, take it
            Path selectIn = Paths.get(ProjectPaths.selectQueryFile);

            try(BufferedReader selectReader = Files.newBufferedReader(selectIn)) {
                this.getTheQueriesToPerform(selectReader, this.queryNumber);
            } catch (IOException e) {
                e.printStackTrace();
            } // now we have the query
            // we run the query on the database to get its result set
            ReturnBox dbBox = this.runQueryOnWholeDBSavingData();
            // save the value in the cache relational database for future reference
            elapsed += this.updateRelationalDatabase(dbBox);
            this.queryNumber++; // move to the next query
        }
        return elapsed;

    }

    private long dealWithTheCapOnBaseline() {
        if(!ProjectValues.capRequired) {
            return 0;
        }

        long start = System.currentTimeMillis();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = executor.submit(new DealWithCapOnRDBCacheThread(this));
        ReturnBox rb = new ReturnBox();
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

        return System.currentTimeMillis() - start;

    }

    private void printTimeRequiredToUpdateRDB(long elapsed) throws IOException {
        int size = this.getSizeOfRelationalCacheInTuples();
        String prettySize = this.getSizeOfRelationalCacheInBytes();
        this.updateRDBFw.write(elapsed + "," + size + "," + prettySize + "\n");
    }

    /** Gets the dimension in tuples of the cache as it is now
     * */
    private int getSizeOfRelationalCacheInTuples() {
        int size = 0;
        String sql = String.format(SqlStrings.GET_COUNT_BASELINECACHE, ProjectValues.schema);
        try {
            PreparedStatement pst = this.rdbConnection.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            rs.next();
            size = rs.getInt(1);
            rs.close();
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return size;
    }

    private String getSizeOfRelationalCacheInBytes() {
        String size = "";
        String sql = String.format(SqlStrings.GET_SIZE_BASELINECACHE, ProjectValues.schema);
        try {
            PreparedStatement pst = this.rdbConnection.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            rs.next();
            size = rs.getString(1);
            rs.close();
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     *
     * @param box the ResultBox coming from the query process on the cache
     * @param dbBox the ResultBox coming from the query process on the whole DB
     *
     * */
    private void printResultsForBaselineMethod(ReturnBox box, ReturnBox dbBox) throws IOException {
        if(this.executionTime == 0) {
            this.cacheFw.write("# QUERYNO " + this.queryNumber + "\n");
        }

        if(!box.inTime)
        { // in case the cache required more than the timeout, the whole DB will require even more
            // we just print the timeout
            this.cacheFw.write( "timeout," + box.queryTime + ",-,-,-\n");
            return;
        }

        if(box.foundSomething) { // cache hit
            if(this.executionTime == 0)
                this.cacheFw.write("hit," + box.queryTime + ",-,-," + box.resultSetSize + "\n");
            else
                this.cacheFw.write( "hit," + box.queryTime + ",-,-,-\n");
        } else { // cache miss
            long totalTime =  box.queryTime + dbBox.queryTime; // combine the times

            if(this.executionTime == 0) // we write the size of the result set only the first time
                this.cacheFw.write("miss," + totalTime + "," + box.queryTime +  "," + dbBox.queryTime + "," + dbBox.resultSetSize + "\n");
            else
                this.cacheFw.write("miss," + totalTime + "," + box.queryTime +  "," + dbBox.queryTime + ",-" + "\n");


        }


    }


    private long updateRelationalDatabase(ReturnBox dbBox) {
        long elapsed = 0;
        if(this.executionTime >= ProjectValues.timesOneQueryIsExecuted) { // update the relational database only if it is the last time we are re-executing
            // the same query (otherwise you would have a hit after the first miss in the same multiple-times execution)
            elapsed = this.insertQueryDataToCacheDatabase(dbBox);
        }
        return elapsed;
    }

    /** Inserts the data in the relational database (this method is invoked in case of cache miss)
     *
     * @return the time required to update the relational database
     * */
    private long insertQueryDataToCacheDatabase(ReturnBox dbBox) {
        long start = System.currentTimeMillis();
        // first, check if this query is already present in the database
        String hash = ConvertToHash.convertToHashSHA256(this.selectQuery);
        String findQuery = String.format(SqlStrings.GET_BASELINE_ANSWER, ProjectValues.schema);
        try(PreparedStatement ps = rdbConnection.prepareStatement(findQuery)) {
            ps.setString(1, hash);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                // the query is already present
                long elapsed = System.currentTimeMillis() - start;
                this.updateRDBCacheWithQueryNumber(hash);// I need to update the database for this solution
                return elapsed;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        // if we are here, the query was found for the first time. We update the cache
        String insertQuery = String.format(SqlStrings.ADD_TUPLE_TO_RDB_CACHE, ProjectValues.schema);

        try {
            PreparedStatement ps = this.rdbConnection.prepareStatement(insertQuery);
            for(int i = 0; i < dbBox.results.size(); ++i) {
                ps.setInt(1, this.queryNumber);
                ps.setString(2, hash);
                ps.setString(3, dbBox.results.get(i));
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return System.currentTimeMillis() - start;
    }

    /** Updates the time of execution of a set of answers in a database
     * (Necessary to implement the LRU strategy)
     * */
    private void updateRDBCacheWithQueryNumber(String queryHash) {
        if(this.executionTime != 0) {// do it only the first time we execute this query
            return;
        }
        // query to update this query result to the lastest query number
        String sql = String.format(SqlStrings.UPDATE_RECENTLY_USED_QUERY_NUMBER, ProjectValues.schema);
        try {
            PreparedStatement ps = this.rdbConnection.prepareStatement(sql);
            ps.setInt(1, this.queryNumber);
            ps.setString(2, queryHash);

            ps.executeUpdate();
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private ReturnBox runQueryOnWholeDBSavingData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = null;

        future = executor.submit(new threads.virtuoso.QueryWholeDBAndSaveDataThread(this));
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

        if(result.resultSetSize == 0 && this.executionTime >= ProjectValues.timesOneQueryIsExecuted) {
            // if we are here, it means we need to insert this query among the ones we already found are emtpy
            this.updateEmptyCache(result);
        }

        return result;
    }

    public ReturnBox runOneQuery() {
        // first, check if the query is already present in the "empty" cache
        ReturnBox result = new ReturnBox();

        long start = System.currentTimeMillis();
        if(this.checkIfQueryIsEmpty(this.selectQuery)) {
            long elapsed = System.currentTimeMillis() - start;
            result.queryTime = elapsed;
            result.resultSetSize = 0;
            result.foundSomething = true;
            result.inTime = true;
        } else {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<ReturnBox> future = executor.submit(new QueryRelationalDBCache(this));
            try {
                result = future.get(ProjectValues.timeoutSelectQueries, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                result.foundSomething = false;
                result.inTime = false;
            } finally {
                if(!future.isDone())
                    future.cancel(true);
                if(!executor.isTerminated())
                    executor.shutdownNow();
            }
        }


        return result;
    }

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
        }
    }

    /** It uses the queryNumber, passed as arg[0], to find the query that we want to execute in the list of queries
     * that is the file of queries passed as parameter
     *
     *
     * */
    public void getTheQueriesToPerform(BufferedReader selectReader, int qNumber) throws IOException {
        for(int i = -1; i < qNumber; ++i) {
            selectQuery = selectReader.readLine();
        }

        if(this.selectQuery == null) {
            System.err.println("query not found");
            System.exit(1);
        }
    }

    private void closeDown() {
        try {
            PostgreHandler.closeConnection(this.getClass().getName());
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("UNABLE TO CLOSE CONNECTION TO RDB");
            System.exit(1);
        }

        // close connection to RDF databases
        TripleStoreHandler.closeConnection(TripleStoreHandler.DB);
        // close the writers
        try {
            cacheFw.flush();
            cacheFw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            updateRDBFw.flush();
            updateRDBFw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        RunBaselineWithVirtuoso execution = new RunBaselineWithVirtuoso();
        // read and assign the parameters
        JCommander commander = JCommander.newBuilder().addObject(execution).build();
        commander.parse(args);

        execution.init();

        // run the query
        execution.execution();
        execution.closeDown();
    }


}
