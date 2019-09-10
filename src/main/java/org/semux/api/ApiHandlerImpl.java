/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.semux.Kernel;
import org.semux.api.http.HttpHandler;
import org.semux.api.v2.SemuxApiImpl;
import org.semux.api.v2.server.AccountApi;
import org.semux.api.v2.server.BlockchainApi;
import org.semux.api.v2.server.DelegateApi;
import org.semux.api.v2.server.NodeApi;
import org.semux.api.v2.server.SemuxApi;
import org.semux.api.v2.server.ToolApi;
import org.semux.api.v2.server.WalletApi;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

/**
 * The handler that processes all api requests. It delegates the request to
 * Semux API implementations based on version.
 */
public class ApiHandlerImpl implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiHandlerImpl.class);

    private static final Pattern VERSIONED_PATH = Pattern.compile("/(v[.0-9]+)(/.*)");

    private final Map<ApiVersion, Map<ImmutablePair<HttpMethod, String>, Route>> routes = new HashMap<>();

    public ApiHandlerImpl(Kernel kernel) {
        Map<ImmutablePair<HttpMethod, String>, Route> routesV2 = new HashMap<>();
        SemuxApi implV2 = new SemuxApiImpl(kernel);
        load(routesV2, implV2, kernel.getConfig().apiPublicServices(), true);
        load(routesV2, implV2, kernel.getConfig().apiPrivateServices(), false);

        this.routes.put(ApiVersion.v2_0_0, routesV2);
        this.routes.put(ApiVersion.v2_1_0, routesV2);
        this.routes.put(ApiVersion.v2_2_0, routesV2);
        this.routes.put(ApiVersion.v2_3_0, routesV2);
        this.routes.put(ApiVersion.v2_4_0, routesV2);
    }

    private void load(Map<ImmutablePair<HttpMethod, String>, Route> routes, SemuxApi impl,
            String[] services, boolean isPublic) {
        for (String service : services) {
            switch (service) {
            case "blockchain":
                routes.putAll(loadRoutes(impl, BlockchainApi.class, isPublic));
                break;
            case "account":
                routes.putAll(loadRoutes(impl, AccountApi.class, isPublic));
                break;
            case "delegate":
                routes.putAll(loadRoutes(impl, DelegateApi.class, isPublic));
                break;
            case "tool":
                routes.putAll(loadRoutes(impl, ToolApi.class, isPublic));
                break;
            case "node":
                routes.putAll(loadRoutes(impl, NodeApi.class, isPublic));
                break;
            case "wallet":
                routes.putAll(loadRoutes(impl, WalletApi.class, isPublic));
                break;
            }
        }
    }

    @Override
    public Response service(HttpMethod method, String path, Map<String, String> params, HttpHeaders headers) {
        Route route = matchRoute(method, path);
        if (route == null) {
            return Response.status(NOT_FOUND).entity(HttpHandler.NOT_FOUND_RESPONSE).build();
        }

        // invoke the params
        try {
            return (Response) route.invoke(params);
        } catch (Exception e) {
            logger.warn("Internal error", e);
            return Response.status(INTERNAL_SERVER_ERROR).entity(HttpHandler.INTERNAL_SERVER_ERROR_RESPONSE).build();
        }
    }

    @Override
    public boolean isAuthRequired(HttpMethod method, String path) {
        Route route = matchRoute(method, path);
        return route != null && !route.isPublic;
    }

    /**
     * Matches route by [version, method, path]
     */
    private Route matchRoute(HttpMethod method, String path) {
        Route route = null;
        Matcher m = VERSIONED_PATH.matcher(path);
        if (m.matches() && routes.containsKey(ApiVersion.of(m.group(1)))) {
            route = routes.get(ApiVersion.of(m.group(1))).get(ImmutablePair.of(method, m.group(2)));
        }

        return route;
    }

    private HttpMethod readHttpMethod(Method method) {
        for (Annotation anno : method.getAnnotations()) {
            if (anno.annotationType().equals(GET.class)) {
                return HttpMethod.GET;
            } else if (anno.annotationType().equals(POST.class)) {
                return HttpMethod.POST;
            } else if (anno.annotationType().equals(PUT.class)) {
                return HttpMethod.PUT;
            } else if (anno.annotationType().equals(DELETE.class)) {
                return HttpMethod.DELETE;
            }
        }

        logger.error("Failed to read HTTP method from {}", method);
        return null;
    }

    private String readPath(Method method) {
        try {
            Path path = method.getAnnotation(Path.class);
            return path.value();
        } catch (NullPointerException e) {
            logger.error("Failed to read HTTP path from {}", method, e);
            return null;
        }
    }

    private Map<ImmutablePair<HttpMethod, String>, Route> loadRoutes(Object impl, Class<?> api, boolean isPublic) {
        Map<ImmutablePair<HttpMethod, String>, Route> result = new HashMap<>();

        try {
            for (Method methodInterface : api.getMethods()) {
                Method methodImpl = impl.getClass().getMethod(methodInterface.getName(),
                        methodInterface.getParameterTypes());

                HttpMethod httpMethod = readHttpMethod(methodInterface);
                String path = readPath(methodInterface);

                if (httpMethod != null && path != null) {
                    result.put(ImmutablePair.of(httpMethod, path),
                            new Route(impl, httpMethod, path, methodInterface, methodImpl, isPublic));
                    logger.trace("Loaded route: {} {}", httpMethod, path);
                }
            }
        } catch (SecurityException | NoSuchMethodException e) {
            throw new UnreachableException(e);
        }

        return result;
    }

    private class Route {

        final Object semuxApi;

        @SuppressWarnings("unused")
        final HttpMethod httpMethod;

        @SuppressWarnings("unused")
        final String path;

        @SuppressWarnings("unused")
        final Method methodInterface;

        final Method methodImpl;

        final List<Pair<QueryParam, Class<?>>> queryParams;

        final boolean isPublic;

        Route(Object semuxApi, HttpMethod httpMethod, String path, Method methodInterface, Method methodImpl,
                boolean isPublic) {
            this.semuxApi = semuxApi;
            this.httpMethod = httpMethod;
            this.path = path;
            this.methodInterface = methodInterface;
            this.methodImpl = methodImpl;
            this.queryParams = Arrays
                    .stream(methodInterface.getParameters())
                    .map(p -> new ImmutablePair<QueryParam, Class<?>>(p.getAnnotation(QueryParam.class), p.getType()))
                    .collect(Collectors.toList());
            this.isPublic = isPublic;
        }

        Object invoke(Map<String, String> params) throws Exception {
            Object[] args = queryParams.stream().map(p -> {
                String param = params.getOrDefault(p.getLeft().value(), null);

                if (param == null) {
                    return null;
                }

                // convert params
                if (p.getRight().equals(Boolean.class)) {
                    return Boolean.parseBoolean(param);
                }

                return param;
            }).toArray();

            return this.methodImpl.invoke(semuxApi, args);
        }
    }
}
