/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip39;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class MnemonicGeneratorTest {

    public static final char jpSpace = '\u3000';

    @Test
    public void testHappyPath() {

        Dictionary dictionary;
        try {
            dictionary = new Dictionary(Language.ENGLISH);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unknown dictionary");
        }

        MnemonicGenerator generator = new MnemonicGenerator();

        String phrase = generator.getWordlist(128, Language.ENGLISH);

        byte[] seed = generator.getSeedFromWordlist(phrase, "", Language.ENGLISH);
        byte[] seedAgain = generator.getSeedFromWordlist(phrase, "", Language.ENGLISH);
        Assert.assertArrayEquals(seed, seedAgain);

        // try using japanese spaces in english phrase list
        String jpSpacePhrase = phrase.replace(' ', jpSpace);
        byte[] seedFromJpSpaces = generator.getSeedFromWordlist(jpSpacePhrase, "", Language.ENGLISH);
        Assert.assertArrayEquals(seed, seedFromJpSpaces);

        String[] words = phrase.split(" ");

        @SuppressWarnings("unused")
        int index = dictionary.indexOf(words[0]);

        try {
            words[0] = "asdf";
            generator.getSeedFromWordlist(String.join(" ", words), "", Language.ENGLISH);
            Assert.fail("Should not allow unknown word");
        } catch (IllegalArgumentException e) {
        }

        // try {
        // words[0] = dictionary.getWord((index + 1) % 2048);
        // generator.getSeedFromWordlist(String.join(" ", words), "", Language.english);
        // Assert.fail("Should not allow non-checksum'd words");
        // } catch (IllegalArgumentException e) {
        //
        // }
    }
}
