package threads.virtuoso;

import batch.QueryVault;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import utils.ReturnBox;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.util.concurrent.Callable;

/** This thread represents an attempt to query the cache
 * */
public class QueryDiskCacheThread implements Callable<ReturnBox>  {

    private QueryVault process;

    public QueryDiskCacheThread(QueryVault p) {
        this.process = p;
    }


    public ReturnBox call() {

        ReturnBox box = new ReturnBox();

        box.resultSetSize = 0; // if the query will return an empty set, we sign 0 so we will know

        // prepare to execute the query and take the time
        long start = System.currentTimeMillis();
        int resultSetSize = 0;

        // perform the query on the cache
        Query tupleQuery = QueryFactory.create(this.process.selectQuery);
        VirtuosoQueryExecution vqu = VirtuosoQueryExecutionFactory.create(tupleQuery, this.process.virtuosoCache ); // submit the query to the database
        ResultSet results = vqu.execSelect(); // run the select query
        box.queryTime = System.currentTimeMillis() - start;

        if(results.hasNext()) {
            box.foundSomething = true;
            if(this.process.executionTime == 0) {
                while(results.hasNext()) {
                    results.next();
                    resultSetSize++;
                }
                box.resultSetSize = resultSetSize;
            }
        } else {
            box.foundSomething = false;
        }
        box.inTime = true;
        return box;
    }
}
