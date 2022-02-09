package statistics;

import properties.ProjectPaths;
import properties.ProjectValues;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A generic statistic process
 *
 * */
public class StatisticProcess {

    List<Long> times; // times required on the system (total)
    List<Long> cacheMissTimes; // times required on the cache (when there is a miss)
    List<Long> dbMissTimes; // times required on the DB (sfter the miss)

    /** list with the values corresponding to the average time execution of 1 query
     * */
    List<Double> averages;

    int hits, misses, timeouts;

    BufferedReader queryReader;
    BufferedReader wholeDbReader;
    BufferedReader cacheReader;

    public StatisticProcess() {
        ProjectValues.init();
        ProjectPaths.init();

        times = new ArrayList<>();
        cacheMissTimes = new ArrayList<>();
        dbMissTimes = new ArrayList<>();
        averages = new ArrayList<>();

        this.hits = 0;
        this.misses = 0;
        this.timeouts = 0;

        try {
            queryReader = Files.newBufferedReader(Paths.get(ProjectPaths.selectQueryFile));
            wholeDbReader = Files.newBufferedReader(Paths.get(ProjectPaths.wholeDBresultFile));
            cacheReader = Files.newBufferedReader(Paths.get(ProjectPaths.cacheResultFile));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void close() {
        try {
            wholeDbReader.close();
            cacheReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** given a line taken from a cache result file,extract the
     * information and updates, as needed, the corresponding lists.
     *
     * */
    public void processCacheLine(String line) {
        // read the data
        String[] parts = line.split(",");
        if(parts[0].equals("miss")) { // this is a miss
            long time = Long.parseLong(parts[1]);
            long cacheTime = Long.parseLong(parts[2]);
            long dbTime = Long.parseLong(parts[3]);
            times.add(time); cacheMissTimes.add(cacheTime); dbMissTimes.add(dbTime);
            misses++;
        } else if(parts[0].equals("hit")) { // this is a hit
            long time = Long.parseLong(parts[1]);
            times.add(time);
            hits++;
        } else if(parts[0].equals("timeout")) { // this is a timeout
            long time = Long.parseLong(parts[1]);
            times.add(time);
            timeouts++;
        }
    }
}
