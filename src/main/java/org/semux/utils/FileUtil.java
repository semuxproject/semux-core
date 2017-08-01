/*
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.utils;

import java.io.File;

public class FileUtil {

    /**
     * Delete a file or directory recursively.
     * 
     * @param file
     */
    public static void recursiveDelete(File file) {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                recursiveDelete(f);
            }
        }
        file.delete();
    }

}
