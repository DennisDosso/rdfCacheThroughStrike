package querying.size;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import properties.ProjectPaths;

import java.io.File;

public class CountTriplesInDatabase {

    public static void main(String[] args) {
        ProjectPaths.init();
        String databaseDirectory = ProjectPaths.databaseIndexDirectory;
        String query = "SELECT (count(*) as ?count) WHERE {?s ?p ?o. }";

        // open the database
        File dataDir = new File(databaseDirectory);
        Repository db = new SailRepository(new NativeStore(dataDir, "spoc"));
        db.init();
        RepositoryConnection conn = db.getConnection();

        // prepare the query and execute it
        TupleQuery tupleQuery = conn.prepareTupleQuery(query);
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            // we just iterate over all solutions in the result...
            while (result.hasNext()) {
                BindingSet solution = result.next();
                System.out.println(solution);
            }
        }

        // close the database
        conn.close();
        db.shutDown();

        System.out.println("done");
    }
}
