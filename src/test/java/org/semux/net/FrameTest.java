/**
 * Copyright (c) 2017-2020 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.net;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class FrameTest {

    @Test
    public void testReadAndWrite() {
        short version = 0x1122;
        byte compressType = 0x33;
        byte packetType = 0x44;
        int packetId = 0x55667788;
        int packetSize = 0x99aabbcc;
        int bodySize = 0xddeeff00;
        Frame frame = new Frame(version, compressType, packetType, packetId, packetSize, bodySize, null);

        ByteBuf buf = Unpooled.copiedBuffer(new byte[Frame.HEADER_SIZE]);

        buf.writerIndex(0);
        frame.writeHeader(buf);

        buf.readerIndex(0);
        Frame frame2 = Frame.readHeader(buf);

        assertThat(frame2).isEqualToComparingFieldByFieldRecursively(frame);
    }
}
