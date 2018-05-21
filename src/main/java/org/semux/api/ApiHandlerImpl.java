/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */

package org.semux.api;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

/**
 * Semux RESTful API handler implementation.
 *
 */
public class ApiHandlerImpl implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiHandlerImpl.class);

    private final FailableApiService semuxApi;

    private final Class<?> swaggerInterface;

    /**
     * [http method, uri] => {@link Route}
     */
    private final Map<ImmutablePair<HttpMethod, String>, Route> routes;

    public ApiHandlerImpl(FailableApiService semuxApi, Class<?> swaggerInterface) {
        this.semuxApi = semuxApi;
        this.swaggerInterface = swaggerInterface;
        this.routes = new ConcurrentHashMap<>();
        loadRoutes();
    }

    @Override
    public Object service(HttpMethod method, String uri, Map<String, String> params, HttpHeaders headers) {
        // strip trailing slash
        uri = uri.replaceAll("/$", "");

        Route route = routes.get(ImmutablePair.of(method, uri));
        if (route == null) {
            return semuxApi.failure(NOT_FOUND, "Invalid request: uri = " + uri);
        }

        try {
            return route.invoke(params);
        } catch (Exception e) {
            return semuxApi.failure(INTERNAL_SERVER_ERROR, "Failed to process your request: " + e.getMessage());
        }
    }

    private void loadRoutes() {
        // map of [operation id] => [methodInterface, methodImpl]
        Map<String, ImmutablePair<Method, Method>> methodMap = new HashMap<>();
        try {
            for (Method methodInterface : swaggerInterface.getMethods()) {
                Method methodImpl = semuxApi.getClass().getMethod(methodInterface.getName(),
                        methodInterface.getParameterTypes());
                methodMap.put(methodInterface.getName(), ImmutablePair.of(methodInterface, methodImpl));
            }
        } catch (NoSuchMethodException ex) {
            throw new UnreachableException(ex);
        }

        // load swagger annotations as routes
        Reader reader = new Reader(new Swagger());
        Swagger swagger = reader.read(swaggerInterface);
        for (Map.Entry<String, io.swagger.models.Path> pathEntry : swagger.getPaths().entrySet()) {
            for (Map.Entry<io.swagger.models.HttpMethod, Operation> operation : pathEntry.getValue().getOperationMap()
                    .entrySet()) {
                ImmutablePair<HttpMethod, String> key = ImmutablePair.of(HttpMethod.valueOf(operation.getKey().name()),
                        pathEntry.getKey());
                ImmutablePair<Method, Method> methodPair = methodMap.get(operation.getValue().getOperationId());
                routes.put(
                        key,
                        new Route(key.left, key.right, methodPair.left, methodPair.right));
                logger.debug("Loaded route: {} {}", key.getLeft(), key.getRight());
            }
        }
    }

    private class Route {

        @SuppressWarnings("unused")
        final HttpMethod httpMethod;

        @SuppressWarnings("unused")
        final String uri;

        @SuppressWarnings("unused")
        final Method methodInterface;

        final Method methodImpl;

        final List<Pair<QueryParam, Class<?>>> queryParams;

        Route(HttpMethod httpMethod, String uri, Method methodInterface, Method methodImpl) {
            this.httpMethod = httpMethod;
            this.uri = uri;
            this.methodInterface = methodInterface;
            this.methodImpl = methodImpl;
            this.queryParams = Arrays
                    .stream(methodInterface.getParameters())
                    .map(p -> new ImmutablePair<QueryParam, Class<?>>(p.getAnnotation(QueryParam.class), p.getType()))
                    .collect(Collectors.toList());
        }

        Object invoke(Map<String, String> params) throws Exception {
            return this.methodImpl.invoke(
                    semuxApi,
                    queryParams.stream().map(p -> {
                        String param = params.getOrDefault(p.getLeft().value(), null);

                        if (param == null) {
                            return null;
                        }

                        // convert params
                        if (p.getRight().equals(Boolean.class)) {
                            return Boolean.parseBoolean(param);
                        }

                        if (p.getRight().equals(Integer.class)) {
                            return Integer.parseInt(param);
                        }

                        return param;
                    }).toArray());
        }
    }
}
