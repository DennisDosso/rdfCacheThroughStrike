package query_generation;

import org.apache.commons.math3.distribution.NormalDistribution;
import properties.ProjectPaths;
import properties.ProjectValues;
import query_generation.templates.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Use this class to generate queries for the mixed scenario.
 * It uses the queries from the uniform scenario, so you need to have at least all of those before
 *
 * */
public class GenerateMixedCaseScenarioQueries extends GenerateQueries {

    public GenerateMixedCaseScenarioQueries() {
        super();
    }

    public void printMixedScenariosQueries(String queryValuesDirectoryPath) {
        // first, import all the data from the csv files
        String path1 = queryValuesDirectoryPath + "/Q1.csv";
        String path2 = queryValuesDirectoryPath + "/Q2.csv";
        String path3 = queryValuesDirectoryPath + "/Q3.csv";
        String path5 = queryValuesDirectoryPath + "/Q5.csv";
        String path6 = queryValuesDirectoryPath + "/Q6.csv";
        String path7 = queryValuesDirectoryPath + "/Q7.csv";
        String path8 = queryValuesDirectoryPath + "/Q8.csv";
        String path10 = queryValuesDirectoryPath + "/Q10.csv";

        List<String> list1 = new ArrayList<>();
        this.readAllCsvValues(path1, list1);
        List<String> list2 = new ArrayList<>();
        this.readAllCsvValues(path2, list2);
        List<String> list3 = new ArrayList<>();
        this.readAllCsvValues(path3, list3);
        List<String> list5 = new ArrayList<>();
        this.readAllCsvValues(path5, list5);
        List<String> list6 = new ArrayList<>();
        this.readAllCsvValues(path6, list6);
        List<String> list7 = new ArrayList<>();
        this.readAllCsvValues(path7, list7);
        List<String> list8 = new ArrayList<>();
        this.readAllCsvValues(path8, list8);
        List<String> list10 = new ArrayList<>();
        this.readAllCsvValues(path10, list10);

        List<List<String>> listOfLists = new ArrayList<>();
        listOfLists.add(list1); listOfLists.add(list2); listOfLists.add(list3); listOfLists.add(list5);
        listOfLists.add(list6); listOfLists.add(list7); listOfLists.add(list8); listOfLists.add(list10);

        List<QueryHolder> queriesToPrint = new ArrayList<>();

        // a set to contain the csv values that we extracted, to avoid having multiple equal values
        Set<String> csvValuesSet = new HashSet<>();
        for(int i = 0; i < ProjectValues.queriesToCreate; ++i) {
            // choose randomly what type of query to create
            int queryClass = ThreadLocalRandom.current().nextInt(0, 8);
            List<String> csvList = listOfLists.get(queryClass);

            // now choose randomly one set of csv values for this query
            String csvValues;
            do{
                int randomNum = ThreadLocalRandom.current().nextInt(0, csvList.size());
                csvValues = csvList.get(randomNum);
            } while (csvValuesSet.contains(csvValues));
            csvValuesSet.add(csvValues);
            // now build the query
            QueryHolder qH = this.buildThisQueryClassWithTheseCsvValues(queryClass, csvValues);
            queriesToPrint.add(qH);
        }

        // now we have the list with the queries
        Collections.shuffle(queriesToPrint);

        // now we print the queries
        this.printTheQueries(queriesToPrint);


    }

