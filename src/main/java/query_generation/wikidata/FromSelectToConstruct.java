package query_generation.wikidata;

import properties.ProjectPaths;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Script to create construct queries from the select ones of wikidata*/
public class FromSelectToConstruct {

    public static void main(String[] args) {
        ProjectPaths.init();

        try(BufferedReader reader = Files.newBufferedReader(Paths.get(ProjectPaths.selectQueryFile));
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(ProjectPaths.constructQueryFile))) {

            String line = "";
            String constructQuery = "";
            int counter = 0;

            while((line = reader.readLine()) != null)  {
//                if(counter > 100000)
//                    break;
                counter++;
                if(counter % 1000 == 0)
                    System.out.println("printer " + counter + " queries");
                constructQuery = "";
                // first, get the prefixes - if there are some
                String[] parts = line.split("SELECT|select");
                String prefixes = "";
                if(parts.length>1) {
                    //prefixes may be absent, only if we have 2 elements it is ok to have them
                    prefixes = parts[0];
                    constructQuery += prefixes;
                }

                //extrapolate the whole pattern inside the WHERE clause
                String regex = "(WHERE|where)( |\t)*\\{(.+)}";
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(line);
                String pattern = "";
                if(m.find()){
                    pattern = m.group(3);
                }

                // extrapolate the pattern before OPTIONAL and filter
                regex = "(.+?)(?=OPTIONAL|optional|FILTER|filter)";
                p = Pattern.compile(regex);
                m = p.matcher(pattern);
                String mainBody;
                if(m.find()){
                    mainBody = m.group(1);
                } else {
                    mainBody = pattern;
                }
                mainBody = mainBody.trim();
                if(!mainBody.endsWith(".")) {
                    mainBody += ".";
                }

                // We count how many '.' we have in the mainBody. This gives us an indication of how many triples we have
                long count = mainBody.chars().filter(ch -> ch == '.').count()/3;

                //extrapolate the pattern of OPTIONAL
                regex = "(OPTIONAL|optional) ?\\{([^}]*)}";
                p = Pattern.compile(regex);
                m = p.matcher(pattern);
                String optionalPattern = "";
                while(m.find()) {
                    String optPattern = m.group(2);
                    optionalPattern += " " + optPattern;
                    count++;
                }

                // find the offset if present
                regex = "(OFFSET|offset)( *)([0-9]+)";
                p = Pattern.compile(regex);
                m = p.matcher(line);
                String offset = "";
                if(m.find()) {
                    offset = m.group(3).trim();
                }

                //last, find the limit if present
                regex = "(LIMIT|limit)(.*)";
                p = Pattern.compile(regex);
                m = p.matcher(line);
                int limit = -1;
                if(m.find()) {
                    try{
                        limit = Integer.parseInt(m.group(2).trim());
                    } catch ( NumberFormatException e) {

                    }
                }

                // now build the CONSTRUCT query
                constructQuery += " CONSTRUCT { " + mainBody + " " + optionalPattern +
                        " } WHERE { " + pattern + "} ";
                if(!offset.equals("")) {
                    constructQuery += "OFFSET " + offset;
                }

                if(limit != -1) {
                    constructQuery += " LIMIT " + limit*count;
                } else {
                    // no limit was provided. I decide to put a limit to contain the complexity
                    constructQuery += " LIMIT " + 10*count;
                }

                writer.write(constructQuery);
                writer.newLine();
                writer.flush();
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
