package threads;

import batch.QueryingProcess;
import properties.ProjectValues;
import utils.ReturnBox;
import utils.SqlStrings;
import utils.TripleStoreHandler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/** A class which is a thread that does two things when called:
 * <p>
 *     1) It increments of 1 the strike count of each triple in the lineage passed as parameter, and
 *     2) it inserts in the cache the triples that go beyond the threshold.
 * </p>
 * <p>
 *     An alternative would have been to first update ALL the strikes count, and then
 *     to update the cache. How we do things here is slightly better since we do not need to delete the cache every time
 *     and re-create it from scratch, nor we need to perform every time insertion operations in a cache that already
 *     contains those triples. By inserting the triples only when it is necessary, we make sure to reduce the
 *     insertion operations (or, at least, I hope I am reducing the useless insertion operations).
 * </p>
 *
 *
 * */
public class UpdateRDBDatabaseWithTriplesFromLineageThread implements Callable<ReturnBox> {

    /** the process that called this thread*/
    private QueryingProcess process;
    /** A list of strings (arrays made of three strings each: subject, predicate, object) that
     * represent the lineage of a SPARQL query
     *
     * */
    private List<String[]> lineage;

    public UpdateRDBDatabaseWithTriplesFromLineageThread(QueryingProcess p, List<String[]> l) {
        this.process = p;
        this.lineage = l;
    }

    @Override
    public ReturnBox call()  {
        TripleStoreHandler.initModelBuilder();
        ReturnBox rb = new ReturnBox();
        // statements to insert/update triples in the relational database

        // first, upload triples in the database
        try{

            String insert_q = String.format(SqlStrings.INSERT_TRIPLE, ProjectValues.schema);
            PreparedStatement insert_stmt = this.process.rdbConnection.prepareStatement(insert_q);

            String q = String.format(SqlStrings.updateHits, ProjectValues.schema);
            PreparedStatement update_stmt = this.process.rdbConnection.prepareStatement(q);

            for(String[] triple: lineage) {
                ReturnBox b = this.process.checkTriplePresence(triple);
                if(b.present) {
                    // already present in the DB, update it
                    this.process.dealWithAlreadyPresentTriple(triple[0], triple[1], triple[2], update_stmt);
                } else {
                    // we need to insert it into the RDB
                    this.process.dealWithNewTriple(triple[0], triple[1], triple[2], insert_stmt);
                }

                // if the triple impact is above the threshold (and in the previous strike it was below the threshold)
                // we add it to the cache
                int currentStrikes = b.strikes + 1;
                if(Math.log(currentStrikes + 1) >= ProjectValues.creditThreshold) {
                    if(currentStrikes - 1 == 0) // need to deal with a special degenerate case to avoid log exception
                        TripleStoreHandler.addTripleToCreation(triple[0], triple[1], triple[2]);
                    else {
                        if(Math.log(currentStrikes) < ProjectValues.creditThreshold)
                            TripleStoreHandler.addTripleToCreation(triple[0], triple[1], triple[2]);
                    }
                }
            }
            update_stmt.executeBatch();

            insert_stmt.close();
            update_stmt.close();

        } catch (SQLException e) {
            System.out.println("Exception when updating the RDB");
        }

        // finalize process of addition to the cache
        TripleStoreHandler.addTriplesFromCreationBuilderToThisConnection(TripleStoreHandler.CACHE);

        // completed one update based on one lineage, we move to the "next" lineage
        this.process.insertionToken++;
        rb.foundSomething = true;
        return rb;
    }
}
