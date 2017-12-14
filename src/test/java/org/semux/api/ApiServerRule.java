/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import org.junit.rules.ExternalResource;
import org.semux.db.DBFactory;

public class ApiServerRule extends ExternalResource {

    protected static final String API_IP = "127.0.0.1";
    protected static final int API_PORT = 15171;

    private static SemuxAPIMock api;

    private DBFactory dbFactory;

    public ApiServerRule(DBFactory dbFactory) {
        this.dbFactory = dbFactory;
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        api = new SemuxAPIMock(dbFactory);
        api.start(API_IP, API_PORT);
    }

    @Override
    protected void after() {
        super.after();
        api.stop();
    }

    public SemuxAPIMock getApi() {
        return api;
    }
}
