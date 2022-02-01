package threads;

import batch.QueryingProcess;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import utils.ReturnBox;

import java.util.concurrent.Callable;

/** A thread that performs the execution of a query on the whole DB.
 * This thread is invoked by a QueryingProcess, which needs to have
 * its connection to the triple store.
 *
 * */
public class QueryWholeDBThread implements Callable<ReturnBox>  {

    private QueryingProcess process;

    public QueryWholeDBThread(QueryingProcess qp) {
        this.process = qp;
    }


    @Override
    public ReturnBox call() throws Exception {
        ReturnBox box = new ReturnBox();

        // prepare to execute the query and take the time
        long start = System.currentTimeMillis();
        int resultSetSize = 0;

        TupleQuery tupleQuery  = this.process.repositoryConnection.prepareTupleQuery(this.process.selectQuery);
        // run the query
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            // check if the result set is different from the emptyset
            if(result.hasNext()) {
                result.next(); // this operation may also require time, this is why we take the time in this case
                box.queryTime = System.currentTimeMillis() - start;
                box.foundSomething = true;
                // another operation that may take time is the first next()
                resultSetSize++;
                // complete the operation and count how many triples we have. We only do this when this is the first time
                // we execute the query (we usually execute a query 10 times to take the average)
                if(this.process.executionTime == 0) {
                    while(result.hasNext()) {
                        result.next();
                        resultSetSize++;
                    }
                    box.resultSetSize = resultSetSize;
                }
            } else {
                // in case we have an empty set, the time is probably lower. We register the time
                // nonetheless, but set the foundSomething
                // field to false
                box.foundSomething = false;
                box.queryTime = System.currentTimeMillis() - start;
            }
        }
        return box;
    }
}
