package virtuoso;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import properties.ProjectValues;
import utils.SilenceLog4J;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class TestVirtuosoConnection {
    public static void main(String[] args) {
        SilenceLog4J.silence();

        VirtGraph virtuosoDatabase;
        String connectionString = "jdbc:virtuoso://localhost:1111";
        String dbUri = "http://localhost:8890/BSBM250";
        String user = "dba";
        String password = "dba";

        virtuosoDatabase = new VirtGraph(dbUri, connectionString, user, password);

        String select = "SELECT * WHERE {?s ?p ?o} LIMIT 10";
        Query tupleQuery = QueryFactory.create(select);
        VirtuosoQueryExecution vqu = VirtuosoQueryExecutionFactory.create(tupleQuery, virtuosoDatabase);
        ResultSet results = vqu.execSelect();
        while(results.hasNext()) {
           QuerySolution rs =  results.next();
           System.out.println(rs.get("s") + " " + rs.get("p") + " " + rs.get("s"));
        }

        System.out.println("done");
    }
}
