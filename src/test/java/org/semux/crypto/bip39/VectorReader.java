/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.crypto.bip39;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.semux.crypto.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VectorReader {

    public Map<String, List<Vector>> getVectors() throws IOException {
        InputStream file = this.getClass().getClassLoader().getResourceAsStream("vector/vector.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tree = mapper.readTree(file);

        JsonNode english = tree.get("english");

        List<Vector> enVectors = new ArrayList<>();
        for (int i = 0; i < english.size(); i++) {
            ArrayNode v = (ArrayNode) english.get(i);
            Vector vector = new Vector(Hex.decode(v.get(0).asText()), v.get(1).asText(), v.get(2).asText(),
                    v.get(3).asText(), null);
            enVectors.add(vector);
        }

        JsonNode japanese = tree.get("japanese");

        List<Vector> jpVectors = new ArrayList<>();
        for (int i = 0; i < japanese.size(); i++) {
            ObjectNode v = (ObjectNode) japanese.get(i);
            Vector vector = new Vector(Hex.decode(v.get("entropy").asText()), v.get("mnemonic").asText(),
                    v.get("seed").asText(),
                    v.get("bip32_xprv").asText(), v.get("passphrase").asText());
            jpVectors.add(vector);
        }
        Map<String, List<Vector>> ret = new HashMap<>();
        ret.put("english", enVectors);
        ret.put("japanese", jpVectors);

        return ret;
    }

}
