package threads;

import batch.QueryingProcess;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import utils.ReturnBox;
import utils.TripleStoreHandler;

import java.util.concurrent.Callable;

/** This thread represents an attempt to query the cache
 * */
public class QueryDiskCacheThread implements Callable<ReturnBox>  {

    private QueryingProcess process;

    public QueryDiskCacheThread(QueryingProcess p) {
        this.process = p;
    }


    public ReturnBox call() {
        ReturnBox box = new ReturnBox();
        box.resultSetSize = 0; // if the query will return an empty set, we sign 0 so we will know
        // prepare to execute the query and take the time
        long start = System.currentTimeMillis();
        int resultSetSize = 0;
        // use the cache, provided by the process that called this thread, to perform the query
        TupleQuery tupleQuery = this.process.cacheRepositoryConnection.prepareTupleQuery(this.process.selectQuery);
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
                // in case we have emptyset, the time is probably lower, thus we also take the time in this case
                box.foundSomething = false;
                box.queryTime = System.currentTimeMillis() - start;
            }
        }

        return box;
    }
}
