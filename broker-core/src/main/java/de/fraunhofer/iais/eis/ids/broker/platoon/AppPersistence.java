package de.fraunhofer.iais.eis.ids.broker.platoon;

import de.fraunhofer.iais.eis.AppResource;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.InfrastructureComponent;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.ids.broker.core.common.persistence.ResourceModelCreator;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.index.common.persistence.GenericQueryEvaluator;
import de.fraunhofer.iais.eis.ids.index.common.persistence.JsonLdContextFetchStrategy;
import de.fraunhofer.iais.eis.ids.index.common.persistence.NullIndexing;
import de.fraunhofer.iais.eis.ids.index.common.persistence.RepositoryFacade;
import de.fraunhofer.iais.eis.ids.index.common.persistence.spi.Indexing;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.ARQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//public class AppPersistence {
//}

/**
 * This class takes care of persisting and indexing any changes to apps that are announced to the broker
 */
public class AppPersistence extends AppPersistenceAdapter {
    private final ResourceModelCreator resourceModelCreator = new ResourceModelCreator();

    private static RepositoryFacade repositoryFacade;
    private  int maxNumberOfIndexedConnectorResources;

    private Indexing<InfrastructureComponent> indexing = new NullIndexing<>();
    private final URI componentCatalogUri;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * Constructor
     * @param repositoryFacade repository (triple store) to which the modifications should be stored
     * @param maxNumberOfIndexedConnectorResources
     */
    public AppPersistence(RepositoryFacade repositoryFacade, URI componentCatalogUri, int maxNumberOfIndexedConnectorResources) {
        AppPersistence.repositoryFacade = repositoryFacade;
        this.componentCatalogUri = componentCatalogUri;
        this.maxNumberOfIndexedConnectorResources = maxNumberOfIndexedConnectorResources;
        Serializer.addKnownNamespace("owl", "http://www.w3.org/2002/07/owl#");
    }

    /**
     * Setter for the indexing method
     * @param indexing indexing to be used
     */
    public void setIndexing(Indexing<InfrastructureComponent> indexing) {
        this.indexing = indexing;
    }

    /**
     * Setter for the context document URL. Typically extracted from the application.properties
     * @param contextDocumentUrl the context document URL to be used
     */
    public void setContextDocumentUrl(String contextDocumentUrl) {
        resourceModelCreator.setContextFetchStrategy(JsonLdContextFetchStrategy.FROM_URL, contextDocumentUrl);
    }

    /**
     * Function to obtain the catalog URI of a given connector. This is required when the connector makes new resources available or modifies existing ones
     * @param connectorUri The URI of the connector
     * @return The URI of the IDS Catalog of the connector in question
     * @throws RejectMessageException if the URI could not be retrieved, e.g. because the connector is not known, is inactive or has been deleted
     * @throws URISyntaxException if the URI of the catalog is malformed
     */
    private URI getConnectorCatalog(URI connectorUri) throws RejectMessageException, URISyntaxException {

        //Launch query
        String queryString = "PREFIX ids: <https://w3id.org/idsa/core/> " +
                //Grab URI of catalog containing the resource
                "SELECT DISTINCT ?catalog " +
                //Search is restricted to active graphs by default. This is done within the getResults function (calling SparqlQueryRewriter)
//        repositoryFacade.getActiveGraphs().forEach(graphName -> queryString.append("FROM NAMED <").append(graphName).append("> "));
                //Bind named graph to restrict search to single named graph, which must be, due to the FROM NAMED section above, active
                "WHERE { BIND(<" + connectorUri.toString() + "> AS ?connector) . " +
                //Only interested in the catalog part. The connector is contained in a named graph, which has the connector's ID as name
                "GRAPH ?connector { ?connector ids:resourceCatalog ?catalog . } }";
        String catalogString = getResults(queryString);
        //Are the results equal to the binding name "?catalog" ? If so, no such catalog was found
        if(catalogString.equals("?catalog\n"))
        {
            throw new RejectMessageException(RejectionReason.NOT_FOUND, new NullPointerException("Catalog of connector " + connectorUri + " not found. Did you send a ConnectorAvailableMessage yet?"));
        }
        //TODO: What about multiple catalogs?
        return new URI(catalogString.substring(catalogString.indexOf("<") + 1, catalogString.indexOf(">")));
    }

