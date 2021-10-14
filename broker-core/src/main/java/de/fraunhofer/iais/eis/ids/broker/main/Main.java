/*
 * Copyright (c) 2021 Fraunhofer IAIS.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the
 * Apache License Version 2.0 which accompanies
 * this distribution, and is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *
 * Hantong Liu IAIS
 **Initially developed in the context of PLATOON EU H2020 project
 */
package de.fraunhofer.iais.eis.ids.broker.main;


import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.SecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.component.core.InfomodelFormalException;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.component.protocol.http.server.ComponentInteractorProvider;
import de.fraunhofer.iais.eis.ids.index.common.main.MainTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Objects;

/**
 * Entry point to the Broker
 */
@Configuration
@EnableAutoConfiguration(exclude = SolrAutoConfiguration.class)
@ComponentScan(basePackages = { "de.fraunhofer.iais.eis.ids.component.protocol.http.server"} )
public class Main extends MainTemplate implements ComponentInteractorProvider {

    Logger logger = LoggerFactory.getLogger(Main.class);

    //Initializing properties which are not inherited from MainTemplate
    @Value("${sparql.url}")
    private String sparqlEndpointUrl;

    @Value("${infomodel.contextUrl}")
    private String contextDocumentUrl;

    @Value("${jwks.trustedHosts}")
    private Collection<String> trustedJwksHosts;

    @Value("${daps.validateIncoming}")
    private boolean dapsValidateIncoming;

    @Value("${component.responseSenderAgent}")
    private String responseSenderAgent;

    @Value("${infomodel.validateWithShacl}")
    private boolean validateShacl;

    //Environment allows us to access application.properties
    @Autowired
    private Environment env;

    /**
     * This function is called during startup and takes care of the initialization
     */
    @PostConstruct
    @Override
    public void setUp() {
        //Assigning variables which are inherited from MainTemplate
        componentUri = env.getProperty("component.uri");
        componentMaintainer = env.getProperty("component.maintainer");
        componentCatalogUri = env.getProperty("component.catalogUri");
        componentModelVersion = env.getProperty("component.modelversion");
        sslCertificatePath = env.getProperty("ssl.certificatePath");
        elasticsearchHostname = env.getProperty("elasticsearch.hostname");
        elasticsearchPort = Integer.parseInt(Objects.requireNonNull(env.getProperty("elasticsearch.port")));
        keystorePassword = env.getProperty("keystore.password");
        keystoreAlias = env.getProperty("keystore.alias");
//        componentIdsId = env.getProperty("component.idsid");
        dapsUrl = env.getProperty("daps.url");
        trustAllCerts = Boolean.parseBoolean(env.getProperty("ssl.trustAllCerts"));
        ignoreHostName = Boolean.parseBoolean(env.getProperty("ssl.ignoreHostName"));

        try {
            multipartComponentInteractor = new AppConfig(createSelfDescriptionProvider())
                    .sparqlEndpointUrl(sparqlEndpointUrl)
                    .contextDocumentUrl(contextDocumentUrl)
                    .catalogUri(new URI(componentCatalogUri))
                    .securityTokenProvider(createSecurityTokenProvider())
                    .trustedJwksHosts(trustedJwksHosts)
                    .dapsValidateIncoming(dapsValidateIncoming)
                    .responseSenderAgent(new URI(responseSenderAgent))
                    .performShaclValidation(validateShacl)
                    .build();
        }
        catch (URISyntaxException e) {
            throw new InfomodelFormalException(e);
        }
    }

    @Override @PreDestroy
    public void shutDown() throws IOException {

    }


    public static void main(String[] args) throws TokenRetrievalException, URISyntaxException {
          SpringApplication.run(Main.class, args);
//        System.out.println(new AppResourceBuilder().build().toRdf());
//        final URI dummyUri = new URI("https://example.org/dummy");
//        final URI senderAgent = new URI("http://example.org/");
//        final SecurityTokenProvider securityTokenProvider = new SecurityTokenProvider() {
//            @Override
//            public String getSecurityToken() {
//                return "test1234";
//            }
//        };
//        System.out.println(new AppAvailableMessageBuilder()
//                ._issued_(CalendarUtil.now())
//                        ._affectedResource_(dummyUri)
//                ._modelVersion_("3.0.0")
//                ._issuerConnector_(dummyUri)
//                ._securityToken_(securityTokenProvider.getSecurityTokenAsDAT())
//                ._senderAgent_(senderAgent).build().toRdf());
   }
}
