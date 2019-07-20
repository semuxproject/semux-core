/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip39;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.text.Normalizer;
import java.util.BitSet;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.semux.crypto.bip32.util.BitUtil;
import org.semux.crypto.bip32.util.HashUtil;

/**
 * Generate and Process Mnemonic codes
 */
public class MnemonicGenerator {

    public static final String SPACE_JP = "\u3000";

    private SecureRandom secureRandom = new SecureRandom();

    public byte[] getSeedFromWordlist(String words, String password, Language language) {

        Dictionary dictionary;
        try {
            dictionary = new Dictionary(language);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unknown dictionary");
        }

        if (password == null) {
            password = "";
        }

        password = Normalizer.normalize(password, Normalizer.Form.NFKD);

        String[] wordsList;
        if (language != Language.JAPANESE) {
            words = words.replaceAll(SPACE_JP, " ");
            wordsList = words.split(" ");
        } else {
            wordsList = words.split("" + SPACE_JP);
        }

        // validate that things look alright
        if (wordsList.length < 12) {
            throw new IllegalArgumentException("Must be at least 12 words");
        }
        if (wordsList.length > 24) {
            throw new IllegalArgumentException("Must be less than 24 words");
        }

        words = Normalizer.normalize(words, Normalizer.Form.NFKD);

        // check all the words are found
        for (String word : wordsList) {
            if (dictionary.indexOf(word.trim()) < 0) {
                throw new IllegalArgumentException("Unknown word: " + word);
            }
        }

        // check the checksum

        String salt = "mnemonic" + password;
        return pbkdf2HmacSha512(words.trim().toCharArray(), salt.getBytes(Charset.forName("UTF-8")), 2048, 512);
    }

    private byte[] pbkdf2HmacSha512(final char[] password, final byte[] salt, final int iterations,
            final int keyLength) {

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKey key = skf.generateSecret(spec);
            return key.getEncoded();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public String getWordlist(int entropyLength, Language language) {
        byte[] entropy = secureRandom.generateSeed(entropyLength / 8);
        return getWordlist(entropy, language);
    }

    public String getWordlist(byte[] entropy, Language language) {

        int entropyLength = entropy.length * 8;
        Dictionary dictionary;
        try {
            dictionary = new Dictionary(language);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unknown dictionary");
        }
        if (entropyLength < 128) {
            throw new IllegalArgumentException("Entropy must be over 128");
        }
        if (entropyLength > 256) {
            throw new IllegalArgumentException("Entropy must be less than 256");
        }
        if (entropyLength % 32 != 0) {
            throw new IllegalArgumentException("Entropy must be a multiple of 32");
        }
        int checksumLength = entropyLength / 32;
        byte[] hash = HashUtil.sha256(entropy);

        BitSet hashBitset = createBitset(hash);
        BitSet bitSet = createBitset(entropy);

        BitSet checksum = hashBitset.get(0, checksumLength);
        bitSet = append(checksum, bitSet, entropyLength);

        StringBuilder ret = new StringBuilder();

        int numWords = (entropyLength + checksumLength) / 11;
        for (int i = 0; i < numWords; i++) {
            BitSet range = bitSet.get(i * 11, (i + 1) * 11);
            int wordIdx = 0;
            if (!range.isEmpty()) {
                wordIdx = getInt(range);
            }
            String word = dictionary.getWord(wordIdx);
            if (i > 0) {
                if (language == Language.JAPANESE) {
                    ret.append(SPACE_JP);
                } else {
                    ret.append(" ");
                }

            }
            ret.append(word);

        }

        return ret.toString();
    }

    /**
     * For some reason Bitset.valueOf() does not return correct data we expect.
     *
     * @param bytes
     * @return
     */
    private BitSet createBitset(byte[] bytes) {
        BitSet ret = new BitSet();
        int offset = 0;
        for (byte b : bytes) {
            for (int i = 1; i < 9; i++) {
                if (BitUtil.checkBit(b, i)) {
                    ret.set(offset);
                }
                offset++;
            }
        }
        return ret;
    }

    /**
     * get a printable version of a bitset
     *
     * @param bitset
     * @param length
     * @return
     */
    @SuppressWarnings("unused")
    private String getBitString(BitSet bitset, int length) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < length; i++) {
            ret.append(bitset.get(i) ? "1" : "0");
        }
        return ret.toString();
    }

    private int getInt(BitSet range) {

        int ret = 0;
        for (int i = 0; i < 11; i++) {

            ret = ret << 1;
            if (range.get(i)) {
                ret |= 1;
            }
        }
        return ret;
    }

    private BitSet append(BitSet a, BitSet b, int bLength) {

        // shift A << bLenght
        BitSet ret = shift(a, bLength);
        ret.or(b);
        return ret;
    }

    private BitSet shift(BitSet bitSet, int length) {
        BitSet ret = new BitSet(bitSet.length() + length);
        for (int i = 0; i < bitSet.length(); i++) {
            if (bitSet.get(i)) {
                ret.set(i + length);
            }
        }
        return ret;
    }
}