    /**
     * Function to query the repository whether a resource with a given URI is known to the broker
     * @param resourceUri The URI of the resource to be tested
     * @return true, if the resource is known to the broker AND the connector holding the resource is non-passivated and non-deleted
     * @throws RejectMessageException if an internal error occurs
     */
    @Override
    public boolean resourceExists(URI resourceUri) throws RejectMessageException {
        try {
            List<String> activeGraphs = repositoryFacade.getActiveGraphs();
            if(activeGraphs.isEmpty()) return false;


            StringBuilder queryString = new StringBuilder();
            queryString.append("PREFIX ids: <https://w3id.org/idsa/core/> ");
            queryString.append("ASK ");
            activeGraphs.forEach(graphName -> queryString.append("FROM NAMED <").append(graphName).append("> "));
            queryString.append("WHERE { GRAPH ?g { ")
                    //Instead of binding the value here, use the more secure parameter binding of parameterized sparql strings
                    //.append("BIND(<").append(resourceUri.toString()).append("> AS ?res) . ")

                    //Check done if res is a Appresource instead of Resource
                    .append("{ ?res a ids:Resource . }"
                            + " UNION { ?res a ids:TextResource . }"
                            + " UNION { ?res a ids:AppResource . }"
                            + " UNION { ?res a ids:DataResource . } \n"
                            + "  UNION { ?res a ids:AudioResource . } \n"
                            + "  UNION { ?res a ids:ImageResource . } \n"
                            + "  UNION { ?res a ids:VideoResource . } \n"
                            + "  UNION { ?res a ids:SoftwareResource . }").append("?res ?p ?o . } }");

            ParameterizedSparqlString parameterizedSparqlString = new ParameterizedSparqlString(queryString.toString());
            //Replace variable securely
            parameterizedSparqlString.setIri("res", resourceUri.toString());

            return repositoryFacade.booleanQuery(parameterizedSparqlString.toString());
        }
        catch (Exception e)
        {
            //If an injection attack is detected, an ARQException will be thrown, see:
            // https://jena.apache.org/documentation/query/parameterized-sparql-strings.html
            if(e instanceof ARQException)
            {
                logger.warn("Possible injection attack detected", e);
                //Do not provide any further information to the attacker
                throw new RejectMessageException(RejectionReason.MALFORMED_MESSAGE);
            }
            throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, e);
        }
    }



    /**
     * Function to persist and index modifications to an existing resource
     * @param app The updated resource which was announced to the broker
     * @param connectorUri The connector which is offering the resource
     * @throws IOException thrown, if the connection to the repository could not be established
     * @throws RejectMessageException thrown, if the update is not permitted, e.g. because the resource of an inactive connector is modified, or if an internal error occurs
     */
    @Override
    public URI updated(AppResource app, URI connectorUri) throws IOException, RejectMessageException {
        URI catalogUri;
        try {
            //Check if the connectorURI is rewritten already. If not, rewrite now
            if(!connectorUri.toString().startsWith(componentCatalogUri.toString()))
                connectorUri = AppDescriptionPersistence.rewriteConnectorUri(connectorUri);
            catalogUri = getConnectorCatalog(connectorUri);


            //Rewrite resource
            AppDescriptionPersistence.replacedIds = new HashMap<>(); //Clear the map tracking all URIs that were replaced
            app = new Serializer().deserialize( //Parse to Java Class

                    AppDescriptionPersistence.addSameAsStatements( //Add owl:sameAs statements for all URIs we are replacing
                            AppDescriptionPersistence.rewriteResource(app.toRdf(),app , catalogUri)), //Replace URIs
                    AppResource.class); //Result of parsing should be a Resource
        } catch (URISyntaxException e) {
            throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, e);
        }
        if (!repositoryFacade.graphIsActive(connectorUri.toString())) {
            connectorUri = URI.create(componentCatalogUri.toString() + connectorUri.hashCode());
            if (!repositoryFacade.graphIsActive(connectorUri.toString())) {
                throw new RejectMessageException(RejectionReason.NOT_FOUND, new NullPointerException("The connector with URI " + connectorUri + " is not actively registered at this broker. Cannot update resource for this connector."));
            }
        }

        logger.info("Rewrote resource. New URI: " + app.getId().toString());

        //Try to remove the resource from Triple Store if it exists, so that it is updated properly.
        if (resourceExists(app.getId())) {
            logger.info("Resource already exists. Removing");
            removeFromTriplestore(app.getId(), connectorUri);
        }

        try {
            addToTriplestore(app, connectorUri, catalogUri);
        } catch (URISyntaxException e) {
            throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, e);
        }
        Connector connector = repositoryFacade.getReducedConnector(connectorUri, maxNumberOfIndexedConnectorResources);
        indexing.update(connector);

        indexing.updateResource(connector, app);
        //Return the updated resource URI
        return app.getId();
    }

    static URI tryGetRewrittenResourceUri(URI connectorUri, URI resourceUri) throws RejectMessageException {
        //Cannot do this as parameterised SPARQL query, as the connector URI is not bound to a variable, but to the FROM clause instead
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String queryString = "PREFIX ids: <https://w3id.org/idsa/core/> SELECT ?uri FROM NAMED <" + connectorUri.toString() + "> WHERE { GRAPH ?g { ?uri a ids:AppResource . FILTER regex( str(?uri), \"" + resourceUri.hashCode() + "\" ) } } ";
        ArrayList<QuerySolution> solution = repositoryFacade.selectQuery(queryString);
        if(solution != null && !solution.isEmpty())
        {
            return URI.create(solution.get(0).get("uri").asResource().getURI());
        }
        throw new RejectMessageException(RejectionReason.NOT_FOUND, new NullPointerException("The requested Resource could not be found"));
    }



    /**
     * Function to remove a given Resource from the indexing and the triple store
     * @param resourceUri A URI reference to the resource which is now unavailable
     * @param connectorUri The connector which used to offer the resource
     * @throws IOException if the connection to the triple store could not be established
     * @throws RejectMessageException if the operation is not permitted, e.g. because one is trying to delete the resource from another connector, the resource is not known or due to an internal error
     */
    //TODO: ResourceUnavailableValidationStrategy? Need to make sure that one cannot delete another connector's resources
    @Override
    public void unavailable(URI resourceUri, URI connectorUri) throws IOException, RejectMessageException {
        if(!repositoryFacade.graphIsActive(connectorUri.toString()))
        {
            connectorUri = URI.create(componentCatalogUri.toString() + connectorUri.hashCode());
        }
        if(!repositoryFacade.graphIsActive(connectorUri.toString()))
        {
            throw new RejectMessageException(RejectionReason.NOT_FOUND, new NullPointerException("The connector from which you are trying to sign off a App was not found or is not active."));
        }
        if(!resourceExists(resourceUri)) {
            resourceUri = tryGetRewrittenResourceUri(connectorUri, resourceUri);
        }
        removeFromTriplestore(resourceUri, connectorUri);
        indexing.update(repositoryFacade.getConnectorFromTripleStore(connectorUri));

        indexing.delete(resourceUri);
    }

    /**
     * Utility function to evaluate a given query (in a re-formulated way, respecting passivation and hiding underlying structure of named graphs)
     * @param queryString Query to be evaluated
     * @return Query result in String format
     * @throws RejectMessageException, if the query is illegal, e.g. because it accesses a graph which is not active
     */
    @Override
    public String getResults(String queryString) throws RejectMessageException {
        return new GenericQueryEvaluator(repositoryFacade).getResults(queryString);
    }

    /**
     * Internal function which should only be called from the available function. It applies the changes to the triple store
     * @param resource Resource to be added to triple store
     * @param connectorUri Connector to which the resource should be added
     * @param catalogUri The URI of the catalog of the connector to which the resource should be added
     * @throws IOException thrown, if the changes could not be applied to the triple store
     * @throws RejectMessageException thrown, if the changes are illegal, or if an internal error has occurred
     */
    private void addToTriplestore(AppResource resource, URI connectorUri, URI catalogUri) throws IOException, RejectMessageException, URISyntaxException {

        ResourceModelCreator.InnerModel result = resourceModelCreator.setConnectorUri(connectorUri).toModel(AppDescriptionPersistence.rewriteResource(resource.toRdf(), resource, catalogUri));

        //Add a statement that this Resource is part of some catalog
        //?catalog ids:offeredResource ?resource
        //subject, predicate and object of the triple
        Model m = result.getModel();
        m.add(ResourceFactory.createResource(catalogUri.toString()), ResourceFactory.createProperty("https://w3id.org/idsa/core/offeredResource"), ResourceFactory.createResource(resource.getId().toString()));
        repositoryFacade.addStatements(result.getModel(), result.getNamedGraph().toString());
    }


    /**
     * Internal function which should only be called from the unavailable function. It applies the changes to the triple store
     * @param resourceUri Resource to be removed from triple store
     * @param connectorUri Connector to which the resource should be added
     * @throws RejectMessageException thrown, if the changes are illegal, or if an internal error has occurred
     */
    private void removeFromTriplestore(URI resourceUri, URI connectorUri) throws RejectMessageException {
        //Make sure no passivated graph is updated via ResourceXXXMessage
        if(!repositoryFacade.graphIsActive(connectorUri.toString())) {
            connectorUri = URI.create(componentCatalogUri.toString() + connectorUri.hashCode());
            if (!repositoryFacade.graphIsActive(connectorUri.toString())) {
                throw new RejectMessageException(RejectionReason.NOT_FOUND, new Exception("The appresource you are trying to delete was not found, or the graph owning the resource is not active (i.e. unavailable)."));
            }
            //At this stage, we need to rewrite the URI of the resource to our REST-like scheme
            //resourceUri = tryGetRewrittenResourceUri(connectorUri, resourceUri);
        }
        //Grab "all" information about a Resource. This includes everything pointing at a resource as well as all child objects of a resource, up to a (rather arbitrary) depth of 7
        ParameterizedSparqlString queryString = new ParameterizedSparqlString("CONSTRUCT { ?res ?p ?o . ?o ?p2 ?o2 . ?o2 ?p3 ?o3 . ?o3 ?p4 ?o4 . ?o4 ?p5 ?o5 . ?o5 ?p6 ?o6 . ?o6 ?p7 ?o7 . ?s ?p ?res . } " +
                "WHERE { " +
                //We already ensured that this graph is active
                //"BIND(<" + connectorUri.toString() + "> AS ?g) . " +
                //"BIND(<" + resourceUri.toString() + "> AS ?res) . " +
                "GRAPH ?g { { ?res ?p ?o . OPTIONAL { ?o ?p2 ?o2 . OPTIONAL { ?o2 ?p3 ?o3 . OPTIONAL { ?o3 ?p4 ?o4 . OPTIONAL { ?o4 ?p5 ?o5 . OPTIONAL { ?o5 ?p6 ?o6 . OPTIONAL { ?o6 ?p7 ?o7 . } } } } } } } " +
                "UNION " +
                "{ ?s ?p ?res . }" +
                "} }");
        queryString.setIri("g", connectorUri.toString());
        queryString.setIri("res", resourceUri.toString());
        try {
            Model graphQueryResult = repositoryFacade.constructQuery(queryString.toString());
            if(graphQueryResult.isEmpty())
            {
                throw new RejectMessageException(RejectionReason.NOT_FOUND, new NullPointerException("The appresource you are trying to update or remove was not found. Try sending a ResourceAvailableMessage instead."));
            }
            //Dump the result into a format over which we can iterate multiple times
            ArrayList<Statement> graphQueryResultAsList = new ArrayList<>();
            StmtIterator iterator = graphQueryResult.listStatements();
            while(iterator.hasNext())
            {
                graphQueryResultAsList.add(iterator.next());
            }
            repositoryFacade.removeStatements(graphQueryResultAsList, connectorUri.toString());
        }
        //Check if injection attack was detected
        catch (ARQException e)
        {
            logger.warn("Possible injection attack detected", e);
            //Do not provide any exception information to the potential attacker
            throw new RejectMessageException(RejectionReason.MALFORMED_MESSAGE);
        }

    }
}