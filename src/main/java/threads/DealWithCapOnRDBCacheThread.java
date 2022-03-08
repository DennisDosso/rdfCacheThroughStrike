package threads;

import batch.QueryVault;
import properties.ProjectValues;
import utils.ReturnBox;
import utils.SqlStrings;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/** Checks the size of the relational database table
 * working as cache and reduces its size based on a LRU (least Recently Used) Strategy
 * */
public class DealWithCapOnRDBCacheThread implements Callable<ReturnBox> {

    private QueryVault process;

    public DealWithCapOnRDBCacheThread(QueryVault qp) {
        this.process = qp;
    }

    @Override
    public ReturnBox call()  {
        ReturnBox rb = new ReturnBox();
        long start = System.currentTimeMillis();

        // first, count how many tuples we have in the RDB
        String count = String.format(SqlStrings.CHECK_HOW_MANY_TRIPLES_IN_RDB_CACHE, ProjectValues.schema);
        try{
            PreparedStatement count_stmt = this.process.rdbConnection.prepareStatement(count);
            ResultSet r = count_stmt.executeQuery();
            if(!r.next())
                return null;

            // get the number of triples
            int size = r.getInt(1);

            if(size > ProjectValues.cap) {
                // it is necessary to reduce the size of the cache
                this.applyTheCapToRelationalDBCacheTable(size, ProjectValues.cap);
            }
            r.close();
            count_stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - start;
        rb.queryTime = elapsed;
        return rb;
    }

    private void applyTheCapToRelationalDBCacheTable(int size, int cap) throws SQLException {
        int currentSize = size;
        // remove tuples from the cache table as long as it is necessary
        do {
            // first, find the query number of the query that has been used least recently
            int least_recently_used_id = this.getLeastRecentlyUsedQueryNumberInRDBCacheTable();
            int deletedRows = this.removeTriplesFromThisInsertiontime(least_recently_used_id);
            currentSize -= deletedRows;
        } while (currentSize > cap);
    }

    private int removeTriplesFromThisInsertiontime(int least_recently_used_id) throws SQLException {
        String sql  = String.format(SqlStrings.DELETE_LEAST_RECENTLY_USED_QUERY_IN_BASELINECACHE, ProjectValues.schema);
        PreparedStatement ps = this.process.rdbConnection.prepareStatement(sql);
        int deleted =   ps.executeUpdate();
        ps.close();
        return deleted;
    }

    private int getLeastRecentlyUsedQueryNumberInRDBCacheTable() throws SQLException {
        String sql = String.format(SqlStrings.FIND_OLDEST_QUERY_NUMBER_IN_BASELINECACHE, ProjectValues.schema);
        PreparedStatement min = this.process.rdbConnection.prepareStatement(sql);
        ResultSet rs = min.executeQuery();
        if(rs.next()) {
            int res = rs.getInt(1); rs.close(); min.close();
            return  res;
        }
        rs.close(); min.close();
        return -1;
    }
}
