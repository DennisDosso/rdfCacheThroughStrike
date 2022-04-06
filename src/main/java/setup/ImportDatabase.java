package setup;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
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
import properties.ProjectValues;
import utils.SilenceLog4J;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/** Imports a database. The args[0] should be the path of the properties file
 * to read for the path*/
public class ImportDatabase {

    public static void main(String[] args) {
        if(args.length > 0) {
            ProjectPaths.init(args[0]);
            ProjectValues.init(args[1]);
        } else {
            ProjectPaths.init();
            ProjectValues.init();
        }

        //goddammit log4j
        SilenceLog4J.silence();
        // directory where the .ttl files with the data to import are stored (the "fragments")
        String inputDirectory = ProjectPaths.ttlFilesDirectory;
        // where to save the index
        String databaseDirectory = ProjectPaths.databaseIndexDirectory;


        // list all files, recursively (they must be of type turtle, i.e. ttl)
        Collection<File> files = FileUtils.listFiles(new File(inputDirectory), new String[] {"ttl"}, true);

        //order the files in alphabetic order
        List<File> fileList = new ArrayList<>();
        for(File f : files) {
            fileList.add(f);
        }
        Collections.sort(fileList);



        // open the database
        File dataDir = new File(databaseDirectory);
        Repository db = new SailRepository(new NativeStore(dataDir, ProjectValues.indexes));
        db.init();
        RepositoryConnection conn = db.getConnection();

        for(File f : fileList) {

            if(!f.exists())
                continue;
            System.out.println("Now importing file: " + f.getName());

            // read data from file in Turtle format and save them in the triplestore
            try(FileInputStream input = new FileInputStream(f)) {
                // add the RDF data from the input stream directly to our database
                String baseURI = "http:/";
                RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
                rdfParser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false); // to disable the check on the syntax of a URI
                rdfParser.getParserConfig().set(BasicParserSettings.VERIFY_RELATIVE_URIS, false); // to disable the check on the structure of a URI
//                rdfParser.getParserConfig().set(BasicParserSettings., false);

                GraphQueryResult res = QueryResults.parseGraphBackground(input, baseURI, rdfParser);
                List<Statement> l = new ArrayList<>();
                while(res.hasNext()) {
                    Statement st = res.next();
                    l.add(st);
                }
                try {
                    conn.add(l);
                    conn.commit();
                } catch (IllegalArgumentException ile) {
                    ile.printStackTrace();
                    System.out.println("[DEBUG] error with file " + f.getName());
                    res.close();
                    continue;
                }

                // get the current moment in time
                Calendar rightNow = Calendar.getInstance();
//                conn.add(input, baseURI, RDFFormat.TURTLE);
                System.out.println("Completed successfully the import of file " + f.getName() + " at " + rightNow.get(Calendar.DAY_OF_MONTH) + "/" + rightNow.get(Calendar.MONTH) + " - " + rightNow.get(Calendar.HOUR_OF_DAY) + ":" + rightNow.get(Calendar.MINUTE));

                res.close();
            } catch (RDFParseException | FileNotFoundException e) {
                System.out.println("An error occurred with file " + f + ", moving on");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        conn.close();
        db.shutDown();

        System.out.println("done");
    }
}
