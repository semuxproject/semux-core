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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.semux.Kernel;
import org.semux.api.http.HttpHandler;
import org.semux.api.v2.SemuxApiImpl;
import org.semux.api.v2.api.SemuxApi;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

/**
 * The handler that processes all api requests. It delegates the request to
 * Semux API implementations based on version.
 */
public class ApiHandlerImpl implements ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiHandlerImpl.class);

    private static final Pattern VERSIONED_PATH = Pattern.compile("/(v[\\.0-9]+)(/.*)");

    private final Map<ApiVersion, Map<ImmutablePair<HttpMethod, String>, Route>> routes = new HashMap<>();

    public ApiHandlerImpl(Kernel kernel) {
        SemuxApi apiImplementationV2 = new SemuxApiImpl(kernel);
        Class<?> apiInterfaceV2 = SemuxApi.class;
        Map<ImmutablePair<HttpMethod, String>, Route> routesV2 = loadRoutes(apiImplementationV2, apiInterfaceV2);

        this.routes.put(ApiVersion.v2, routesV2);
        this.routes.put(ApiVersion.v2_0_0, routesV2);
        this.routes.put(ApiVersion.v2_1_0, routesV2);
    }

    @Override
    public Response service(HttpMethod method, String path, Map<String, String> params, HttpHeaders headers) {
        // find the route by [version, method, path]
        Route route = null;
        Matcher m = VERSIONED_PATH.matcher(path);
        if (m.matches() && routes.containsKey(ApiVersion.of(m.group(1)))) {
            route = routes.get(ApiVersion.of(m.group(1))).get(ImmutablePair.of(method, m.group(2)));
        }
        if (route == null) {
            return Response.status(NOT_FOUND).entity(HttpHandler.NOT_FOUND_RESPONSE).build();
        }

        // invoke the params
        try {
            return (Response) route.invoke(params);
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(HttpHandler.INTERNAL_SERVER_ERROR_RESPONSE).build();
        }
    }

    private Map<ImmutablePair<HttpMethod, String>, Route> loadRoutes(Object semuxApi, Class<?> swaggerInterface) {
        Map<ImmutablePair<HttpMethod, String>, Route> result = new HashMap<>();

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
        Swagger swagger = new Reader(new Swagger()).read(swaggerInterface);
        for (Map.Entry<String, io.swagger.models.Path> entry : swagger.getPaths().entrySet()) {
            for (Map.Entry<io.swagger.models.HttpMethod, Operation> operation : entry.getValue().getOperationMap()
                    .entrySet()) {
                HttpMethod method = HttpMethod.valueOf(operation.getKey().name());
                String path = entry.getKey().substring(entry.getKey().indexOf('/', 1)); // stripped path
                ImmutablePair<HttpMethod, String> key = ImmutablePair.of(method, path);
                ImmutablePair<Method, Method> methodPair = methodMap.get(operation.getValue().getOperationId());

                result.put(key, new Route(semuxApi, key.left, key.right, methodPair.left, methodPair.right));
                logger.debug("Loaded route: {} {}", key.getLeft(), key.getRight());
            }
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

        Route(Object semuxApi, HttpMethod httpMethod, String path, Method methodInterface, Method methodImpl) {
            this.semuxApi = semuxApi;
            this.httpMethod = httpMethod;
            this.path = path;
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

                        return param;
                    }).toArray());
        }
    }
}
