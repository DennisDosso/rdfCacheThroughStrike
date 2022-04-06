package virtuoso;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.io.FileUtils;
import properties.ProjectPaths;
import properties.ProjectValues;
import utils.PostgreHandler;
import virtuoso.jena.driver.VirtGraph;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/** Class to clean things up before starting the experiment. It
 * cleans the cache directory and the support RDB
 *
 * in-line parameters:
 * <ul>
 *     <li>-MD the main directory where we are operating</li>
 *     <li>-VP the path of the file values.properties (parameters used in the project) </li>
 * </ul>*/
public class CleanThingsUpWithVirtuoso {

    /** Main directory where we are operating.
     * */
    @Parameter(
            names = {"--master_directory", "-MD"},
            arity = 1,
            required = true,
            description = "Main directory where we are operating. It is used to build the other paths used in the execution"
    )
    public String masterDirectory;

    /** path of the file containing the values property file */
    @Parameter(
            names = {"--values_path", "-VP"},
            arity = 1,
            required = true,
            description = "path of the file containing the values property file"
    )
    public String valuesPath;

    public CleanThingsUpWithVirtuoso() {

    }

    public void init() {
        ProjectPaths.masterInit(this.masterDirectory, "0");
        ProjectValues.init(this.valuesPath);
    }

    public void clean() {
        if(!ProjectValues.cleanCache) {
            return;
        }

        // delete the cache database in the disk
        try {
            FileUtils.cleanDirectory(new File(ProjectPaths.cacheDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // truncate the table containing the tables with the data used for the caching, and reset autoincremental IDs
        String sql = "TRUNCATE TABLE %s.triples; TRUNCATE TABLE %s.triplestimeframes; TRUNCATE TABLE %s.lineage_cache; TRUNCATE TABLE %s.baselinecache;";
        sql = String.format(sql, ProjectValues.schema, ProjectValues.schema, ProjectValues.schema, ProjectValues.schema);
        try {
            // query being executed
            System.out.println("query being executed to clean up: \n" + sql);
            PostgreHandler.getConnection(ProjectValues.produceJdbcString(), "Oppenheimer").prepareStatement(sql).execute();
            sql = "ALTER SEQUENCE %s.triples_tripleid_seq RESTART WITH 1;";
            sql = String.format(sql, ProjectValues.schema);
            PostgreHandler.getConnection("Oppenheimer").prepareStatement(sql).executeUpdate();

            // now close the connection and say hello
            PostgreHandler.closeConnection("Oppenheimer");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // clean the cache database
        VirtGraph virtuosoDatabase = new VirtGraph(ProjectValues.virtuosoCacheIRI, ProjectValues.virtuosoConnString, ProjectValues.virtuosoUser, ProjectValues.virtuosoPassword);
        virtuosoDatabase.clear();

        // delete the construct support file (could be left from a previous iteration and have useless queries inside of it)
        (new File(ProjectPaths.supportTextFile)).delete();

        System.out.println("Everything was correctly cleaned");
    }


    public static void main(String[] args) {
        CleanThingsUpWithVirtuoso execution = new CleanThingsUpWithVirtuoso();
        // read and assign the parameters
        JCommander commander = JCommander.newBuilder().addObject(execution).build();
        commander.parse(args);
        execution.init();
        execution.clean();
    }
}
