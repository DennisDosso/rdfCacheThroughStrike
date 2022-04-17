package statistics.windows;

import properties.ProjectPaths;
import properties.ProjectValues;
import statistics.StatisticProcess;
import statistics.StatisticsBox;
import utils.NumberUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/** USed to compare the completeness of answers obtained from the cache
 *
 * Srt as properties:
 * the result file from the whole DB (wholeDBresultFile)
 * the result file from the cache execution (cacheResultFile)
 * the file containing the select queries (selectQueryFile)
 * the number of queries being considered
 * the size of the epoch
 * */
public class CompareCompletenessOnWindows extends StatisticProcess {

    public String oldWholeDbLine, wholeDbLine;


    public  CompareCompletenessOnWindows() { super(); }

    public void compareCompleteness(int windowNumber) throws IOException {
        int startingQuery = ProjectValues.queriesToCheck * windowNumber;

        // read information from files
        List<String> queryList = this.readQueriesFile(startingQuery);
        List<StatisticsBox> dbList = this.readWholeDbResults(startingQuery, queryList);
        List<StatisticsBox> cacheList = this.readCacheResults(startingQuery, queryList);

        // compute statistics
        System.out.println("Results for window: " + (windowNumber + 1));
        Map<Integer, StatisticsBox> dbMap = convertListToMap(dbList);
        this.computeStatistics(dbList, cacheList, dbMap);

        queryReader.close();
        cacheReader.close();
        wholeDbReader.close();
    }

    private Map<Integer, StatisticsBox> convertListToMap(List<StatisticsBox> dbList) {
        Map<Integer, StatisticsBox> m = new HashMap<>();
        for(StatisticsBox sb : dbList) {
            m.put(sb.queryNo, sb);
        }
        return m;
    }

    private void computeStatistics(List<StatisticsBox> dbList, List<StatisticsBox> cacheList, Map<Integer, StatisticsBox> dbMap) {
        int commonHits = 0;
        int specialHits = 0;
        int misses = 0;
        List<Double> commonCompletenessRatios = new ArrayList<>(); // completeness on queries seen more than once
        List<Double> specialCompletenessRatios = new ArrayList<>(); // completeness on queries seen for the first time
        List<Double> ratios = new ArrayList<>(); // all the completeness ratios for a hit, without distinction



        Set<String> alreadySeenQueries = new HashSet<>();
        int counter = 0;
        // now we read the two lists together
        for(int i = 0; i < cacheList.size(); ++i) {
            if(counter >= ProjectValues.queriesToCheck)
            {
                break;
            } else counter++;

            StatisticsBox cacheResult = cacheList.get(i);
            StatisticsBox dbResult = dbMap.get(cacheResult.queryNo);
            if(dbResult == null)
                continue;

            if(cacheResult.hit) {
                // found a hit
                commonHits++;
                int cacheResultSet = cacheResult.resultSetSize;
                int groundTruthResultSet = dbResult.resultSetSize;

                double r = (double) cacheResultSet/groundTruthResultSet;
                if(r <= 1.0) {
                    ratios.add( (double) cacheResultSet/groundTruthResultSet);
                }

                if(!alreadySeenQueries.contains(cacheResult.query)) {
                    // this query was found for the first time, it is a "special" hit
                    double completenessRatio = (double) cacheResultSet / groundTruthResultSet;
                    if(completenessRatio > 1) {
                        // just in case. This may due to some timeout on the part of the whole DB vs the cache
                        continue;
                    }
                    specialHits++;
                    specialCompletenessRatios.add(completenessRatio);
                } else {
                    // this is a query that we found previously
                    double completenessRatio = (double) cacheResultSet / groundTruthResultSet;
                    if(completenessRatio >=1) completenessRatio = 1;
                    commonCompletenessRatios.add(completenessRatio);
                }
            } else { // we had a miss
                misses++;
            }

            // in any case, add now this query to the set of queries already seen
            alreadySeenQueries.add(cacheResult.query);
        } // seen all queries

        // total number of hits / total number of queries
        double overallHitRatio = (double) (commonHits) / ProjectValues.queriesToCheck;
        System.out.println("hits / total number of queries: " + overallHitRatio);

        // hits seen for the first time / total number of hits
        double specialHitsRatio = (double) (specialHits) / commonHits;
        System.out.println("first-time hits / hits: " + specialHitsRatio);

        double specialComplAvg = NumberUtils.averageDouble(specialCompletenessRatios);
        double commonComplAvg = NumberUtils.averageDouble(commonCompletenessRatios); // this should be 1
        double completenessAvg = NumberUtils.averageDouble(ratios);

        double variance = NumberUtils.variance(specialCompletenessRatios, specialComplAvg);
        double standardDeviation = NumberUtils.standardDeviation(variance);
        double zInterval =NumberUtils.zInterval(standardDeviation, specialCompletenessRatios.size(), 1.96);

        specialComplAvg = Math.round(specialComplAvg * 100.0) / 100.0;
        zInterval = Math.round(zInterval * 100.0) / 100.0;
        System.out.println("first-time hits completeness: " + specialComplAvg + "\\pm" + zInterval);

        variance = NumberUtils.variance(ratios, completenessAvg);
        standardDeviation = NumberUtils.standardDeviation(variance);
        zInterval = NumberUtils.zInterval(standardDeviation, ratios.size(), 1.96);
        completenessAvg = Math.round(completenessAvg * 100.0) / 100.0;
        zInterval = Math.round(zInterval * 100.0) / 100.0;
        System.out.println("Overall completeness: " + completenessAvg + "\\pm" + zInterval);



    }

