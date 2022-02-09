package utils;

import java.util.List;

public class NumberUtils {

    public static double averageInt(List<Integer> l) {
        int sum = 0;
        for(int i : l) {
            sum += i;
        }
        double avg = (double) sum / l.size();
        return avg;
    }

    public static double averageDouble(List<Double> l) {
        double sum = 0;
        for(double i : l) {
            sum += i;
        }
        double avg =  sum / l.size();
        return avg;
    }

    public static double averageLong(List<Long> l) {
        long sum = 0;
        for(long i : l) {
            sum += i;
        }
        double avg = (double) sum / l.size();
        return avg;
    }

    /**
     * @param list the list with all the samples
     * @param average the average of the samples, already computed with other methods of this class
     * */
    public static double variance(List<Double> list, double average) {
        double st = 0;
        for(double l : list) {
            double c = (l - average) * (l -average);
            st += c;
        }
        return (double) st / list.size();
    }

    public static double standardDeviation(double variance) {
        return Math.sqrt(variance);
    }

    /** Computes the zInterval. If parameter is set to 1.96, we have confidence 95% and alpha 5
     * */
    public static double zInterval(double standardDeviation, int samples, double parameter) {
        double v = (parameter) * (standardDeviation / Math.sqrt(samples));
        return v;
    }
}