    private void printTheQueries(List<QueryHolder> queriesToPrint) {
        // prepare the distribution - the lower the standard deviation, the more frequent certain queries will be
        double stdv = Math.max(1, (double)  queriesToPrint.size() / ProjectValues.alpha);
        NormalDistribution distribution = new NormalDistribution((float) queriesToPrint.size()/2, stdv);

        // open the writers
        try(BufferedWriter selectWriter = Files.newBufferedWriter(Paths.get(ProjectPaths.selectQueryFile));
            BufferedWriter constructWriter = Files.newBufferedWriter(Paths.get(ProjectPaths.constructQueryFile))) {

            for(int i = 0; i < ProjectValues.queriesToCreate; ++i) {
                // decide which query to print
                int randomNum =  -1;
                while(randomNum < 0 || randomNum > queriesToPrint.size() - 1)
                    randomNum = (int) Math.floor(distribution.sample());

                QueryHolder qH = queriesToPrint.get(randomNum);
                selectWriter.write(qH.selectQuery);
                selectWriter.newLine();
                constructWriter.write(qH.constructQuery);
                constructWriter.newLine();
            }

            // we are done
            selectWriter.flush();
            constructWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private QueryHolder buildThisQueryClassWithTheseCsvValues(int queryClass, String csvValues) {
        String[] parameters = csvValues.split(",");
        QueryHolder qH = null;
        if(queryClass == 0) {
            qH = this.createQueryForQueryClass1(parameters);
        } else if(queryClass == 1) {
            qH = this.createQueryForQueryClass2(parameters);
        } else if(queryClass == 2) {
            qH = this.createQueryForQueryClass3(parameters);
        } else if(queryClass == 3) {
            qH = this.createQueryForQueryClass5(parameters);
        } else if(queryClass == 4) {
            qH = this.createQueryForQueryClass6(parameters);
        } else if(queryClass == 5) {
            qH = this.createQueryForQueryClass7(parameters);
        } else if(queryClass == 6) {
            qH = this.createQueryForQueryClass8(parameters);
        } else if(queryClass == 7) {
            qH = this.createQueryForQueryClass10(parameters);
        }

        return qH;
    }

    private QueryHolder createQueryForQueryClass10(String[] parameters) {
        QueryHolder qH = new QueryHolder();

        String param1 = "<" + parameters[0] + ">";
        String selectQuery = String.format(BSBMQuery10.select, param1);

        qH.selectQuery = selectQuery.replaceAll("\n", " ");

        String constructQuery = String.format(BSBMQuery10.construct, param1, param1);
        qH.constructQuery = constructQuery.replaceAll("\n", " ");
        return qH;
    }

    private QueryHolder createQueryForQueryClass8(String[] parameters) {
        QueryHolder qH = new QueryHolder();

        String param1 = "<" + parameters[0] + ">";
        String selectQuery = String.format(BSBMQuery8.select, param1);

        qH.selectQuery = selectQuery.replaceAll("\n", " ");

        String constructQuery = String.format(BSBMQuery8.construct, param1, param1);
        qH.constructQuery = constructQuery.replaceAll("\n", " ");
        return qH;
    }

    private QueryHolder createQueryForQueryClass7(String[] parameters) {
        QueryHolder qH = new QueryHolder();

        String param1 = "<" + parameters[0] + ">";
        String param2 = "<" + parameters[1] + ">";
        String selectQuery = String.format(BSBMQuery7.select, param1, param1, param2, param1);

        qH.selectQuery = selectQuery.replaceAll("\n", " ");

        String constructQuery = String.format(BSBMQuery7.construct, param1, param1, param2, param1,
                param1, param1, param2, param1);
        qH.constructQuery = constructQuery.replaceAll("\n", " ");
        return qH;
    }

    private QueryHolder createQueryForQueryClass6(String[] parameters) {
        QueryHolder qH = new QueryHolder();

        String param1 =  parameters[0];
        String selectQuery = String.format(BSBMQuery6.select, param1);

        qH.selectQuery = selectQuery.replaceAll("\n", " ");

        String constructQuery = String.format(BSBMQuery6.construct, param1, param1);
        qH.constructQuery = constructQuery.replaceAll("\n", " ");
        return qH;
    }

    private QueryHolder createQueryForQueryClass5(String[] parameters) {
        QueryHolder qH = new QueryHolder();

        String param1 = "<" + parameters[0] + ">";
        String selectQuery = String.format(BSBMQuery5.select, param1, param1, param1, param1);

        qH.selectQuery = selectQuery.replaceAll("\n", " ");

        String constructQuery = String.format(BSBMQuery5.construct, param1, param1, param1, param1,
                param1, param1, param1, param1);
        qH.constructQuery = constructQuery.replaceAll("\n", " ");
        return qH;
    }

    private QueryHolder createQueryForQueryClass3(String[] parameters) {
        QueryHolder qH = new QueryHolder();

        String param1 = "<" + parameters[0] + ">";
        String param2 = "<" + parameters[1] + ">";
        String param3 = "<" + parameters[2] + ">";

        String selectQuery = String.format(BSBMQuery3.select, param1, param2, param3);
        qH.selectQuery = selectQuery.replaceAll("\n", " ");

        String constructQuery = String.format(BSBMQuery3.construct, param1, param2, param3,
                param1, param2, param3);
        qH.constructQuery = constructQuery.replaceAll("\n", " ");
        return qH;
    }

    private QueryHolder createQueryForQueryClass2(String[] parameters) {
        QueryHolder qH = new QueryHolder();
        String param1 = "<" + parameters[0] + ">";
        String selectQuery = String.format(BSBMQuery2.select, param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1);
        qH.selectQuery = selectQuery.replaceAll("\n", " ");

        String constructQuery = String.format(BSBMQuery2.construct, param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1, param1,
                param1, param1);
        qH.constructQuery = constructQuery.replaceAll("\n", " ");
        return qH;
    }

    private QueryHolder createQueryForQueryClass1(String[] parameters) {
        QueryHolder qH = new QueryHolder();
        // print the select and then the construct query
        String param1 = "<" + parameters[0] + ">";
        String param2 = "<" + parameters[1] + ">";
        String param3 = "<" + parameters[2] + ">";
        
        String selectQuery = String.format(BSBMQuery1.select_query, param1, param2, param3);
        qH.selectQuery = selectQuery.replaceAll("\n", " ");

        String constructQuery = String.format(BSBMQuery1.construct_query, param1, param2, param3, param1, param2, param3);
        qH.constructQuery = constructQuery.replaceAll("\n", " ");
        return qH;
    }

    private void readAllCsvValues(String path1, List<String> list1) {
        // open a reader for each query file in the queryValues directory
        try(BufferedReader reader1 = Files.newBufferedReader(Paths.get(path1))) {
            String line = "";
            reader1.readLine();// first line is usually a header, discard it
            while((line = reader1.readLine()) != null) {
                list1.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        GenerateMixedCaseScenarioQueries execution = new GenerateMixedCaseScenarioQueries();

        String queryValuesDirectoryPath = "/Users/dennisdosso/Documents/databases/BSBM/revival/1M/building_query_values";
        execution.printMixedScenariosQueries(queryValuesDirectoryPath);
        System.out.println("done");
    }


}
