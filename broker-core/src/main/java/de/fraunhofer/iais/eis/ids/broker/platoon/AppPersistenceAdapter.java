package de.fraunhofer.iais.eis.ids.broker.platoon;

import de.fraunhofer.iais.eis.AppResource;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.connector.commons.broker.QueryResultsProvider;

import java.io.IOException;
import java.net.URI;

public class AppPersistenceAdapter implements AppStatusHandler, QueryResultsProvider {

    @Override
    public void unavailable(URI resourceUri, URI connectorUri) throws IOException, RejectMessageException {

    }

    @Override
    public URI updated(AppResource dataApp, URI connectorUri) throws IOException, RejectMessageException {
        return null;
    }

    @Override
    public boolean resourceExists(URI resourceUri) throws RejectMessageException {
        return false;
    }

    @Override
    public String getResults(String query) throws RejectMessageException {
        return null;
    }
}
