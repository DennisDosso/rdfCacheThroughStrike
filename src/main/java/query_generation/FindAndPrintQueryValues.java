package query_generation;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import properties.ProjectPaths;
import properties.ProjectValues;
import query_generation.templates.*;
import utils.TripleStoreHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/** This class asks one query to the database and prints the results.
 * It is used to create the csv files that allow us to have build other
 * queries
 * */
public class FindAndPrintQueryValues {

    RepositoryConnection rc;

    /** Writer that will print the values on the output file
     * */
    FileWriter outputWriter;

    public FindAndPrintQueryValues(String[] args) {
        if(args.length >= 2) {
            ProjectPaths.init(args[0]);
            ProjectValues.init(args[1]);
        } else {
            ProjectValues.init();
            ProjectPaths.init();
        }
        this.rc = TripleStoreHandler.getConnection(ProjectPaths.databaseIndexDirectory, this.getClass().getName());
        try {
            outputWriter = new FileWriter(ProjectPaths.queryBuildingValuesFile, true);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Based on the value of the property whichQueryTypeToCreate, it
     * executes one of the queries to obtain values, and prints the results in a csv file
     * */
    public void findAndPrintQueryValues() {
        if(ProjectValues.whichQueryTypeToCreate.equals("ONE")) {
            this.findAndPrintQueryValuesONE();
        } else if(ProjectValues.whichQueryTypeToCreate.equals("TWO")) {
            this.findAndPrintQueryValuesTWO();
        } else if(ProjectValues.whichQueryTypeToCreate.equals("THREE")) {
            this.findAndPrintQueryValuesTHREE();
        } else if(ProjectValues.whichQueryTypeToCreate.equals("FIVE")) {
            this.findAndPrintQueryValuesFIVE();
        } else if(ProjectValues.whichQueryTypeToCreate.equals("SIX")) {
            this.findAndPrintQueryValuesSIX();
        } else if(ProjectValues.whichQueryTypeToCreate.equals("SEVEN")) {
            this.findAndPrintQueryValuesSEVEN();
        } else if(ProjectValues.whichQueryTypeToCreate.equals("EIGHT")) {
            this.findAndPrintQueryValuesEIGHT();
        } else if(ProjectValues.whichQueryTypeToCreate.equals("TEN")) {
            this.findAndPrintQueryValuesTEN();
        } else if(ProjectValues.whichQueryTypeToCreate.equals("ANY")) {
            this.findAndPrintQueryValuesANY();
        }
    }

    private void findAndPrintQueryValuesONE() {
        TupleQuery query = this.rc.prepareTupleQuery(BSBMQuery1.building_values_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                String param1 = solution.getValue("prodType").toString();
                String param2 = solution.getValue("prodFeat1").toString();
                String param3 = solution.getValue("prodFeat2").toString();
                try {
                    this.outputWriter.write(param1 + "," + param2 + "," + param3 + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void findAndPrintQueryValuesTWO() {
        TupleQuery query = this.rc.prepareTupleQuery(BSBMQuery2.building_values_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                String param1 = solution.getValue("product").toString();
                try {
                    this.outputWriter.write(param1 + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void findAndPrintQueryValuesTHREE() {
        TupleQuery query = this.rc.prepareTupleQuery(BSBMQuery3.building_values_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                String param1 = solution.getValue("product_type").toString();
                String param2 = solution.getValue("product_feature_1").toString();
                String param3 = solution.getValue("product_feature_2").toString();
                try {
                    this.outputWriter.write(param1 + "," + param2 + "," + param3 + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void findAndPrintQueryValuesFIVE() {
        TupleQuery query = this.rc.prepareTupleQuery(BSBMQuery5.building_values_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                String param1 = solution.getValue("pxyz").toString();
                try {
                    this.outputWriter.write(param1 + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void findAndPrintQueryValuesSIX() {
        TupleQuery query = this.rc.prepareTupleQuery(BSBMQuery6.building_values_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                String param1 = solution.getValue("label").toString();
                try {
                    this.outputWriter.write(param1 + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void findAndPrintQueryValuesSEVEN() {
        TupleQuery query = this.rc.prepareTupleQuery(BSBMQuery7.building_values_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                String param1 = solution.getValue("pxyz").toString();
                String param2 = solution.getValue("country").toString();
                try {
                    this.outputWriter.write(param1 + "," + param2 + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void findAndPrintQueryValuesEIGHT() {
        TupleQuery query = this.rc.prepareTupleQuery(BSBMQuery8.building_values_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                String param1 = solution.getValue("pxyz").toString();
                try {
                    this.outputWriter.write(param1 + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void findAndPrintQueryValuesTEN() {
        TupleQuery query = this.rc.prepareTupleQuery(BSBMQuery10.building_values_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                String param1 = solution.getValue("productXYZ").toString();
                try {
                    this.outputWriter.write(param1 + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void findAndPrintQueryValuesANY() {
        TupleQuery query = this.rc.prepareTupleQuery(ProjectValues.sparql_tuples_query);
        try(TupleQueryResult result = query.evaluate()) {
            for(BindingSet solution : result) {
                Set<String> bindingNames = solution.getBindingNames();
                try {
                    for(String b : bindingNames) {
                        String param = solution.getValue(b).toString();
                        this.outputWriter.write(param + ",");
                    }
                    this.outputWriter.write("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }




    public void close() {
        try {
            this.outputWriter.flush();
            this.outputWriter.close();
            TripleStoreHandler.closeConnection(this.getClass().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // create object and read the properties
        FindAndPrintQueryValues execution = new FindAndPrintQueryValues(args);
        execution.findAndPrintQueryValues();

        execution.close();
        System.out.println("done");
        System.exit(0);
    }


}
