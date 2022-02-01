package setup;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import properties.ProjectPaths;
import utils.SilenceLog4J;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

public class ImportDatabase {

    public static void main(String[] args) {
        ProjectPaths.init();
        //goddammit log4j
        SilenceLog4J.silence();
        // directory where the .ttl files with the data to import are stored (the "fragments")
        String inputDirectory = ProjectPaths.ttlFilesDirectory;
        // where to save the index
        String databaseDirectory = ProjectPaths.databaseIndexDirectory;


        // list all files, recursively (they must be of type turtle, i.e. ttl)
        Collection<File> files = FileUtils.listFiles(new File(inputDirectory), new String[] {"ttl"}, true);

        // open the database
        File dataDir = new File(databaseDirectory);
        Repository db = new SailRepository(new NativeStore(dataDir, ""));
        db.init();
        RepositoryConnection conn = db.getConnection();

        for(File f : files) {
            if(!f.exists())
                continue;
            System.out.println("Now importing file: " + f.getName());

            // read data from file in Turtle format and save them in the triplestore
            try(FileInputStream input = new FileInputStream(f)) {
                // add the RDF data from the input stream directly to our database
                conn.add(input, "", RDFFormat.TURTLE);
                conn.commit();
                System.out.println("Completed successfully the import of file " + f.getName());
            } catch (RDFParseException | FileNotFoundException e) {
                System.out.println("en error occurred with file " + f + ", moving on");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        conn.close();
        db.shutDown();


    }
}
