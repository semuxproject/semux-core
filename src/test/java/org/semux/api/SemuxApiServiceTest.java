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
    public void getAPIUrlTest() {
        assertEquals("http://127.0.0.1:51710/v2.0.0/", apiMock.getApi().getAPIUrl());
    }

    @Test
    public void getSwaggerUrlTest() {
        assertEquals("http://127.0.0.1:51710/v2.0.0/swagger.html", apiMock.getApi().getSwaggerUrl());
    }

}
