package querying.bsbm.select;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class SelectQuery1 {

    public static void main(String[] args) {
        // disable the warnings from log4j
        List<org.apache.log4j.Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for ( org.apache.log4j.Logger logger : loggers ) {
            logger.setLevel(Level.OFF);
        }

        String databaseDirectory = "/Users/dennisdosso/Documents/progetti_di_ricerca/creditToRDFRe/indici/BSBM1M";

        String query = "PREFIX bsbm-inst: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/>\n" +
                "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "\n" +

                "SELECT DISTINCT ?product ?label \n" +

                "WHERE { \n" +

                " ?product rdfs:label ?label .\n" +
                " ?product a <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1> .\n" +  // parameter 1
                " ?product bsbm:productFeature <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductFeature2757> . \n" + // parameter 2
                " ?product bsbm:productFeature <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductFeature2736> . \n" + // parameter 3
                "?product bsbm:productPropertyNumeric1 ?value1 . \n" +

                "	FILTER (?value1 > 50) \n" + // parameter 4 (anche no)
                "	}\n" +

			    "ORDER BY ?label\n" + // the order by operation, in a large DB, breaks the query
                "LIMIT 10";

        // open the database
        File dataDir = new File(databaseDirectory);
        Repository db = new SailRepository(new NativeStore(dataDir, "spoc"));
        db.init();
        RepositoryConnection conn = db.getConnection();

        long start = System.currentTimeMillis();

        // prepare the query and execute it
        TupleQuery tupleQuery = conn.prepareTupleQuery(query);
        long elapsed = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            int c = 0;
            // we just iterate over all solutions in the result...
            while (result.hasNext()) {
                BindingSet solution = result.next();
                if(c==0) {
                    elapsed = System.currentTimeMillis() - start;
                    c++;
                }
                System.out.println(solution.getValue("product"));
            }
        }

        System.out.println("elapsed time : " + elapsed + " (ms)");

        // close the database
        conn.close();
        db.shutDown();

        System.exit(1);
    }
}
