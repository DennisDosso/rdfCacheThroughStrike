package threads;

import batch.QueryingProcess;
import utils.ReturnBox;

import java.util.List;
import java.util.concurrent.Callable;

public class LineageComputationThread implements Callable<ReturnBox>  {

    private QueryingProcess process;

    public LineageComputationThread(QueryingProcess p) {
        this.process = p;
    }


    @Override
    public ReturnBox call() {
        ReturnBox box = new ReturnBox();

        // compute the lineage of the query. We use a method of the same class that called this thread to do this
        long start = System.currentTimeMillis();
        List<String[]> lineage = this.process.computeQueryLineage(this.process.constructQuery);
        box.lineage = lineage;
        long elapsed = System.currentTimeMillis() - start;

        //set the time and if we found something
        box.queryTime = elapsed;
        if(lineage.size() > 0)
            box.foundSomething = true;
        else
            box.foundSomething = false;

        box.resultSetSize = lineage.size();
        return box;
    }
}
