package threads.virtuoso;

import batch.QueryVault;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.eclipse.rdf4j.query.*;
import properties.ProjectValues;
import utils.ConvertToHash;
import utils.ReturnBox;
import utils.SqlStrings;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.sql.PreparedStatement;
import java.util.concurrent.Callable;

public class QueryWholeDBAndSaveDataThread implements Callable<ReturnBox> {
    private QueryVault process;

    public QueryWholeDBAndSaveDataThread(QueryVault qp) {
        this.process = qp;
    }

    @Override
    public ReturnBox call() throws Exception {
        ReturnBox box = new ReturnBox();

        // prepare to execute the query and take the time
        long start = System.currentTimeMillis();
        int resultSetSize = 0;

        // prepare the query
        Query tupleQuery = null;
        try {
            tupleQuery = QueryFactory.create(this.process.selectQuery);
        } catch (Exception e1) {
            System.err.println("[DEBUG] error when invoking the QueryFactory create with query " + this.process.selectQuery);
            e1.printStackTrace();
            System.exit(-1);
        }

        if(tupleQuery == null)
            System.exit(-1);

        // if we are here, we can execute the query
        VirtuosoQueryExecution vqu = null;

        try{
            vqu = VirtuosoQueryExecutionFactory.create(tupleQuery, this.process.virtuosoDatabase ); // submit the query to the database
        } catch (Exception e) {
            System.err.println("[DEBUG] unable to 'create' the query");
            e.printStackTrace();
            System.exit(-1);
        }
        ResultSet results = vqu.execSelect(); // run the select query
        box.queryTime = System.currentTimeMillis() - start;

        if(results.hasNext()) {
            box.foundSomething = true;
            // when necessary (first time, to print the size of the result set, and last time, to see if it is empty)
            if(this.process.executionTime == 0 || this.process.executionTime >= ProjectValues.timesOneQueryIsExecuted) {
                while(results.hasNext()) {
                    QuerySolution slt = results.nextSolution();
                    String solution = this.process.getValuesFromJenaQuerySolution(slt);
                    box.results.add(solution);
                    resultSetSize++;
                }
                box.resultSetSize = resultSetSize;
            }
        } else { // empty answer
//            System.out.println("[DEBUG] We didn't find anything");
            box.foundSomething = false;
        }

        box.inTime = true;
        return box;
    }

}
