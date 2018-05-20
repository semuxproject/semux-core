/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static org.junit.Assert.assertEquals;
import static org.semux.api.http.SemuxApiService.DEFAULT_VERSION;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.semux.rules.KernelRule;

public class SemuxApiServiceTest {

    @Rule
    public KernelRule kernelRule = new KernelRule(51610, 51710);

    SemuxApiMock apiMock;

    @Before
    public void setUp() {
        apiMock = new SemuxApiMock(kernelRule.getKernel());
        apiMock.start();
    }

    @After
    public void tearDown() {
        apiMock.stop();
    }

    @Test
    public void getAPIUrlTest() {
        assertEquals(String.format("http://127.0.0.1:51710/%s/", DEFAULT_VERSION.prefix),
                apiMock.getApi().getAPIUrl());
    }

    @Test
    public void getSwaggerUrlTest() {
        assertEquals(String.format("http://127.0.0.1:51710/%s/swagger.html", DEFAULT_VERSION.prefix),
                apiMock.getApi().getSwaggerUrl());
    }

}
