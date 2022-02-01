package threads;

import batch.QueryingProcess;

import properties.ProjectValues;
import utils.ReturnBox;
import utils.SqlStrings;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public class DealWithCapOnTheCacheBashThread implements Callable<ReturnBox>  {

    private QueryingProcess process;

    public DealWithCapOnTheCacheBashThread(QueryingProcess qp) {
        this.process = qp;
    }

    @Override
    public ReturnBox call()  {
        ReturnBox rb = new ReturnBox();
        // I decided that each timeframe has an equal quantity of available memory
        int timeframeCap = (int) ProjectValues.cap / ProjectValues.timeframes;

        // first, ask if the cache has more triples than necessary
        String q = String.format(SqlStrings.CHECK_HOW_MANY_TRIPLES, ProjectValues.schema);

        try {
            PreparedStatement count_stmt = this.process.rdbConnection.prepareStatement(q);
            count_stmt.setInt(1, this.process.timeframe);
            ResultSet r = count_stmt.executeQuery();
            if(!r.next())
                return null;
            int timeFrameSize = r.getInt(1);

            // if necessary, reduce the size of the current timeframe
            if(timeFrameSize > timeframeCap) {
                this.process.reduceTimeFrameSize(timeFrameSize, timeframeCap);
            }

            r.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch(Exception e2) {
            e2.printStackTrace();
        }

        rb.foundSomething = true;
        return rb;
    }
}
