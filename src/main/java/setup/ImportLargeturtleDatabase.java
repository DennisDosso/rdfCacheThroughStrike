package setup;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import properties.ProjectPaths;
import utils.SilenceLog4J;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Use this class to import a big database, e.g. Wikidata, in Turtle format.
 *
 * Properties:
 * bigDatabaseFilePath
 * databaseIndexDirectory
 *
 * java -cp rdfCreditRe-1.0-SNAPSHOT.jar:./lib/* setup/ImportLargeturtleDatabase
 *
 * */
public class ImportLargeturtleDatabase {

    public static void main(String[] args) throws IOException {

        ProjectPaths.init();
        //goddammit log4j
        SilenceLog4J.silence();

        // take the big file
        String inputFile =ProjectPaths.bigDatabaseFilePath;
        FileInputStream input = new FileInputStream(new File(inputFile));

        // where to save the index
        String databaseDirectory = ProjectPaths.databaseIndexDirectory;
        File dataDir = new File(databaseDirectory);
        Repository db = new SailRepository(new NativeStore(dataDir, "spoc"));
        db.init();
        RepositoryConnection conn = db.getConnection();

        List<Statement> l = new ArrayList<>();
        int count = 0;

        // here we create a special rdf4j RDF parser that avoids to verify the URIs (Wikidata has
        // too many strange URIs).
        RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
        rdfParser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false); // to disable the check on the syntax of a URI
        rdfParser.getParserConfig().set(BasicParserSettings.VERIFY_RELATIVE_URIS, false); // to disable the check on the structure of a URI

        // in case someone had the bright idea of putting relative URIs in the turtle without providing a namespace,
        // we provide ourselves the base URI - I chose a base URI that is really limited but that should "always" work
        String baseURI = "http:/";

        // read the big file containing the database and import the triples
//        try (GraphQueryResult res = QueryResults.parseGraphBackground(input, "", RDFFormat.TURTLE)) {// version in which we use a default parser. Rises exceptions
        try (GraphQueryResult res = QueryResults.parseGraphBackground(input, baseURI, rdfParser)) { // our custom parser of above. Exceptions are disabled

            while(res.hasNext()) {
                Statement st = res.next();
                l.add(st);
                if(l.size() > 10000) {
                    conn.add(l);
                    conn.commit();
                    l.clear();
                    System.out.println("imported " + count * 10000 + " triples");
                    count++;
                }
            }

            if(l.size() > 0) {
                conn.add(l);
                conn.commit();
                l.clear();
                System.out.println("completed the import");
            }
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
            System.err.println("Mannaggia");
        } catch (RDFParseException rdfe) {
            rdfe.printStackTrace();
            System.out.println("Mannaggia RDF parse exception");
        } finally {
            input.close();
        }

        conn.close();
        db.shutDown();

        System.out.println("done");
    }
}
