package batch.wholedb;

import batch.QueryingProcess;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import properties.ProjectPaths;
import utils.ReturnBox;
import utils.SilenceLog4J;
import utils.TripleStoreHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

/** Class thought to run a SELECT query on one database and print the time, without the use
 * of any cache. The class is also thought to work in a bash environment.
 * */
public class RunOnWholeDB extends QueryingProcess {



    public RunOnWholeDB() {
        super();
        try {
            this.wholeDbFw = new FileWriter(ProjectPaths.wholeDbTimesFile, true);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Runs one query on the whole database in the context of a call from a bash script.
     * */
    public ReturnBox runOneQuery() {
        // first, initialize the field this.selectQuery to the correct query from the file ProjectPaths.selectQueryFile
        this.takeQueryToPerform();
        // now perform the query
        ReturnBox res = this.runQueryOnWholeDB();
        return res;
    }



    /** sets the field selectQuery using the file containing SELECT queries
     * specified in the property selectQueryFile*/
    private void takeQueryToPerform() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(ProjectPaths.selectQueryFile))) {
            for(int i = -1; i < this.queryNumber; ++i) { // let us get to the line where there is the query we are interested in
                this.selectQuery = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // if we are here the field this.selectQuery has the correct select query. We can proceed
    }

    private void close() {
        try {
            this.wholeDbFw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TripleStoreHandler.closeConnection(TripleStoreHandler.DB);
        try {
            this.rdbConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }



    public static void main(String[] args) throws IOException {
        RunOnWholeDB execution = new RunOnWholeDB();

        execution.queryNumber = Integer.parseInt(args[0]);
        execution.executionTime = Integer.parseInt(args[1]);

        ReturnBox res = execution.runOneQuery();

        // now print the results
        if(execution.executionTime == 0) { // first time we run this query
            execution.wholeDbFw.write("# QUERYNO " + execution.queryNumber + ", " + res.resultSetSize + "\n");
        }
        execution.wholeDbFw.write(res.queryTime + "\n");

        execution.close();
    }
}
