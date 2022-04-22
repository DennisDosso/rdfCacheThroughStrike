package threads;

import batch.QueryVault;
import properties.ProjectValues;
import utils.ConvertToHash;
import utils.ReturnBox;
import utils.SqlStrings;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public class QueryRelationalDBCache implements Callable<ReturnBox>  {

    private QueryVault process;

    public QueryRelationalDBCache(QueryVault p) {
        this.process = p;
    }

    public ReturnBox call() {
        ReturnBox box = new ReturnBox();
        box.resultSetSize = 0;
        long start = System.currentTimeMillis();

        // convert the query into a hash
        String queryHash = ConvertToHash.convertToHashSHA256(this.process.selectQuery);

        // ask the relational database if it knows about this query
        String query = String.format(SqlStrings.GET_BASELINE_ANSWER, ProjectValues.schema);
        try {
            PreparedStatement ps = this.process.rdbConnection.prepareStatement(query);
            ps.setString(1, queryHash);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) { // cache hit
                box.foundSomething = true;
                box.queryTime = System.currentTimeMillis() - start;
                int counter = 1;
                while(rs.next()) {
                    counter++;
                }
                box.resultSetSize = counter;
            } else { // cache miss
                box.foundSomething = false;
                box.queryTime = System.currentTimeMillis() - start;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return box;
    }


}
