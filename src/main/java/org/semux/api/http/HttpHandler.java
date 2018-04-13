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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.semux.Kernel;
import org.semux.api.ApiHandler;
import org.semux.api.Version;
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
 * 
 */
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpHandler.class);

    protected static final int MAX_BODY_SIZE = 512 * 1024; // 512KB
    private static final Charset CHARSET = CharsetUtil.UTF_8;
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap(
            HttpHandler.class.getResourceAsStream("/org/semux/api/mime.types"));

    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
    private static final String INTERNAL_SERVER_ERROR_RESPONSE = "{\"success\":false,\"message\":\"500 Internal Server Error\"}";
    private static final String NOT_FOUND_RESPONSE = "{\"success\":false,\"message\":\"404 Not Found\"}";
    private static final String BAD_REQUEST_RESPONSE = "{\"success\":false,\"message\":\"400 Bad Request\"}";

    private static final Pattern STATIC_FILE_PATTERN = Pattern.compile("^.+\\.(html|json|js|css|png)$");

    private Config config;
    private final Map<Version, ApiHandler> apiHandlers;

    private Boolean isKeepAlive = false;

    public HttpHandler(Kernel kernel, final Map<Version, ApiHandler> apiHandlers) {
        this.config = kernel.getConfig();
        this.apiHandlers = apiHandlers;
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
        this.apiHandlers = Collections
                .unmodifiableMap(Arrays.stream(Version.values())
                        .collect(Collectors.toMap(
                                v -> v,
                                v -> apiHandler)));
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
        ByteBuf body = Unpooled.buffer(MAX_BODY_SIZE);

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
        if ("application/x-www-form-urlencoded".equals(headers.get("Content-type"))
                && body.readableBytes() > 0) {
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
        Version version = checkVersionPrefix(uri.toString());
        final String path = uri.getPath();
        if (STATIC_FILE_PATTERN.matcher(path).matches()) { // static files
            lastContentFuture = writeStaticFile(ctx, "/org/semux/api", uriToResourcePath(path));
        } else { // api
            boolean prettyPrint = Boolean.parseBoolean(map.get("pretty"));
            Object response = apiHandlers.get(version).service(msg.method(), path, map, headers);
            lastContentFuture = writeApiResponse(ctx, prettyPrint, response);
        }

        if (!isKeepAlive) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean checkBasicAuth(HttpHeaders headers) {
        Pair<String, String> auth = BasicAuth.parseAuth(headers.get(HttpHeaderNames.AUTHORIZATION));

        return auth != null
                && MessageDigest.isEqual(Bytes.of(auth.getLeft()), Bytes.of(config.apiUsername()))
                && MessageDigest.isEqual(Bytes.of(auth.getRight()), Bytes.of(config.apiPassword()));
    }

    private Version checkVersionPrefix(String uri) {
        Optional<Version> versionOptional = Arrays.stream(uri.split("/"))
                .filter(s -> s.startsWith("v"))
                .findFirst()
                .map(Version::fromPrefix);
        return versionOptional.orElse(Version.v1_0_1);
    }

    private String uriToResourcePath(String uri) {
        for (Version version : Version.values()) {
            String versionRegex = version.prefix.replace(".", "\\.");
            if (uri.matches("^/" + versionRegex + ".*$")) {
                return uri.replaceFirst(versionRegex, version.toString().replace(".", "_"));
            }
        }
        return uri;
    }

    private ChannelFuture writeStaticFile(ChannelHandlerContext ctx, String prefix, String filePath) {
        InputStream inputStream = getClass().getResourceAsStream(prefix + filePath);
        if (inputStream == null) {
            return writeJsonResponse(ctx, NOT_FOUND, NOT_FOUND_RESPONSE);
        }

        DefaultHttpResponse resp = new DefaultHttpResponse(HTTP_1_1, OK);
        resp.headers().set(CONNECTION, isKeepAlive ? KEEP_ALIVE : CLOSE);
        resp.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(filePath));
        HttpUtil.setTransferEncodingChunked(resp, true);
        ctx.write(resp);

        return ctx.writeAndFlush(new HttpChunkedInput(new ChunkedStream(inputStream)));
    }

    private ChannelFuture writeApiResponse(ChannelHandlerContext ctx, Boolean prettyPrint, Object response) {
        HttpResponseStatus status;
        if (response instanceof javax.ws.rs.core.Response) { // since v2.0.0, a standard JAX-RS response is provided
            status = HttpResponseStatus.valueOf(((Response) response).getStatus());
            response = ((Response) response).getEntity();
        } else { // prior to v2.0.0, status is always OK
            status = OK;
        }

        // encode response object
        String responseBody;
        try {
            if (prettyPrint) {
                responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            } else {
                responseBody = objectMapper.writeValueAsString(response);
            }
        } catch (JsonProcessingException e) {
            status = INTERNAL_SERVER_ERROR;
            responseBody = INTERNAL_SERVER_ERROR_RESPONSE;
        }

        return writeJsonResponse(ctx, status, responseBody);
    }

    private ChannelFuture writeJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String responseBody) {
        return writeStringResponse(ctx, JSON_CONTENT_TYPE, status, responseBody);
    }

    private ChannelFuture writeStringResponse(ChannelHandlerContext ctx, String contentType, HttpResponseStatus status,
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in API http handler", cause);
        writeJsonResponse(ctx, INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_RESPONSE);
    }
}
