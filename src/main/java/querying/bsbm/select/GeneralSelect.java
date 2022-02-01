package querying.bsbm.select;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import java.io.File;

public class GeneralSelect {

    public static void main(String[] args) {
        String databaseDirectory = "/Users/dennisdosso/Documents/progetti_di_ricerca/creditToRDFRe/indici/BSBM1M";

//        int i = Integer.parseInt(args[0]);
//        String query = "select * WHERE {?s ?p ?o} offset " + i*1000 + " limit 5000";
        String query = "SELECT (count(*) as ?count) WHERE {?s ?p ?o}";

        // open the database
        File dataDir = new File(databaseDirectory);
        Repository db = new SailRepository(new NativeStore(dataDir, ""));
        db.init();
        RepositoryConnection conn = db.getConnection();

        long start = System.currentTimeMillis();

        // prepare the query and execute it
        TupleQuery tupleQuery = conn.prepareTupleQuery(query);
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            // we just iterate over all solutions in the result...
            while (result.hasNext()) {
                BindingSet solution = result.next();
                System.out.println(solution);
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed time : " + elapsed + " (ms)");

        // close the database
        conn.close();
        db.shutDown();

        System.out.println("done");



    }
}
