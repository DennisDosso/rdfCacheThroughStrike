package utils;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.atlas.lib.tuple.Tuple;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType;
import org.apache.jena.datatypes.xsd.impl.XSDDateType;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
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
import virtuoso.jena.driver.VirtGraph;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
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

    /**The indexes to be used with the DB. Default at spoc*/
    public static String indexes = "spoc";

    /** An rdf4j builder used to keep in RAM the triples of the cache to later use them to update the RDB */
    public static ModelBuilder creationModelBuilder;
    /** An rdf4j builder used to keep in RAM the triples to be removed from the cache*/
    public static ModelBuilder deletionModelBuilder;

    /** In-memory graph used to build the cache */
    public static Model virtuosoCreationModel;
    /** In-memory graph used to remove old triples from the cache */
    public static Model virtuosoDeletionModel;

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

    public static void initVirtuosoInMemoryGraph() {

        virtuosoCreationModel = ModelFactory.createDefaultModel();
    }

    public static void addTripleToCreation(String sub, String pred, String obj) {
        TripleStoreHandler.addTripleToBuilder(creationModelBuilder, sub, pred, obj);
    }

    public static void addTripleToCreationUsingVirtuoso(String sub, String pred, String obj) {
        TripleStoreHandler.addTripleToVirtuosoModel(virtuosoCreationModel, sub, pred, obj);
    }

    private static void addTripleToVirtuosoModel(Model m, String sub, String pred, String obj) {
        UrlValidator urlValidator = new UrlValidator();
        // DBPedia is always hard to work with
        if(!urlValidator.isValid(sub))
            sub = "http://dbpedia.org/node/" + sub;
        if(!urlValidator.isValid(pred))
            sub = "http://dbpedia.org/property/" + pred;

        if(urlValidator.isValid(obj)) {
            // todo controllare che funzi correttamente
            Resource subjNode = m.createResource(sub);
            Property p = m.createProperty(pred);
            Resource objNode = m.createResource(obj);
            m.add(subjNode, p, objNode);
        } else {
            // the could be a literal, or a "broken" IRI, one of those of Dbpedia
            //first let's try to see what we got
            String[] parts = StripObjectFromDatatype.stripObjectFromDatatype(obj);
            if(parts == null) {
                try{
                    // the object is a broken IRI since we were unable to find a datatype
                    Resource subjNode = m.createResource(sub);
                    Property p = m.createProperty(pred);
                    m.createResource(subjNode).addProperty(p, obj);
                } catch (ModelException e) {
                    // we do not do anything, this triple is simply lost
                    System.err.println("Raised model exception with triple "
                            + sub + " " + pred + " " + obj);
                }
            } else {
                // it is some form of literal

                    Resource subjNode = m.createResource(sub);
                    Property p = m.createProperty(pred);

                    if(parts[2].equals("plain")) { // generic case
                        Literal l = ResourceFactory.createPlainLiteral(parts[0]);
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("@")) { // language
                        Literal l = ResourceFactory.createLangLiteral(parts[0], parts[1]);
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("integer")) {
                        Literal l = ResourceFactory.createTypedLiteral(Integer.parseInt(parts[0]));
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("double")) {
                        Literal l = ResourceFactory.createTypedLiteral(Double.parseDouble(parts[0]));
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("float")) {
                        Literal l = ResourceFactory.createTypedLiteral(Float.parseFloat(parts[0]));
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("date")) {
                        Literal l = ResourceFactory.createTypedLiteral(parts[0], XSDDatatype.XSDdate);
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("dateTime")) {
                        Literal l = ResourceFactory.createTypedLiteral(parts[0], XSDDatatype.XSDdateTime);
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("nonNegativeInteger")) {
                        Literal l = ResourceFactory.createTypedLiteral(parts[0], XSDDatatype.XSDnonNegativeInteger);
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("gYear")) {
                        Literal l = ResourceFactory.createTypedLiteral(parts[0], XSDDatatype.XSDgYear);
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("gMonthDay")) {
                        Literal l = ResourceFactory.createTypedLiteral(parts[0], XSDDatatype.XSDgMonthDay);
                        m.add(subjNode, p, l);
                    }

                    if(parts[2].equals("custom")) {// custom datatype from dbpedia
                        // create datatype
                        RDFDatatype custom = new XSDBaseStringType(parts[3] + parts[4]);
                        Literal l = ResourceFactory.createTypedLiteral(parts[0], custom);
                        m.add(subjNode, p, l);

                    }
                    if(parts[2].equals("XMLSchema")) {
                        RDFDatatype custom = new XSDBaseStringType(parts[3] + parts[4]);
                        Literal l = ResourceFactory.createTypedLiteral(parts[0], custom);
                        m.add(subjNode, p, l);

                    }
                if(parts[2].equals("openlinksw")) {
                    RDFDatatype custom = new XSDBaseStringType(parts[3] + "#" + parts[4]);
                    Literal l = ResourceFactory.createTypedLiteral(parts[0], custom);
                    m.add(subjNode, p, l);

                }
                    if(parts[2].equals("unknown")) {
                        System.out.println("[WARNING] this triple has a special datatype, thus is added " +
                                "as simple literal: " +
                                sub + " " + pred + " " + obj);

                        Literal l = ResourceFactory.createPlainLiteral(obj.replaceAll("\"", ""));
                        m.add(subjNode, p, l);
                    }
                }

        }// end of the creation of the triple
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

    public static void addTriplesToVirtuosoCache(VirtGraph cache) {
        // iterate through the triples in the in-memory cache
        Graph g = virtuosoCreationModel.getGraph();
        for (Iterator i = g.find(Node.ANY, Node.ANY, Node.ANY); i.hasNext();) {
            Triple t = (Triple)i.next(); // get one triple and add it to the lineage
            cache.add(t);
        }
        virtuosoCreationModel.removeAll(); // clear the model, we are done
    }

    /** Creates the builder that will keep in RAM the triples that later we will delete
     * from the in-disk cache
     *
     * */
    public static void initDeletion() {
        deletionModelBuilder = new ModelBuilder().setNamespace("n", ProjectValues.namedGraphName);
    }

    public static void initDeletionWithVirtuoso() {
        virtuosoDeletionModel = ModelFactory.createDefaultModel();
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

    public static void removeTriplesFromCacheWithVirtuoso(VirtGraph cache) {
        // iterate through the triples in the in-memory cache
        Graph g = virtuosoDeletionModel.getGraph();
        for (Iterator i = g.find(Node.ANY, Node.ANY, Node.ANY); i.hasNext();) {
            Triple t = (Triple)i.next(); // get one triple and add it to the lineage
            cache.remove(t);
        }
        virtuosoDeletionModel.removeAll(); // clear the model, we are done
    }

    /** Given three strings representing a triple, it adds this triple to a builder in RAM.
     * This builder will be used later to remove the triples it contains from the cache
     *
     * */
    public static void addTripleToDeletion(String sub, String pred, String obj) {
        TripleStoreHandler.addTripleToBuilder(deletionModelBuilder, sub, pred, obj);
    }


    public static void addTripleToDeletionUsingVirtuoso(String sub, String pred, String obj) {
        TripleStoreHandler.addTripleToVirtuosoModel(virtuosoDeletionModel, sub, pred, obj);
    }

}
