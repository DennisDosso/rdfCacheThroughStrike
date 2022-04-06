package virtuoso;

import com.beust.jcommander.JCommander;
import properties.ProjectPaths;
import properties.ProjectValues;
import threads.virtuoso.QueryDiskCacheThread;
import threads.virtuoso.UpdateRDBDatabaseWithTriplesFromLineageThread;
import utils.PostgreHandler;
import utils.ReturnBox;
import virtuoso.jena.driver.VirtGraph;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** A process that runs one query using the cache.
 * Built to work with inline parameters.
 * The required ones are:
 * <ul>
 * <li>-QN the query number</li>
 * <li>-ET execution time, the number of times we have already executed this query </li>
 * <li>-MD master directory, the directory where all the data about the current database is stored</li>
 * <li>-VP values.properties path of the property file containing the parameters being used</li>
 * <li>-QC query class, a string defining the query class being dealt with. Used to take the correct
 * files from the master directory and print the correct files in the master directory</li>
 * </ul>*/
public class RunVirtuosoCache extends VirtuosoQueryingProcess {

    public RunVirtuosoCache() {
        super();
    }

    public void init() {
        super.init();
        // open connection to cache
        virtuosoCache = new VirtGraph(ProjectValues.virtuosoCacheIRI, ProjectValues.virtuosoConnString, ProjectValues.virtuosoUser, ProjectValues.virtuosoPassword);

        // open file writers
        try {
            cacheFw = new FileWriter(ProjectPaths.cacheTimesFile, true);
            constructFw = new FileWriter(ProjectPaths.constructTimesFile, true);
            updateRDBFw = new FileWriter(ProjectPaths.updateRDBTimesFile, true);
            coolDownWriter = new FileWriter(ProjectPaths.coolDownTimesFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // degenerate case in which this is the very first query
        if(this.queryNumber == 0) {
            this.timeframe = 1;
            this.epoch = 1;
            return;
        }

        if(ProjectValues.timeframesRequired)
            this.timeframe = (int) Math.ceil((float) this.queryNumber / ProjectValues.timeframeLenght );
        else
            this.timeframe = 1; // we always stay on the same timeframe

        this.epoch = (int) Math.ceil((float) this.queryNumber / ProjectValues.epochLength);
        // the insertion token represents the "moment" in which we are inserting some triples in the RDB
        // it is associated to the query ID of each query
        // since we are doing this via bash, we need to do some simple calculations to know,
        // at each new execution, what is exactly the id of the query that we are dealing with
        this.insertionToken = this.queryNumber - ProjectValues.epochLength;

        if(this.queryNumber % 10 == 0 && this.executionTime == 0)
            System.out.println("dealing with query " + this.queryNumber);

        /** Set the schema of the database that we are using
         * */
        if(this.db_schema != null && !this.db_schema.equals("")) {
            ProjectValues.schema = this.db_schema;
        }
    }

    /** It closes connections when we are done */
    public void closeDown() {
        try {
            PostgreHandler.closeConnection(this.getClass().getName());
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("UNABLE TO CLOSE CONNECTION TO RDB");
            System.exit(1);
        }
        // close connection to RDF databases
        virtuosoDatabase.close();
        virtuosoCache.close();


        // close the writers
        try {
            cacheFw.flush();
            cacheFw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            constructFw.flush();
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

        try {
            coolDownWriter.flush();
            coolDownWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** It uses the queryNumber, passed as arg[0], to find the query that we want to execute in the list of queries
     * that is the file of queries passed as parameter
     *
     *
     * */
    public void getTheQueriesToPerform(BufferedReader selectReader, BufferedReader constructReader) throws IOException {
        for(int i = -1; i < this.queryNumber; ++i) {
            selectQuery = selectReader.readLine();
            constructQuery = constructReader.readLine();
        }

        if(this.selectQuery == null) {
            System.err.println("query not found");
            System.exit(1);
        }

        if(this.constructQuery == null) {
            System.err.println("query not found");
            System.exit(1);
        }
    }

    /** Part of the procedure after the moment where we found the queries in our file. Now we are ready to actually perform
     * the query. We use a separate thread in order to deal with the time limit
     *<p>
     *     The method also prints the results on the result file.
     *</p>
     * */
    private void runOneQuery() throws IOException {
        // prepare the thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = executor.submit(new QueryDiskCacheThread(this));
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


        // print the results on the file used as file with the results of the cache
        this.printResultsFromCacheExperiments(result);

    }

    /** Given one lineage, i.e. one set of triples, increments the strike count on the RDB
     * by 1 of each one of these triples by using a thread. It also updates the cache.
     *
     * */
    private ReturnBox updateRDBPhaseWithOneLineage(List<String[]> lineage) {
        // todo scrivi il tempo per fare l'update del database relazionale
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ReturnBox> future = null;
        ReturnBox rb = new ReturnBox();

        future = executor.submit(new UpdateRDBDatabaseWithTriplesFromLineageThread(this, lineage));

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

    protected ReturnBox updateRDBAndCachePhase(List<List<String[]>> lineageBuffer) {
        // for each lineage (we have many lineages because we are dealing with all the queries of the epoch at one time)
        // update the RDB
        ReturnBox rb = new ReturnBox();
        long start = System.currentTimeMillis();

        for(List<String[]> lineage : lineageBuffer) {
            rb = this.updateRDBPhaseWithOneLineage(lineage);
        }
        long elapsed = System.currentTimeMillis() - start;
        rb.queryTime = elapsed;
//        System.out.println("[DEBUG] elapsed time to update the database: " + elapsed);

        return rb;
    }



    /** This method computes the lineage and updates the support relational database */
    private void updatePhase () {

        List<List<String[]>> lineageBuffer = new ArrayList<>();

        // COMPUTE THE LINEAGES
        this.computeLineages(lineageBuffer); // todo controlla che funzi

        // UPDATE THE RDB
        ReturnBox rb = this.updateRDBAndCachePhase(lineageBuffer);

        // DEAL WITH THE CAP
        this.dealWithTheCap(rb);

        this.printUpdateAboutTheCache(rb);

        // deal with the timeframe update
        this.dealWithTimeframes();
    }

    private void printUpdateAboutTheCache(ReturnBox rb) {
        // if required, count how big the cache is now
        int cacheSize = 0;
        if(ProjectValues.printCacheSize) {
            cacheSize = this.checkCacheSize();
        }

        // print the time required to update the RDB and its dimension
        try {
            updateRDBFw.write(this.epoch + "," + rb.queryTime + "," + cacheSize + "\n");
            updateRDBFw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** Starting method where we begin our journey to execute one query.
     * It opens the two files containing the queries. It then invokes the step method,
     * which represents one "step", one operation.
     *
     *
     * */
    public void execution() {
        // open the files with the query (both select and construct)
        Path selectIn = Paths.get(ProjectPaths.selectQueryFile);
        Path constructIn = Paths.get(ProjectPaths.constructQueryFile);

        try(BufferedReader selectReader = Files.newBufferedReader(selectIn);
            BufferedReader constructReader = Files.newBufferedReader(constructIn)) {
            // get the query that we are executing here
            this.getTheQueriesToPerform(selectReader, constructReader);

        } catch (IOException e) {
            e.printStackTrace();
        }
        // check that we actually found our queries -- we may have set a wrong number of queries in the property file
        if(this.selectQuery == null || this.constructQuery == null)
            System.exit(1);

        // if we are here, we have our queries and we execute them on the cache
        try {
            this.runOneQuery();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(this.executionTime == 0) {
            // if this is the first time in the 10 repetitions of the same query
            // we save the construct query in a separate txt file. We use
            // this file as memory to perform the update operation
            this.saveConstructQuery();
        }

        // UPDATE PHASE
        // the boolean condition asks that: 1) an epoch has passed.
        //2) this is NOT the very first query (degenerate case of an epoch being passed)
        // 3) this is the last time we are executing the query in the series of 10 times
        // we execute the query to take the average time
        if(this.queryNumber % ProjectValues.epochLength == 0 && this.queryNumber > 0 && this.executionTime >= ProjectValues.timesOneQueryIsExecuted) {
            this.updatePhase();
        }
    }

    public static void main(String[] args) {
        RunVirtuosoCache execution = new RunVirtuosoCache();
        // read and assign the parameters
        JCommander commander = JCommander.newBuilder().addObject(execution).build();
        commander.parse(args);
        execution.init();

        // run the query
        execution.execution();
        execution.closeDown();
    }


}
