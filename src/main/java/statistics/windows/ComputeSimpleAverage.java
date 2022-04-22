package statistics.windows;

import properties.ProjectPaths;
import properties.ProjectValues;
import utils.NumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Use this when you simply want to compute the average of a series of values.
 * Use this with the file containing the values used to compute the lineages.
 * */
public class ComputeSimpleAverage {

    public ComputeSimpleAverage() {
        ProjectPaths.init();
        ProjectValues.init();
    }

    public void computeAverageAndZval(int windowNumber) {
        List<Double> times = new ArrayList<>(); // times required on the system (total)

        int limit = ProjectValues.queriesToCheck;
        int startingQuery = ProjectValues.queriesToCheck * windowNumber;

        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.averageResultFile))) {
            String line = "";
            int counter = 0;

            for (int j = 0; j <= startingQuery; j++) { // get to the line we actually want
                r.readLine();
            }
            while((line = r.readLine()) != null) {
                double val = Double.parseDouble(line);
                times.add(val);
                counter++;
                if(counter > limit)
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double avgTime = NumberUtils.averageDouble(times);
        double variance = NumberUtils.variance(times, avgTime);
        double standardDeviation = NumberUtils.standardDeviation(variance);
        double zInterval =NumberUtils.zInterval(standardDeviation, times.size(), 1.96);
        // we round a little bit because I want to copy-paste this into latex
        avgTime = Math.round(avgTime * 100.0) / 100.0;
        zInterval = Math.round(zInterval * 100.0) / 100.0;
        System.out.println("average time for window " + (windowNumber + 1));
        System.out.println("$" + avgTime + "\\pm" + zInterval + "$");
    }

    public static void main(String[] args) {
        ComputeSimpleAverage execution = new ComputeSimpleAverage();
        execution.computeAverageAndZval(0);
        execution.computeAverageAndZval(1);
        execution.computeAverageAndZval(2);
        execution.computeAverageAndZval(3);
        execution.computeAverageAndZval(4);
        execution.computeAverageAndZval(5);
        execution.computeAverageAndZval(6);
        execution.computeAverageAndZval(7);
        execution.computeAverageAndZval(8);
        execution.computeAverageAndZval(9);
    }
}
