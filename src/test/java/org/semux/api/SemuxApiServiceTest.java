/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static org.junit.Assert.assertEquals;

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
    public void testGetApiBaseUrl() {
        assertEquals(String.format("http://127.0.0.1:51710/%s/", ApiVersion.DEFAULT.prefix),
                apiMock.getApi().getApiBaseUrl());
    }

    @Test
    public void testGetApiExplorerUrl() {
        assertEquals(String.format("http://127.0.0.1:51710/index.html"),
                apiMock.getApi().getApiExplorerUrl());
    }

}
