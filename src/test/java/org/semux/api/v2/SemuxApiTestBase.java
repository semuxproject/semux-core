/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v2;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.Before;
import org.semux.api.ApiVersion;

public abstract class SemuxApiTestBase extends org.semux.api.SemuxApiTestBase {

    protected org.semux.api.v2.client.SemuxApi api;

    @Before
    public void setUp() {
        super.setUp();

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(config.apiUsername(), config.apiPassword())
                .build();
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(feature);
        clientConfig.register(JacksonFeature.class);

        Client client = ClientBuilder.newClient(clientConfig);

        WebTarget target = client.target("http://localhost:51710/" + ApiVersion.DEFAULT.prefix);
        api = WebResourceFactory.newResource(org.semux.api.v2.client.SemuxApi.class, target);
    }
}
