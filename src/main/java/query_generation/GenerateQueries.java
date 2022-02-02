package query_generation;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import properties.ProjectPaths;
import properties.ProjectValues;
import query_generation.templates.*;
import utils.SilenceLog4J;
import utils.TripleStoreHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Given a csv file containing the required values that make up a query,
 * it uses it to write these same queries, one line at a time, in another file
 * <p> Writes two files containing SPARQL SELECT and CONSTRUCT queries.
 *      It uses the property ProjectValues.queriesToCreate to know how many queries to create,
 *      the property outputSelectQueryFile to know where to write the select queries,
 *      the property outputConstructQueryFile to know where to write the construct queries,
 *      the property whichQueryTypeToCreate to know which query type we want to create (e.g., ONE
 *      for query of type 1 in the BSBM case)
 *      and queryBuildingValuesFile to get the values necessary to build the queries.
 *      </p>
 * */
public class GenerateQueries {



    String databaseIndexDirectory;
    /** File where the values (obtained through another SELECT query from GraphDB, for example) used to
     * build the queries */
    String buildingQueryValuesPath;

    String outputSelectQueryFile;
    String outputConstructQueryFile;

    int queriesToCreate;
    int alpha;

    public GenerateQueries() {
        // goddammit log4j!
        SilenceLog4J.silence();

        ProjectValues.init();
        ProjectPaths.init();

        databaseIndexDirectory = ProjectPaths.databaseIndexDirectory;
        buildingQueryValuesPath = ProjectPaths.queryBuildingValuesFile;
        outputSelectQueryFile = ProjectPaths.selectQueryFile;
        outputConstructQueryFile = ProjectPaths.constructQueryFile;


        queriesToCreate = ProjectValues.queriesToCreate;
        alpha = ProjectValues.alpha;
    }

