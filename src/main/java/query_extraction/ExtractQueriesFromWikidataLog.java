package query_extraction;

import properties.ProjectPaths;
import properties.ProjectValues;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** The query logs which we are dealing with here are taken from
 * https://iccl.inf.tu-dresden.de/web/Wikidata_SPARQL_Logs/en
 *
 * */
public class ExtractQueriesFromWikidataLog {

    public static void main(String[] args) {
        ProjectPaths.init();
        int count = 0;

        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.wikidataLogFilePath));
            BufferedWriter w = Files.newBufferedWriter(Paths.get(ProjectPaths.wikidataAmendedLogFilePath))) {
            r.readLine(); // first line is made of headers
            String line = "";
            while((line = r.readLine()) != null) {
                String[] parts = line.split("\t");
                String query = parts[0];
                String timestamp = parts[1];
                String queryType = parts[2];
                String userAgent = "";
                try {
                    userAgent = parts[3];
                } catch (IndexOutOfBoundsException e) {
                    // do nothing
                }

                // decode the query
                query = java.net.URLDecoder.decode(query, StandardCharsets.UTF_8.name());
                query = query.replaceAll("\n", " ");

                if(checkQueryQuality(query, queryType)) {
//                    System.out.println(query);
                    query = checkQueryLimit(query);

                    w.write(query);
                    w.newLine();
                    count++;
                }
            }
            w.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("valid queries " + count);

    }

    private static String checkQueryLimit(String query) {
        if(query.contains("LIMIT"))
            return query;

        if(query.contains("limit"))
            return query;

        return query += " LIMIT 1000";
    }

    private static boolean checkQueryQuality(String query, String queryType) {
        if(!queryType.equals("robotic")) // if it is not a synthetic query, return false
            return false;

        if(query.contains("ASK"))
            return false;

        if(query.contains("UNION"))
            return false;

        if(query.contains("SERVICE"))
            return false;

        if(query.contains("MINUS"))
            return false;

        if(query.contains("describe"))
            return false;

        if(query.contains("Describe"))
            return false;

        if(query.contains("BIND"))
            return false;

        if(query.contains("BOUND"))
            return false;

        if(query.contains("/*"))
            return false;

        if(query.contains("*/"))
            return false;

        if(query.matches("(.*)WHERE(.*)\\*(.*)"))
            return false;

        if(query.contains("SUBSTR"))
            return false;

        if(query.contains("STRAFTER"))
            return false;

        if(query.contains("VALUES"))
            return false;


        return true;
    }
}
