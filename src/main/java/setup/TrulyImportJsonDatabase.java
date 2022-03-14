package setup;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import utils.SilenceLog4J;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * https://rdf4j.org/documentation/programming/rio/
 * */
public class TrulyImportJsonDatabase {

    public static void main(String[] args) throws IOException {
        //goddammit log4j
        SilenceLog4J.silence();

        String inputFile = "/Users/dennisdosso/Documents/databases/wikidata/ritest.json";
        FileInputStream input = new FileInputStream(new File(inputFile));

        // open database
        String databaseDirectory = "/Users/dennisdosso/Documents/databases/wikidata/db";
        File dataDir = new File(databaseDirectory);
        Repository db = new SailRepository(new NativeStore(dataDir, "spoc"));
        db.init();
        RepositoryConnection conn = db.getConnection();
        List<Statement> l = new ArrayList<>();

        try (GraphQueryResult res = QueryResults.parseGraphBackground(input, "", RDFFormat.JSONLD)) {
            while(res.hasNext()) {
                Statement st = res.next();
                l.add(st);
                if(l.size() > 0) {
                    conn.add(l);
                    conn.commit();
                    l.clear();
                }
            }

        }catch (RDF4JException e) {
            // handle unrecoverable error
        }
        finally {
            input.close();
        }
    }
}
