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
