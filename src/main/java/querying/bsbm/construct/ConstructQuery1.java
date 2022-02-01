package querying.bsbm.construct;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import java.io.File;

public class ConstructQuery1 {

    public static void main(String[] args) {
        String databaseDirectory = "/Users/dennisdosso/Documents/progetti_di_ricerca/creditToRDFRe/indici/BSBM1M";

        String construct_query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX bsbm-inst: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/>\n" +
                "PREFIX bsbm: <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/>\n" +
                "CONSTRUCT {?product rdfs:label ?label ;\n" +
                "     	a <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1> ;\n" +
                "    	bsbm:productFeature <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductFeature41> ;\n" +
                "        bsbm:productFeature <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductFeature42>  ;\n" +
                "        bsbm:productPropertyNumeric1 ?value1 .}\n" +
                "WHERE { \n" +
                "    ?product rdfs:label ?label ;\n" +
                "     	a <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductType1> ;\n" +
                "    	bsbm:productFeature <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductFeature41> ;\n" +
                "        bsbm:productFeature <http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/ProductFeature42> ;\n" +
                "        bsbm:productPropertyNumeric1 ?value1 .\n" +
                "    FILTER (?value1 > 50) .\n" +
                "	}\n" +
			    "ORDER BY ?label\n" +
                "LIMIT 100";


        // open the database
        File dataDir = new File(databaseDirectory);
        Repository db = new SailRepository(new NativeStore(dataDir, ""));
        db.init();
        RepositoryConnection conn = db.getConnection();

        long start = System.currentTimeMillis();

        //execute the query
        GraphQuery graphQuery = conn.prepareGraphQuery(construct_query);
        try (GraphQueryResult result = graphQuery.evaluate()) {
            // we just iterate over all solutions in the result...
            for (Statement st: result) {
                // get the three elements
                String subject = st.getSubject().stringValue();
                String predicate = st.getPredicate().stringValue();
                String object = st.getObject().stringValue();

                System.out.println(subject + " " + predicate + " " + object);
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Required " + elapsed + " ms");

    }
}