    /** Writes two files containing SPARQL SELECT and CONSTRUCT queries.
     * It uses the property ProjectValues.queriesToCreate to know how many queries to create
     *
     * */
    public void generateBSBMQuery() {
        // open/get the connection and the query
        List<String[]> queryValues = new ArrayList<>();

        // open the file with the query values
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(buildingQueryValuesPath))) {
            // read the values and put them in an array
            String line = "";
            reader.readLine();// the first line usually has headers

            while((line = reader.readLine()) != null) { // read all the values and save them in a list
                String[] values = line.split(",");
                queryValues.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // if we are here, we have all the query values. Open the reader to write the queries
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(outputSelectQueryFile));
             BufferedWriter wc = Files.newBufferedWriter(Paths.get(outputConstructQueryFile))) {

            // we randomly choose the queries with a normal distribution, thus to simulate the random
            // coming of queries from different users

            // prepare the distribution - the lower the standard deviation, the more frequent certain queries will be
            double stdv = Math.max(1, (double)  queryValues.size() /alpha);
            NormalDistribution distribution = new NormalDistribution((float) queryValues.size()/2, stdv);

            for(int i = 0; i < queriesToCreate; ++i) {
                // we create as many queries as indicated in queriesToCreate

                // get a number that is the index of the query that we want. We make sure that we do not go out of index exception
                // we sample until we get a valid index
                int randomNum =  -1;
                while(randomNum < 0 || randomNum > queryValues.size() - 1)
                    randomNum = (int) Math.floor(distribution.sample());

                // now get the values
                String[] parameters = queryValues.get(randomNum);

                // IMPORTANT
                // now decide which type of query we want to print
                if(ProjectValues.whichQueryTypeToCreate.equals("ONE")) {
                    this.generateBSBMQuery1(parameters, w, wc);
                } else if(ProjectValues.whichQueryTypeToCreate.equals("TWO")) {
                    this.generateBSBMQuery2(parameters, w, wc);
                } else if (ProjectValues.whichQueryTypeToCreate.equals("THREE")) {
                    this.generateBSBMQuery3(parameters, w, wc);
                } else if (ProjectValues.whichQueryTypeToCreate.equals("FIVE")) {
                    this.generateBSBMQuery5(parameters, w, wc);
                } else if (ProjectValues.whichQueryTypeToCreate.equals("SIX")) {
                    this.generateBSBMQuery6(parameters, w, wc);
                } else if (ProjectValues.whichQueryTypeToCreate.equals("SEVEN")) {
                    this.generateBSBMQuery7(parameters, w, wc);
                } else if (ProjectValues.whichQueryTypeToCreate.equals("EIGHT")) {
                    this.generateBSBMQuery8(parameters, w, wc);
                } else if (ProjectValues.whichQueryTypeToCreate.equals("TEN")) {
                    this.generateBSBMQuery10(parameters, w, wc);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void generateBSBMQuery1(String[] parameters,  BufferedWriter w,  BufferedWriter wc) throws IOException {
        // print the select and then the construct query
        String param1 = "<" + parameters[0] + ">";
        String param2 = "<" + parameters[1] + ">";
        String param3 = "<" + parameters[2] + ">";
        String selectQuery = String.format(BSBMQuery1.select_query, param1, param2, param3);
        w.write(selectQuery.replaceAll("\n", " "));
        w.newLine();
        String constructQuery = String.format(BSBMQuery1.construct_query, param1, param2, param3, param1, param2, param3);
        wc.write(constructQuery.replaceAll("\n", " "));
        wc.newLine();
    }

    public void generateBSBMQuery2(String[] parameters,  BufferedWriter w,  BufferedWriter wc) throws IOException {
        String param1 = "<" + parameters[0] + ">";
        String selectQuery = String.format(BSBMQuery2.select, param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1);
        w.write(selectQuery.replaceAll("\n", " "));
        w.newLine();
        String constructQuery = String.format(BSBMQuery2.construct, param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1);
        wc.write(constructQuery.replaceAll("\n", " "));
        wc.newLine();
    }

    private void generateBSBMQuery3(String[] parameters, BufferedWriter w, BufferedWriter wc) throws IOException {
        String param1 = "<" + parameters[0] + ">";
        String param2 = "<" + parameters[1] + ">";
        String param3 = "<" + parameters[2] + ">";
        String selectQuery = String.format(BSBMQuery3.select, param1, param2, param3);
        w.write(selectQuery.replaceAll("\n", " "));
        w.newLine();
        String constructQuery = String.format(BSBMQuery3.construct, param1, param2, param3,
                param1, param2, param3);
        wc.write(constructQuery.replaceAll("\n", " "));
        wc.newLine();
    }

    private void generateBSBMQuery5(String[] parameters, BufferedWriter w, BufferedWriter wc) throws IOException {
        String param1 = "<" + parameters[0] + ">";
        String selectQuery = String.format(BSBMQuery5.select, param1, param1, param1, param1);
        w.write(selectQuery.replaceAll("\n", " "));
        w.newLine();
        String constructQuery = String.format(BSBMQuery5.construct, param1, param1, param1, param1,
                param1, param1, param1, param1);
        wc.write(constructQuery.replaceAll("\n", " "));
        wc.newLine();
    }

    private void generateBSBMQuery6(String[] parameters, BufferedWriter w, BufferedWriter wc) throws IOException {
        String param1 =  parameters[0];
        String selectQuery = String.format(BSBMQuery6.select, param1);
        w.write(selectQuery.replaceAll("\n", " "));
        w.newLine();
        String constructQuery = String.format(BSBMQuery6.construct, param1, param1);
        wc.write(constructQuery.replaceAll("\n", " "));
        wc.newLine();
    }

    private void generateBSBMQuery7(String[] parameters, BufferedWriter w, BufferedWriter wc) throws IOException {
        String param1 = "<" + parameters[0] + ">";
        String param2 = "<" + parameters[1] + ">";
        String selectQuery = String.format(BSBMQuery7.select, param1, param1, param2, param1);
        w.write(selectQuery.replaceAll("\n", " "));
        w.newLine();
        String constructQuery = String.format(BSBMQuery7.construct, param1, param1, param2, param1,
                param1, param1, param2, param1);
        wc.write(constructQuery.replaceAll("\n", " "));
        wc.newLine();
    }

    private void generateBSBMQuery8(String[] parameters, BufferedWriter w, BufferedWriter wc) throws IOException {
        String param1 = "<" + parameters[0] + ">";
        String selectQuery = String.format(BSBMQuery8.select, param1);
        w.write(selectQuery.replaceAll("\n", " "));
        w.newLine();
        String constructQuery = String.format(BSBMQuery8.construct, param1, param1);
        wc.write(constructQuery.replaceAll("\n", " "));
        wc.newLine();
    }

    private void generateBSBMQuery10(String[] parameters, BufferedWriter w, BufferedWriter wc) throws IOException {
        String param1 = "<" + parameters[0] + ">";
        String selectQuery = String.format(BSBMQuery10.select, param1);
        w.write(selectQuery.replaceAll("\n", " "));
        w.newLine();
        String constructQuery = String.format(BSBMQuery10.construct, param1, param1);
        wc.write(constructQuery.replaceAll("\n", " "));
        wc.newLine();
    }

    @Deprecated
    public void generateBSBMQuery1_() {
        // open/get the connection and the query
        List<String[]> queryValues = new ArrayList<>();

        // open the file with the query values
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(buildingQueryValuesPath))) {
            // read the values and put them in an array
            String line = "";
            reader.readLine();// the first line usually has headers

            while((line = reader.readLine()) != null) { // read all the values and save them in a list
                String[] values = line.split(",");
                queryValues.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // if we are here, we have all the query values. Open the reader to write the queries
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(outputSelectQueryFile))) {
            BufferedWriter wc = Files.newBufferedWriter(Paths.get(outputConstructQueryFile));
            // we randomly choose the queries with a normal distribution, thus to simulate the random
            // coming of queries from different users

            // prepare the distribution - the lower the standard deviation, the more frequent certain queries will be
            double stdv = Math.max(1, (double)  queryValues.size() /alpha);
            NormalDistribution distribution = new NormalDistribution((float) queryValues.size()/2, stdv);

            for(int i = 0; i < queriesToCreate; ++i) {
                // we create as many queries as indicated in queriesToCreate

                // get a number that is the index of the query that we want. We make sure that we do not go out of index exception
                // we sample until we get a valid index
                int randomNum =  -1;
                while(randomNum < 0 || randomNum > queryValues.size() - 1)
                    randomNum = (int) Math.floor(distribution.sample());

                // now get the values
                String[] parameters = queryValues.get(randomNum);

                // print the select and then the construct query
                String param1 = "<" + parameters[0] + ">";
                String param2 = "<" + parameters[1] + ">";
                String param3 = "<" + parameters[2] + ">";
                String selectQuery = String.format(BSBMQuery1.select_query, param1, param2, param3);
                w.write(selectQuery.replaceAll("\n", " "));
                w.newLine();
                String constructQuery = String.format(BSBMQuery1.construct_query, param1, param2, param3, param1, param2, param3);
                wc.write(constructQuery.replaceAll("\n", " "));
                wc.newLine();
            }
            wc.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }






    public static void main(String[] args) {
        ProjectValues.init();
        GenerateQueries execution = new GenerateQueries();
        execution.generateBSBMQuery();

    }
}
