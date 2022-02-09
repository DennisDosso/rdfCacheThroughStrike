package batch.cache.cleaning;

import org.apache.commons.io.FileUtils;
import properties.ProjectPaths;
import properties.ProjectValues;
import utils.PostgreHandler;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/** When we start a new set of experiments involving the cache, we need to first delete the cache left at the end of the
 * previous experiment. Also, we need to delete the data from the relational database.
 * <p>
 * This method does just that, and it should be invoked in the bash file before everything else.
 * </p>
 *<p>
 *     In-line parameters: 1) the path of the paths.properties file, 2) the path of the values.properties file.
 *     If nothing is passed, the default paths properties/paths.properties and properties/values.properties
 *     are used.
 *</p>
 * <p>
 *     Used properties:
 *     <li>cleanCache</li> (tell me if you want to delete or not)
 *     <li>cacheDirectory</li> (deletes the cache, the content in the directory)
 *     <li>RDB values</li> (deletes the relational database)
 * </p>
 * */
public class CleanThingsUp {

    public static void main(String[] args) {
        if(args.length >= 2) {
            ProjectPaths.init(args[0]);
            ProjectValues.init(args[1]);
        } else {
            ProjectPaths.init();
            ProjectValues.init();
        }

        if(ProjectValues.cleanCache) {
            return;
        }

        // delete the cache database in the disk
        try {
            FileUtils.cleanDirectory(new File(ProjectPaths.cacheDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // truncate the table containing the tables with the data used for the caching, and reset autoincremental IDs
        String sql = "TRUNCATE TABLE %s.triplestore; TRUNCATE TABLE %s.triples; TRUNCATE TABLE %s.triplestimeframes; TRUNCATE TABLE %s.lineage_cache";
        sql = String.format(sql, ProjectValues.schema, ProjectValues.schema, ProjectValues.schema, ProjectValues.schema);
        try {
            PostgreHandler.getConnection(ProjectValues.produceJdbcString(), "Oppenheimer").prepareStatement(sql).execute();
            sql = "ALTER SEQUENCE %s.triples_tripleid_seq RESTART WITH 1;";
            sql = String.format(sql, ProjectValues.schema);
            PostgreHandler.getConnection("Oppenheimer").prepareStatement(sql).executeUpdate();

            // now close the connection and say hello
            PostgreHandler.closeConnection("Oppenheimer");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // The lock directory may still be in the database directory due to some aborted operation early,
        // and it will block future read/write operations. We delete it
        String lockPath = ProjectPaths.databaseIndexDirectory + "/lock";
        try {
            FileUtils.cleanDirectory(new File(lockPath));
            FileUtils.deleteDirectory(new File(lockPath));
        } catch (IOException e) {
            // do nothing, simply it is not necessary
        } catch (IllegalArgumentException e1) {
            // do nothing
        }

        System.out.println("cleaned things up");


    }
}