    /** Reads all the queries in a window of size ProjectValues.queriesToCheck
     * starting at startingQuery and memorizes them in the RAM.
     * */
    private List<String> readQueriesFile(int startingQuery) throws IOException {
        List<String> queryList = new ArrayList<>();
        String q = "";
        // first of all, skip the useless queries
        queryReader = Files.newBufferedReader(Paths.get(ProjectPaths.selectQueryFile));
        for(int j = 0; j < startingQuery; ++j) {
            queryReader.readLine();
        }

        int counter = 0;
        while((q = queryReader.readLine()) != null) {
            // read as many queries as we want
            if(counter >= ProjectValues.queriesToCheck)
                break;
            queryList.add(q);
            counter++;
        }
        return queryList;
    }

    /** reads the file containing the whole DB data and saves the information in-RAM
     * @return*/
    private List<StatisticsBox> readWholeDbResults(int startingQuery, List<String> queryList) throws IOException {
        String oldLine = "";
        String line = "";
        List<StatisticsBox> list = new ArrayList<>();

        wholeDbReader = Files.newBufferedReader(Paths.get(ProjectPaths.wholeDBresultFile));
        oldLine = wholeDbReader.readLine();
        while(!oldLine.contains("QUERYNO " + startingQuery))
            oldLine = wholeDbReader.readLine();

        int counter = 0;

        while((line = wholeDbReader.readLine()) != null) { // read all the lines
            if(counter >= ProjectValues.queriesToCheck)
                break;
            if(oldLine.startsWith("#")) {
                // a new query
                StatisticsBox box = new StatisticsBox();
                // fin the number of query we are dealing with
                String[] parts = oldLine.split(" ");
                int queryNumber = Integer.parseInt(parts[2].replaceAll(",", ""));
                box.queryNo = queryNumber;
                // take the information we need: size of the result set, and query being used
                String size = parts[3].split(",")[0].trim();
                int resultSetSize = Integer.parseInt(size);
                box.resultSetSize = resultSetSize;
                try{
                    box.query = queryList.get(queryNumber - startingQuery);
                } catch (IndexOutOfBoundsException e) {
                    // something missing in the file, it may happen
                    counter++;
                    continue;
                }
                list.add(box);

                oldLine = line;
                counter++;
            } else {
                oldLine = line;
            }
        }

        return list;
    }

    private List<StatisticsBox> readCacheResults(int startingQuery, List<String> queryList) throws IOException {
        String oldLine = "";
        String line = "";
        List<StatisticsBox> list = new ArrayList<>();

        // reset the reader
        cacheReader = Files.newBufferedReader(Paths.get(ProjectPaths.cacheResultFile));
        oldLine = cacheReader.readLine();
        while(!oldLine.contains("QUERYNO " + startingQuery))
            oldLine = cacheReader.readLine();

        int counter = 0;

        while((line = cacheReader.readLine()) != null) {
            if(counter >= (ProjectValues.queriesToCheck -1))
                break;

            if(oldLine.startsWith("#")) { // beginning of a new query
                int queryNumber = Integer.parseInt(oldLine.split(" ")[2]);
                StatisticsBox box = new StatisticsBox();
                box.queryNo = queryNumber;

                // find out if it is hit or miss
                String hit = line.split(",")[0];
                box.setHitOrMiss(hit);

                try{
                    int resultSetSize = Integer.parseInt(line.split(",")[4]);
                    box.resultSetSize = resultSetSize;
                } catch (NumberFormatException e) {
//                    System.err.println(line);
                    box.resultSetSize = 0;
                }

                try{
                    box.query = queryList.get(queryNumber - (startingQuery));
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("query Number out of bound: " + queryNumber);
                }

                list.add(box);

                oldLine = line;
                counter++;
            } else {
                oldLine = line;
            }
        }

        return list;
    }

    public static void main(String[] args) {
        CompareCompletenessOnWindows execution = new CompareCompletenessOnWindows();

        try {
            execution.compareCompleteness(0);
            execution.compareCompleteness(1);
            execution.compareCompleteness(2);
            execution.compareCompleteness(3);
            execution.compareCompleteness(4);
            execution.compareCompleteness(5);
            execution.compareCompleteness(6);
            execution.compareCompleteness(7);
            execution.compareCompleteness(8);
            execution.compareCompleteness(9);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
