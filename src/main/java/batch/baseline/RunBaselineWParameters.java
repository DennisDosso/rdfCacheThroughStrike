package batch.baseline;

import batch.QueryingProcessParam;
import com.beust.jcommander.JCommander;
import properties.ProjectPaths;
import properties.ProjectValues;
import threads.*;
import utils.*;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.*;

/** Use this to run queries on the whole database using an RDB table as cache, namely, baselinecache.
 * This table keeps the results of the queries as tuples (one tuple ofor each result of the
 * query in SPARQL). It also implements a maximum cap limit on the cache, using a LRU algorithm to reduce
 * the size.
 * */
public class RunBaselineWParameters extends QueryingProcessParam {

    public RunBaselineWParameters() {
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

        try(BufferedReader selectReader = Files.newBufferedReader(selectIn)) {
            this.getTheQueriesToPerform(selectReader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(this.selectQuery == null)
            System.exit(1);

        // if we are here, we can try the query on the cache
        ReturnBox box = this.runOneQuery();

        ReturnBox dbBox = null;
        long elapsed = 0;
        if(!box.foundSomething) {
            // in case of cache miss, ask the whole database
            dbBox = this.runQueryOnWholeDBSavingData();
            // save the value in the cache relational database for future reference
            elapsed = this.updateRelationalDatabase(dbBox);
        }

        // now that we performed the query, print the results
        try {
            this.printResultsForBaselineMethod(box, dbBox);
            this.printTimeRequiredToUpdateRDB(elapsed);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // finally, perform the cap update if necessary
        if(this.queryNumber % ProjectValues.epochLength == 0 && this.queryNumber > 0 && this.executionTime >= ProjectValues.timesOneQueryIsExecuted) {
            this.dealWithTheCapOnBaseline();
        }
    }

    private void dealWithTheCapOnBaseline() {
        if(!ProjectValues.capRequired) {
            return;
        }
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

        // write the time required to update due to the cap



    }

    private void printTimeRequiredToUpdateRDB(long elapsed) throws IOException {
        if(this.executionTime == 0)
            this.updateRDBFw.write(elapsed + "\n");
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
        if(this.executionTime == 0) { // update the relational database only if it is the first time we run this query
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
        String insertQuery = String.format(SqlStrings.ADD_TUPLE_TO_RDB_CACHE, ProjectValues.schema);
        String hash = ConvertToHash.convertToHashSHA256(this.selectQuery);
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
        long elapsed = System.currentTimeMillis() - start;
        return elapsed;

    }

    private ReturnBox runQueryOnWholeDBSavingData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = null;

        future = executor.submit(new QueryWholeDBAndSaveDataThread(this));
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

    public ReturnBox runOneQuery() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = executor.submit(new QueryRelationalDBCache(this));
        ReturnBox result = new ReturnBox();
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
    public void getTheQueriesToPerform(BufferedReader selectReader) throws IOException {
        for(int i = -1; i < this.queryNumber; ++i) {
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
        RunBaselineWParameters execution = new RunBaselineWParameters();
        // read and assign the parameters
        JCommander commander = JCommander.newBuilder().addObject(execution).build();
        commander.parse(args);
        execution.init();

        // run the query
        execution.execution();
        execution.closeDown();
    }


}
