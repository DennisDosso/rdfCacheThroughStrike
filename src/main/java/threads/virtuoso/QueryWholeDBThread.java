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
        Query tupleQuery = QueryFactory.create(this.process.selectQuery);
        VirtuosoQueryExecution vqu = VirtuosoQueryExecutionFactory.create(tupleQuery, this.process.virtuosoDatabase ); // submit the query to the database
        ResultSet results = vqu.execSelect(); // run the select query
        box.queryTime = System.currentTimeMillis() - start;

        if(results.hasNext()) {
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
            box.foundSomething = false;
        }
        // todo NB: we may need to deal with exceptions, try some ad-hoc example to see which kind of exceptions may rise
        box.inTime = true;
        return box;
    }
}
