/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import static junit.framework.TestCase.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SemuxIpFilterSaverTest extends SemuxIpFilterTestBase {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws UnknownHostException {
        return Arrays.asList(new Object[][] {
                { getFile("empty.json"),
                        new SemuxIpFilter.Builder()
                                .build(),
                        null
                },
                { getFile("blacklist.json"),
                        new SemuxIpFilter.Builder()
                                .reject("1.2.3.4")
                                .reject("5.6.7.8")
                                .build(),
                        null
                },
                { getFile("whitelist.json"),
                        new SemuxIpFilter.Builder()
                                .accept("127.0.0.1/8")
                                .accept("192.168.0.0/16")
                                .reject("0.0.0.0/0")
                                .build(),
                        null
                },
        });
    }

    /**
     * Expectation
     */
    File jsonFile;

    /**
     * The ipfilter to be saved
     */
    SemuxIpFilter ipFilter;

    Class<? extends Throwable> exception;

    public SemuxIpFilterSaverTest(File jsonFile, SemuxIpFilter ipFilter, Class<? extends Throwable> exception) {
        this.jsonFile = jsonFile;
        this.ipFilter = ipFilter;
        this.exception = exception;
    }

    @Test
    public void testSave() throws IOException {
        if (exception != null) {
            expectedException.expect(exception);
        }

        SemuxIpFilter.Saver saver = new SemuxIpFilter.Saver();
        Path dest = temporaryFolder.newFile().toPath();
        saver.save(dest, ipFilter);

        assertEquals(
                FileUtils.readFileToString(jsonFile, "UTF-8").trim(),
                FileUtils.readFileToString(dest.toFile(), "UTF-8").trim());
    }
}
