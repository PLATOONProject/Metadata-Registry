package de.fraunhofer.iais.eis.ids.broker.handler;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.broker.core.common.persistence.RegistrationHandler;
import de.fraunhofer.iais.eis.ids.connector.commons.app.map.AppMAP;
import de.fraunhofer.iais.eis.ids.broker.platoon.AppMessageHandler;
import de.fraunhofer.iais.eis.ids.broker.platoon.AppStatusHandler;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.component.core.SecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.connector.commons.broker.InfrastructureComponentStatusHandler;
import de.fraunhofer.iais.eis.ids.connector.commons.broker.map.InfrastructureComponentMAP;
import de.fraunhofer.iais.eis.ids.index.common.persistence.RepositoryFacade;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import de.fraunhofer.iais.eis.util.Util;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.fraunhofer.iais.eis.util.Util.asList;

@Ignore
public class AppRegistrationHandlerTest {

    private final URI dummyUri = new URI("https:example.org/dummy");
    // private AppResourceBuilder app = new AppResourceBuilder();
    private AppResourceBuilder dataApp = new AppResourceBuilder(dummyUri);
    private final InfrastructureComponent app = new BaseConnectorBuilder(dummyUri)
            ._title_(new ArrayList<>(asList(new TypedLiteral("App", "en"))))
            ._curator_(dummyUri)
            ._maintainer_(dummyUri)
            ._outboundModelVersion_("3.0.0")
            ._inboundModelVersion_(asList("3.0.0"))
            ._resourceCatalog_(asList(new ResourceCatalogBuilder().build()))
            ._securityProfile_(SecurityProfile.BASE_SECURITY_PROFILE)
            .build();
    private final SecurityTokenProvider securityTokenProvider = new SecurityTokenProvider() {
        @Override
        public String getSecurityToken() {
            return "test1234";
        }
    };
    private final URI senderAgent = new URI("http:example.org/");

    private final InfrastructureComponent broker = new BrokerBuilder()
            ._title_(asList(new TypedLiteral("EIS Broker", "en")))
            ._description_(asList(new TypedLiteral("A semantic impl for demonstration purposes", "en")))
            ._maintainer_(dummyUri)
            ._curator_(dummyUri)
            ._inboundModelVersion_(Util.asList("3.0.0"))
            ._outboundModelVersion_("3.0.0")
            ._resourceCatalog_(asList(new ResourceCatalogBuilder().build()))
            ._securityProfile_(SecurityProfile.BASE_SECURITY_PROFILE)
            .build();


    private final Message appAvailable = new AppAvailableMessageBuilder()
            ._issued_(CalendarUtil.now())
            ._modelVersion_("3.0.0")
            ._issuerConnector_(dummyUri)
            ._affectedResource_(dummyUri)
            ._securityToken_(securityTokenProvider.getSecurityTokenAsDAT())
            ._senderAgent_(senderAgent)
            .build();

    private final Message appUnavailable = new AppUnavailableMessageBuilder()
            ._issued_(CalendarUtil.now())
            ._modelVersion_("3.0.0")
            ._issuerConnector_(app.getId())
            ._affectedResource_(app.getId())
            ._securityToken_(securityTokenProvider.getSecurityTokenAsDAT())
            ._senderAgent_(senderAgent)
            .build();

    public AppRegistrationHandlerTest() throws TokenRetrievalException, URISyntaxException {
    }

    @Test
    public void handleRegister() throws RejectMessageException, URISyntaxException {
        AppMessageHandler appRegistrationHandler = new AppMessageHandler(new AppStatusHandler() {

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



        }, broker, new SecurityTokenProvider() {
            @Override
            public String getSecurityToken() {
                return "test1234";
            }
        }, new RepositoryFacade(), new URI("http:example.org/"));
        appRegistrationHandler.handle(new AppMAP(appAvailable, dataApp.build()));
    }

    @Test
    public void handleUnregister() throws RejectMessageException, URISyntaxException {
        RegistrationHandler registrationHandler = new RegistrationHandler(new InfrastructureComponentStatusHandler() {

            @Override
            public void unavailable(URI issuerConnector) {
                Assert.assertEquals(issuerConnector, appUnavailable.getIssuerConnector());
            }

            @Override
            public URI updated(InfrastructureComponent selfDescription) {
                Assert.fail();
                return null;
            }

        }, broker, new SecurityTokenProvider() {
            @Override
            public String getSecurityToken() {
                return "test1234";
            }
        }, new RepositoryFacade(), new URI("http:example.org/"));

        registrationHandler.handle(new InfrastructureComponentMAP(appUnavailable));
    }


    @Test
    public void handleUpdate() throws RejectMessageException, URISyntaxException {
        RegistrationHandler registrationHandler = new RegistrationHandler(new InfrastructureComponentStatusHandler() {

            @Override
            public void unavailable(URI issuerConnector) {
                Assert.fail();
            }

            @Override
            public URI updated(InfrastructureComponent selfDescription) {
                Assert.assertEquals(app, selfDescription);
                return null;
            }

        }, broker, new SecurityTokenProvider() {
            @Override
            public String getSecurityToken() {
                return "test1234";
            }
        }, new RepositoryFacade(), new URI("http:example.org/"));

        registrationHandler.handle(new InfrastructureComponentMAP(appAvailable, app));
    }

  //  TODO: Add test for passivation!



}
