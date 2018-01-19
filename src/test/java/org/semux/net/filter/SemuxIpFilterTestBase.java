/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net.filter;

import java.io.File;

abstract class SemuxIpFilterTestBase {

    /**
     * Get a testing ipfilter.json from resource bundle
     *
     * @param fileName
     * @return
     */
    protected static File getFile(String fileName) {
        return new File(SemuxIpFilterLoaderTest.class.getResource("/ipfilter/" + fileName).getFile());
    }
}
