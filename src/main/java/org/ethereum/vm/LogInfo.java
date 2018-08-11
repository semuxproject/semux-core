/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.vm;

import java.util.List;

import org.semux.crypto.Hex;

/**
 * Represents a Log emitted by a smart contract.
 */
public class LogInfo {

    private byte[] address;
    private List<DataWord> topics;
    private byte[] data;

    public LogInfo(byte[] address, List<DataWord> topics, byte[] data) {
        this.address = address;
        this.topics = topics;
        this.data = data;
    }

    public byte[] getAddress() {
        return address;
    }

    public List<DataWord> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (DataWord topic : topics) {
            String topicStr = Hex.encode(topic.getData());
            sb.append(topicStr).append(" ");
        }
        sb.append("]");

        return "LogInfo{" +
                "address=" + Hex.encode(address) +
                ", topics=" + sb +
                ", data=" + Hex.encode(data) +
                '}';
    }
}
