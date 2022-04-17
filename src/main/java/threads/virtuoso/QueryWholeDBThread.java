package threads.virtuoso;

import batch.QueryVault;
import batch.QueryingProcess;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.eclipse.rdf4j.query.*;
import utils.ReturnBox;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.util.concurrent.Callable;

/** A thread that performs the execution of a query on the whole DB.
 * This thread is invoked by a QueryingProcess, which needs to have
 * its connection to the triple store.
 *
 * */
public class QueryWholeDBThread implements Callable<ReturnBox>  {

    private QueryVault process;

    public QueryWholeDBThread(QueryVault qp) {
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

        VirtuosoQueryExecution vqu = null;
        try{
            vqu = VirtuosoQueryExecutionFactory.create(tupleQuery, this.process.virtuosoDatabase ); // submit the query to the database
        } catch (Exception e) {
            System.err.println("[DEBUG] error when invoking create");
            e.printStackTrace();
            System.exit(-1);
        }
        ResultSet results = vqu.execSelect(); // run the select query
//        System.out.println("[DEBUG] list of vars: " + results.getResultVars());
        box.queryTime = System.currentTimeMillis() - start;

        if(results.hasNext()) {
//            System.out.println("[DEBUG] Yee, we found something!");
            box.foundSomething = true;

            // if necessary, compute the size of the result set
            if(this.process.executionTime == 0) {
                while(results.hasNext()) {
                    results.next();
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
