package utils;

import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import properties.ProjectValues;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.iri;

/**
 * Role of this class is to maintain the connection to an rdf4j index, perform queries etc.
 * Each connection need to have a string name, e.g. "DB" or "CACHE", so you can deal with
 * multiple singleton connections (e.g. when we need to connect both to the cache AND the
 * full database)
 *
 * */
public class TripleStoreHandler {

    public static final String CACHE = "cache";
    public static final String DB = "db";

    /** Triple store repository */
    public static Map<String, Repository> repositoryMap = new HashMap<>();
    public static Map<String, RepositoryConnection> connectionMap = new HashMap<>();

    /**The indexes to be used with the DB. Default at spo*/
    public static String indexes = "spoc";

    /** An rdf4j builder used to keep in RAM the triples of the cache to later use them to update the RDB */
    public static ModelBuilder creationModelBuilder;
    /** An rdf4j builder used to keep in RAM the triples to be removed from the cache*/
    public static ModelBuilder deletionModelBuilder;

    /** Opens a connection to a repository and associates it to its repositoryName.
     * If a repositoryName already exists, it
     * simply returns it.
     * */
    public static RepositoryConnection getConnection(String repositoryPath, String repositoryName) {
        Repository repo = repositoryMap.get(repositoryName);
        if(repo == null) {
            // user requested a new connection. Open it and add it to the map of available connections
            File dataDir = new File(repositoryPath);
            repo = new SailRepository(new NativeStore(dataDir, ProjectValues.indexes));
            repo.init();
            repositoryMap.put(repositoryName, repo);

            RepositoryConnection c = repo.getConnection();
            connectionMap.put(repositoryName, c);
            return c;
        } else {
            // we already have a connection with that name. Return it to the user
            return connectionMap.get(repositoryName);
        }
    }

    public static void closeConnection(String connectionName) {
        RepositoryConnection c = connectionMap.get(connectionName);
        if(c != null && c.isOpen()) {
            c.close();
            connectionMap.remove(connectionName);
        }

        Repository p = repositoryMap.get(connectionName);
        if(p!= null){
            p.shutDown();
            repositoryMap.remove(connectionName);
        }
    }

    public static void initModelBuilder() {
        creationModelBuilder = new ModelBuilder().setNamespace("n", ProjectValues.namedGraphName);
    }

    public static void addTripleToCreation(String sub, String pred, String obj) {
        TripleStoreHandler.addTripleToBuilder(creationModelBuilder, sub, pred, obj);
    }

    private static void addTripleToBuilder(ModelBuilder builder, String sub, String pred, String obj) {
        UrlValidator urlValidator = new UrlValidator();
        ValueFactory vf = SimpleValueFactory.getInstance();
        // DBPedia is always a bad girl
        if(!urlValidator.isValid(sub))
            sub = "http://dbpedia.org/node/" + sub;
        if(!urlValidator.isValid(pred))
            sub = "http://dbpedia.org/property/" + pred;

        if(urlValidator.isValid(obj)) {
            try{
                IRI o = iri(obj);
                builder.subject(sub).add(pred, o);
            } catch(IllegalArgumentException iae) {
                System.err.println("Raised illegal argument exception with triple "
                        + sub + " " + pred + " " + obj);
            } catch (ModelException e) {
                System.err.println("Raised model exception with triple "
                        + sub + " " + pred + " " + obj);
            }
        } else {
            // the could be a literal, or a "broken" IRI, one of those of Dbpedia
            //first let's try to see what we got
            String[] parts = StripObjectFromDatatype.stripObjectFromDatatype(obj);
            if(parts == null) {
                try{
                    // the object is a broken IRI since we were unable to find a datatype
                    builder.subject(sub).add(pred, obj);
                } catch (ModelException e) {
                    // we do not do anything, this triple is simply lost
                    System.err.println("Raised model exception with triple "
                            + sub + " " + pred + " " + obj);
                }
            } else {
                // it is some form of literal
                try{
                    if(parts[2].equals("plain"))
                        builder.subject(sub).add(pred, parts[0]);

                    if(parts[2].equals("@"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], parts[1]));

                    if(parts[2].equals("integer"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], XSD.INTEGER));
                    if(parts[2].equals("double"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], XSD.DOUBLE));
                    if(parts[2].equals("float"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], XSD.FLOAT));
                    if(parts[2].equals("date"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], XSD.DATE));
                    if(parts[2].equals("dateTime"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], XSD.DATETIME));
                    if(parts[2].equals("nonNegativeInteger"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], XSD.NON_NEGATIVE_INTEGER));
                    if(parts[2].equals("gYear"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], XSD.GYEAR));
                    if(parts[2].equals("gMonthDay"))
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], XSD.GMONTHDAY));
                    if(parts[2].equals("custom")) {// custom datatype from dbpedia
                        CustomURI u = new CustomURI(parts[3], parts[4]);
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], u));
                    }
                    if(parts[2].equals("XMLSchema")) {
                        CustomURI u = new CustomURI(parts[3], parts[4]);
                        builder.subject(sub).add(pred, vf.createLiteral(parts[0], u));
                    }
                    if(parts[2].equals("unknown")) {
                        System.out.println("[WARNING] this triple has a special datatype, thus is added " +
                                "as simple literal: " +
                                sub + " " + pred + " " + obj);
                        builder.subject(sub).add(pred, obj.replaceAll("\"", ""));
                    }
                } catch(ModelException e) {
                    System.err.println("error in inserting literal " + obj + " inserting it as general literal");
                    try{
                        builder.subject(sub).add(pred, obj.replaceAll("\"", ""));
                    } catch (Exception e2) {
                        System.err.println("Truly impossible to import " + sub + " " + pred + " " + obj);
                        e.printStackTrace();
                        e2.printStackTrace();
                    }
                }
            }
        }// end of the creation of the triple
    }

    /** The connection name is the type of connection to where we want to
     * save the triples, e.g. the main db ("DB") or the cache ("CACHE")*/
    public static void addTriplesFromCreationBuilderToThisConnection(String connectionName) {
        connectionMap.get(connectionName).add(creationModelBuilder.build());
        connectionMap.get(connectionName).commit();
    }

    /** Creates the builder that will keep in RAM the triples that later we will delete
     * from the in-disk cache
     *
     * */
    public static void initDeletion() {
        deletionModelBuilder = new ModelBuilder().setNamespace("n", ProjectValues.namedGraphName);
    }

    public static void removeTriplesFromCacheUsingDeletionBuilder() {
        try{
            connectionMap.get(CACHE).remove(deletionModelBuilder.build());
            connectionMap.get(CACHE).commit();
        } catch (RepositoryException e) {
            System.out.println("Repository Exception");
        } catch (Exception e1) {
            System.out.println("Strange exception when deleting");
            e1.printStackTrace();
        }
    }

    /** Given three strings representing a triple, it adds this triple to a builder in RAM.
     * This builder will be used later to remove the triples it contains from the cache
     *
     * */
    public static void addTripleToDeletion(String sub, String pred, String obj) {
        TripleStoreHandler.addTripleToBuilder(deletionModelBuilder, sub, pred, obj);
    }

}
