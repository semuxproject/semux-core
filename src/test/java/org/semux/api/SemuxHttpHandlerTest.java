package org.semux.api;

import java.util.Map;

import org.junit.BeforeClass;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

public class SemuxHttpHandlerTest {

    private static String ip = "127.0.0.1";
    private static int port = 15171;

    @BeforeClass
    public void testPOST() {
        SemuxAPI server = new SemuxAPI(new ApiHandler() {
            @Override
            public String service(String uri, Map<String, String> params, HttpHeaders headers, ByteBuf body) {
                return uri;
            }
        });
        server.start(ip, port);

        // wait for server to boot up
        while (!server.isListening()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }

        server.stop();
    }
}
