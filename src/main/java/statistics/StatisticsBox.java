package statistics;

/** Little boject to contain the information about the queries, such as execution time,
 * result set, hit or miss.
 *
 * */
public class StatisticsBox {

    public int queryNo;

    public long time;

    public String query;

    public boolean hit;

    public int resultSetSize;

    /**
     * Set a time expressed as ns in the time field as ms
     * */
    public void setTimeAsMs(long ns) {
        this.time = ns / 1000000;
    }

    public void setHitOrMiss(String value) {
        if(value.equals("hit"))
            this.hit = true;
        else if(value.equals("miss"))
            this.hit = false;
        else if(value.equals("timeout"))
            this.hit = false;
    }
}
