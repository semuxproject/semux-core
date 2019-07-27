/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.http;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.Kernel;
import org.semux.api.ApiHandler;
import org.semux.config.Config;
import org.semux.util.BasicAuth;
import org.semux.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;

/**
 * HTTP handler for Semux API.
 */
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpHandler.class);

    public static final String INTERNAL_SERVER_ERROR_RESPONSE = "{\"success\":false,\"message\":\"500 Internal Server Error\"}";
    public static final String NOT_FOUND_RESPONSE = "{\"success\":false,\"message\":\"404 Not Found\"}";
    public static final String BAD_REQUEST_RESPONSE = "{\"success\":false,\"message\":\"400 Bad Request\"}";

    private static final Charset CHARSET = CharsetUtil.UTF_8;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap(
            HttpHandler.class.getResourceAsStream("/org/semux/api/mime.types"));

    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private static final Pattern STATIC_FILE_PATTERN = Pattern.compile("^.+\\.(html|json|js|css|png)$");

    private final Config config;
    private final ApiHandler apiHandler;

    private Boolean isKeepAlive = false;

    /**
     * Construct a HTTP handler.
     *
     * @param kernel
     * @param apiHandler
     */
    public HttpHandler(Kernel kernel, ApiHandler apiHandler) {
        this.config = kernel.getConfig();
        this.apiHandler = apiHandler;
    }

    /**
     * For testing only.
     *
     * @param config
     *            semux config instance.
     * @param apiHandler
     *            a customized ApiHandler for testing purpose.
     */
    protected HttpHandler(Config config, ApiHandler apiHandler) {
        this.config = config;
        this.apiHandler = apiHandler;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        URI uri = URI.create(msg.uri());

        // copy collection to ensure it is writable
        Map<String, List<String>> params = new HashMap<>(new QueryStringDecoder(msg.uri(), CHARSET).parameters());
        HttpHeaders headers = msg.headers();
        ByteBuf body = Unpooled.buffer(HttpConstants.MAX_BODY_SIZE);

        // basic authentication
        if (!checkBasicAuth(headers)) {
            FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
            resp.headers().set(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"Semux RESTful API\"");
            resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());

            ctx.writeAndFlush(resp);
            return;
        }

        // check decoding result
        if (!msg.decoderResult().isSuccess()) {
            writeJsonResponse(ctx, BAD_REQUEST, BAD_REQUEST_RESPONSE);
            return;
        }

        // check if keep-alive is supported
        isKeepAlive = HttpUtil.isKeepAlive(msg);

        // read request body
        ByteBuf content = msg.content();
        int length = content.readableBytes();
        if (length > 0) {
            body.writeBytes(content, length);
        }

        // parse parameter from request body
        if (body.readableBytes() > 0) {
            // FIXME: assuming "application/x-www-form-urlencoded"
            QueryStringDecoder decoder = new QueryStringDecoder("?" + body.toString(CHARSET));
            Map<String, List<String>> map = decoder.parameters();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (params.containsKey(entry.getKey())) {
                    params.get(entry.getKey()).addAll(entry.getValue());
                } else {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // filter parameters
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            List<String> v = entry.getValue();
            // duplicate names are not allowed.
            if (!v.isEmpty()) {
                map.put(entry.getKey(), v.get(0));
            }
        }

        // delegate the request
        ChannelFuture lastContentFuture;
        String path = uri.getPath();
        if ("/".equals(path)) {
            lastContentFuture = writeStaticFile(ctx, "/org/semux/api/index.html");

        } else if (STATIC_FILE_PATTERN.matcher(path).matches()) {
            if (path.startsWith("/swagger-ui/")) {
                lastContentFuture = writeStaticFile(ctx,
                        "/META-INF/resources/webjars/swagger-ui/3.22.2" + path.substring(11));
            } else {
                lastContentFuture = writeStaticFile(ctx, "/org/semux/api" + path);
            }

        } else {
            boolean prettyPrint = Boolean.parseBoolean(map.get("pretty"));
            Response response = apiHandler.service(msg.method(), path, map, headers);
            lastContentFuture = writeApiResponse(ctx, prettyPrint, response);
        }

        if (!isKeepAlive) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in API http handler", cause);
        writeJsonResponse(ctx, INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_RESPONSE);
    }

    private boolean checkBasicAuth(HttpHeaders headers) {
        Pair<String, String> auth = BasicAuth.parseAuth(headers.get(HttpHeaderNames.AUTHORIZATION));

        return auth != null
                && MessageDigest.isEqual(Bytes.of(auth.getLeft()), Bytes.of(config.apiUsername()))
                && MessageDigest.isEqual(Bytes.of(auth.getRight()), Bytes.of(config.apiPassword()));
    }

    private ChannelFuture writeStaticFile(ChannelHandlerContext ctx, String resourceFullPath) {
        InputStream inputStream = getClass().getResourceAsStream(resourceFullPath);
        if (inputStream == null) {
            return writeJsonResponse(ctx, NOT_FOUND, NOT_FOUND_RESPONSE);
        }

        DefaultHttpResponse resp = new DefaultHttpResponse(HTTP_1_1, OK);
        resp.headers().set(CONNECTION, isKeepAlive ? KEEP_ALIVE : CLOSE);
        resp.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(resourceFullPath));
        HttpUtil.setTransferEncodingChunked(resp, true);
        ctx.write(resp);

        return ctx.writeAndFlush(new HttpChunkedInput(new ChunkedStream(inputStream)));
    }

    private ChannelFuture writeApiResponse(ChannelHandlerContext ctx, Boolean prettyPrint, Response response) {
        HttpResponseStatus status = HttpResponseStatus.valueOf(response.getStatus());
        String responseBody;

        Object entity = response.getEntity();
        if (entity instanceof String) {
            responseBody = (String) entity;
        } else {
            try {
                if (prettyPrint) {
                    responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entity);
                } else {
                    responseBody = objectMapper.writeValueAsString(entity);
                }
            } catch (JsonProcessingException e) {
                status = INTERNAL_SERVER_ERROR;
                responseBody = INTERNAL_SERVER_ERROR_RESPONSE;
            }
        }

        return writeJsonResponse(ctx, status, responseBody);
    }

    private ChannelFuture writeJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String responseBody) {
        return writeResponse(ctx, JSON_CONTENT_TYPE, status, responseBody);
    }

    private ChannelFuture writeResponse(ChannelHandlerContext ctx, String contentType, HttpResponseStatus status,
            String responseBody) {
        // construct a HTTP response
        FullHttpResponse resp = new DefaultFullHttpResponse(
                HTTP_1_1,
                status,
                Unpooled.copiedBuffer(responseBody == null ? "" : responseBody, CHARSET));

        // set response headers
        resp.headers().set(CONNECTION, isKeepAlive ? KEEP_ALIVE : CLOSE);
        resp.headers().set(CONTENT_TYPE, contentType);
        HttpUtil.setTransferEncodingChunked(resp, true);

        // write response
        return ctx.writeAndFlush(resp);
    }
}
