/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip39;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Dictionary {

    private List<String> words = new ArrayList<>();

    public Dictionary(Language language) throws IOException {

        InputStream wordStream = this.getClass().getClassLoader()
                .getResourceAsStream("wordlists/" + language.name() + ".txt");

        BufferedReader reader = new BufferedReader(new InputStreamReader(wordStream, StandardCharsets.UTF_8));
        String word;

        while ((word = reader.readLine()) != null) {
            words.add(word);
        }
    }

    public String getWord(int wordIdx) {
        return words.get(wordIdx);
    }

    public int indexOf(String word) {
        return words.indexOf(word);
    }
}
