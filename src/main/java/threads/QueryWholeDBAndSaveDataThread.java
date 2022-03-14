package threads;

import batch.QueryVault;
import org.eclipse.rdf4j.query.*;
import properties.ProjectValues;
import utils.ConvertToHash;
import utils.ReturnBox;
import utils.SqlStrings;

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


        try{
            TupleQuery tupleQuery  = this.process.repositoryConnection.prepareTupleQuery(this.process.selectQuery);


            // run the query
            try(TupleQueryResult result = tupleQuery.evaluate()) {
                // check if the result set is different from the emptyset
                if(result.hasNext()) {
                    // another operation that may take time is the first next()
                    BindingSet solution = result.next(); // this operation may also require time, this is why we take the time in this case
                    box.queryTime = System.currentTimeMillis() - start;
                    box.foundSomething = true;
                    resultSetSize++;

                    String values = this.process.getValuesFromResult(solution);
                    box.results.add(values);

                    // complete the operation and count how many triples we have. We only do this when this is the last time
                    // we execute the query (we usually execute a query 10 times to take the average)
                    if(this.process.executionTime == 0 || this.process.executionTime >= ProjectValues.timesOneQueryIsExecuted) {
                        while(result.hasNext()) {
                            resultSetSize++;

                            solution = result.next();
                            values = this.process.getValuesFromResult(solution);
                            box.results.add(values);
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
            } catch (QueryEvaluationException e) {
                if(this.process.executionTime == 0){
                    e.printStackTrace();
                }
                box.foundSomething = false;
                box.malformed = true;
                box.queryTime = System.currentTimeMillis() - start;
            }
        } catch(MalformedQueryException e2) {
            if(this.process.executionTime == 0){
                System.out.println("[DEBUG] found a defective query, queryNo: " + this.process.queryNumber);
                e2.printStackTrace();
            }
            box.foundSomething = false;
            box.queryTime = System.currentTimeMillis() - start;
        }
        return box;
    }

}
