package statistics;

import properties.ProjectValues;
import utils.NumberUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** USed to compare the completeness of answers obtained from the cache
 *
 * Srt as properties:
 * the result file from the whole DB (wholeDBresultFile)
 * the result file from the cache execution (cacheResultFile)
 * the file containing the select queries (selectQueryFile)
 * the number of queries being considered
 * the size of the epoch
 * */
public class CompareCompleteness extends StatisticProcess {

    public String oldWholeDbLine, wholeDbLine;


    public  CompareCompleteness() {
        super();
    }

    public void compareCompleteness() throws IOException {
        // read information from files
        List<String> queryList = this.readQueriesFile();
        List<StatisticsBox> dbList = this.readWholeDbResults(queryList);
        List<StatisticsBox> cacheList = this.readCacheResults(queryList);

        // compute statistics
        this.computeStatistics(dbList, cacheList);
    }

    private void computeStatistics(List<StatisticsBox> dbList, List<StatisticsBox> cacheList) {
        int commonHits = 0;
        int specialHits = 0;
        int misses = 0;
        List<Double> commonCompletenessRatios = new ArrayList<>();
        List<Double> specialCompletenessRatios = new ArrayList<>();

        Set<String> alreadySeenQueries = new HashSet<>();
        int counter = 0;
        // now we read the two lists together
        for(int i = 0; i < cacheList.size(); ++i) {
            if(counter >= ProjectValues.queriesToCheck)
            {
                break;
            } else counter++;

            StatisticsBox dbResult = dbList.get(i);
            StatisticsBox cacheResult = cacheList.get(i);
            if(cacheResult.hit) {
                // found a hit
                commonHits++;
                int cacheResultSet = cacheResult.resultSetSize;
                int groundTruthResultSet = dbResult.resultSetSize;
                if(!alreadySeenQueries.contains(cacheResult.query)) {
                    // this query was found for the first time, it is a "special" hit
                    specialHits++;
                    double completenessRatio = (double) cacheResultSet / groundTruthResultSet;
                    if(completenessRatio >1) // just in case. It should not be possible though -  the debugging confirmed
                        completenessRatio = 1;
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
        System.out.println("total number of hits / total number of queries: " + overallHitRatio);

        // hits seen for the first time / total number of hits
        double specialHitsRatio = (double) (specialHits) / commonHits;
        System.out.println("hits seen for the first time / total number of hits: " + specialHitsRatio);

        double specialComplAvg = NumberUtils.averageDouble(specialCompletenessRatios);
        double commonComplAvg = NumberUtils.averageDouble(commonCompletenessRatios); // this should be 1

        double variance = NumberUtils.variance(specialCompletenessRatios, specialComplAvg);
        double standardDeviation = NumberUtils.standardDeviation(variance);
        double zInterval =NumberUtils.zInterval(standardDeviation, specialCompletenessRatios.size(), 1.96);

        specialComplAvg = Math.round(specialComplAvg * 100.0) / 100.0;
        zInterval = Math.round(zInterval * 100.0) / 100.0;


        System.out.println("Special completeness ratio: " + specialComplAvg + "\\pm" + zInterval);



    }

    /** Reads all the select queries we performed and memorizes them in-ram
     * */
    private List<String> readQueriesFile() throws IOException {
        List<String> queryList = new ArrayList<>();
        String q = "";
        while((q = queryReader.readLine()) != null) {
            queryList.add(q);
        }
        return queryList;
    }

    /** reads the file containing the whole DB data and saves the information in-RAM
     * @return*/
    private List<StatisticsBox> readWholeDbResults(List<String> queryList) throws IOException {
        String oldLine = "";
        String line = "";
        List<StatisticsBox> list = new ArrayList<>();

        oldLine = wholeDbReader.readLine();
        while((line = wholeDbReader.readLine()) != null) { // read all the lines
            if(oldLine.startsWith("#")) {
                // a new query
                StatisticsBox box = new StatisticsBox();
                // fin the number of query we are dealing with
                String[] parts = oldLine.split(" ");
                int queryNumber = Integer.parseInt(parts[2].replaceAll(",", ""));
                box.queryNo = queryNumber;
                // take the information we need: size of the result set, and query being used
                int resultSetSize = Integer.parseInt(parts[3].trim());
                box.resultSetSize = resultSetSize;
                box.query = queryList.get(queryNumber);
                list.add(box);

                oldLine = line;
            } else {
                oldLine = line;
            }
        }

        return list;
    }

    private List<StatisticsBox> readCacheResults(List<String> queryList) throws IOException {
        String oldLine = "";
        String line = "";
        List<StatisticsBox> list = new ArrayList<>();

        oldLine = cacheReader.readLine();
        while((line = cacheReader.readLine()) != null) {
            if(oldLine.startsWith("#")) { // beginning of a new query
                int queryNumber = Integer.parseInt(oldLine.split(" ")[2]);
                StatisticsBox box = new StatisticsBox();
                box.queryNo = queryNumber;

                // find out if it is hit or miss
                String hit = line.split(",")[0];
                box.setHitOrMiss(hit);

                int resultSetSize = Integer.parseInt(line.split(",")[4]);
                box.resultSetSize = resultSetSize;

                box.query = queryList.get(queryNumber);
                list.add(box);

                oldLine = line;
            } else {
                oldLine = line;
            }
        }

        return list;
    }

    public static void main(String[] args) {
        CompareCompleteness execution = new CompareCompleteness();
        try {
            execution.compareCompleteness();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
