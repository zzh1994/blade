package com.blade.mvc.route;

import com.blade.ioc.annotation.Order;
import com.blade.kit.*;
import com.blade.mvc.RouteContext;
import com.blade.mvc.handler.RouteHandler;
import com.blade.mvc.handler.RouteHandler0;
import com.blade.mvc.hook.Signature;
import com.blade.mvc.hook.WebHook;
import com.blade.mvc.http.HttpMethod;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.blade.kit.BladeKit.logAddRoute;
import static com.blade.kit.BladeKit.logWebSocket;

/**
 * Default Route Matcher
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.7.1-release
 */
@Slf4j
public class RouteMatcher {

    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("/([^:/]*):([^/]+)");
    private static final String  METHOD_NAME           = "handle";

    // Storage URL and route
    private Map<String, Route>       routes          = new HashMap<>();
    private Map<String, List<Route>> hooks           = new HashMap<>();
    private List<Route>              middleware      = null;
    private Map<String, Method[]>    classMethodPool = new ConcurrentHashMap<>();
    private Map<Class<?>, Object>    controllerPool  = new ConcurrentHashMap<>();

    private Map<HttpMethod, Map<Integer, FastRouteMappingInfo>> regexRoutes        = new HashMap<>();
    private Map<String, Route>                                  staticRoutes       = new HashMap<>();
    private Map<HttpMethod, Pattern>                            regexRoutePatterns = new HashMap<>();
    private Map<HttpMethod, Integer>                            indexes            = new HashMap<>();
    private Map<HttpMethod, StringBuilder>                      patternBuilders    = new HashMap<>();

    private List<String> webSockets = new ArrayList<>(4);

    @Deprecated
    private Route addRoute(HttpMethod httpMethod, String path, RouteHandler0 handler, String methodName) throws NoSuchMethodException {
        Class<?> handleType = handler.getClass();
        Method   method     = handleType.getMethod(methodName, Request.class, Response.class);
        return addRoute(httpMethod, path, handler, RouteHandler0.class, method);
    }

    private Route addRoute(HttpMethod httpMethod, String path, RouteHandler handler, String methodName) throws NoSuchMethodException {
        Class<?> handleType = handler.getClass();
        Method   method     = handleType.getMethod(methodName, RouteContext.class);
        return addRoute(httpMethod, path, handler, RouteHandler.class, method);
    }

    Route addRoute(Route route) {
        String     path           = route.getPath();
        HttpMethod httpMethod     = route.getHttpMethod();
        Object     controller     = route.getTarget();
        Class<?>   controllerType = route.getTargetType();
        Method     method         = route.getAction();
        return addRoute(httpMethod, path, controller, controllerType, method);
    }

    private Route addRoute(HttpMethod httpMethod, String path, Object controller, Class<?> controllerType, Method method) {

        // [/** | /*]
        path = "*".equals(path) ? "/.*" : path;
        path = path.replace("/**", "/.*").replace("/*", "/.*");

        String key = path + "#" + httpMethod.toString();

        // exist
        if (this.routes.containsKey(key)) {
            log.warn("\tRoute {} -> {} has exist", path, httpMethod.toString());
        }

        Route route = new Route(httpMethod, path, controller, controllerType, method);
        if (BladeKit.isWebHook(httpMethod)) {
            Order order = controllerType.getAnnotation(Order.class);
            if (null != order) {
                route.setSort(order.value());
            }
            if (this.hooks.containsKey(key)) {
                this.hooks.get(key).add(route);
            } else {
                List<Route> empty = new ArrayList<>();
                empty.add(route);
                this.hooks.put(key, empty);
            }
        } else {
            this.routes.put(key, route);
        }
        return route;
    }

    @Deprecated
    public Route addRoute(String path, RouteHandler0 handler, HttpMethod httpMethod) {
        try {
            return addRoute(httpMethod, path, handler, METHOD_NAME);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public Route addRoute(String path, RouteHandler handler, HttpMethod httpMethod) {
        try {
            return addRoute(httpMethod, path, handler, METHOD_NAME);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    public void route(String path, Class<?> clazz, String methodName) {
        Assert.notNull(methodName, "Method name not is null");
        HttpMethod httpMethod = HttpMethod.ALL;
        if (methodName.contains(":")) {
            String[] methodArr = methodName.split(":");
            httpMethod = HttpMethod.valueOf(methodArr[0].toUpperCase());
            methodName = methodArr[1];
        }
        this.route(path, clazz, methodName, httpMethod);
    }

    public void route(String path, Class<?> clazz, String methodName, HttpMethod httpMethod) {
        try {
            Assert.notNull(path, "Route path not is null!");
            Assert.notNull(clazz, "Route type not is null!");
            Assert.notNull(methodName, "Method name not is null");
            Assert.notNull(httpMethod, "Request Method not is null");

            Method[] methods = classMethodPool.computeIfAbsent(clazz.getName(), k -> clazz.getMethods());
            if (null == methods) {
                return;
            }
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    Object controller = controllerPool.computeIfAbsent(clazz, k -> ReflectKit.newInstance(clazz));
                    addRoute(httpMethod, path, controller, clazz, method);
                }
            }
        } catch (Exception e) {
            log.error("Add route method error", e);
        }
    }

    public Route lookupRoute(String httpMethod, String path) {
        path = parsePath(path);
        String routeKey = path + '#' + httpMethod.toUpperCase();
        Route  route    = staticRoutes.get(routeKey);
        if (null != route) {
            return route;
        }
        route = staticRoutes.get(path + "#ALL");
        if (null != route) {
            return route;
        }

        Map<String, String> uriVariables  = new LinkedHashMap<>();
        HttpMethod          requestMethod = HttpMethod.valueOf(httpMethod);
        try {
            Pattern pattern = regexRoutePatterns.get(requestMethod);
            if (null == pattern) {
                pattern = regexRoutePatterns.get(HttpMethod.ALL);
                if (null != pattern) {
                    requestMethod = HttpMethod.ALL;
                }
            }
            if (null == pattern) {
                return null;
            }
            Matcher matcher = null;
            if (path != null) {
                matcher = pattern.matcher(path);
            }
            boolean matched = false;
            if (matcher != null) {
                matched = matcher.matches();
            }
            if (!matched) {
                requestMethod = HttpMethod.ALL;
                pattern = regexRoutePatterns.get(requestMethod);
                if (null == pattern) {
                    return null;
                }
                if (path != null) {
                    matcher = pattern.matcher(path);
                }
                matched = matcher != null && matcher.matches();
            }
            if (matched) {
                int i;
                for (i = 1; matcher.group(i) == null; i++) ;
                FastRouteMappingInfo mappingInfo = regexRoutes.get(requestMethod).get(i);
                route = mappingInfo.getRoute();

                // find path variable
                String uriVariable;
                int    j = 0;
                while (++i <= matcher.groupCount() && (uriVariable = matcher.group(i)) != null) {
                    String pathVariable = cleanPathVariable(mappingInfo.getVariableNames().get(j++));
                    uriVariables.put(pathVariable, uriVariable);
                }
                route.setPathParams(uriVariables);
                log.trace("lookup path: " + path + " uri variables: " + uriVariables);
            }
            return route;
        } catch (Exception e) {
            throw e;
        }
    }

    private String cleanPathVariable(String pathVariable) {
        if (pathVariable.contains(".")) {
            return pathVariable.substring(0, pathVariable.indexOf('.'));
        }
        return pathVariable;
    }

    public boolean hasBeforeHook() {
        return hooks.values().stream()
                .flatMap(Collection::stream).anyMatch(route -> route.getHttpMethod().equals(HttpMethod.BEFORE));
    }

    public boolean hasAfterHook() {
        return hooks.values().stream()
                .flatMap(Collection::stream).anyMatch(route -> route.getHttpMethod().equals(HttpMethod.AFTER));
    }

    /**
     * Find all in before of the hook
     *
     * @param path request path
     */
    public List<Route> getBefore(String path) {
        String cleanPath = parsePath(path);
        List<Route> collect = hooks.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparingInt(Route::getSort))
                .filter(route -> route.getHttpMethod() == HttpMethod.BEFORE && matchesPath(route.getPath(), cleanPath))
                .collect(Collectors.toList());

        this.giveMatch(path, collect);
        return collect;
    }

    /**
     * Find all in after of the hooks
     *
     * @param path request path
     */
    public List<Route> getAfter(String path) {
        String cleanPath = parsePath(path);

        List<Route> afters = hooks.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparingInt(Route::getSort))
                .filter(route -> route.getHttpMethod() == HttpMethod.AFTER && matchesPath(route.getPath(), cleanPath))
                .collect(Collectors.toList());

        this.giveMatch(path, afters);
        return afters;
    }

    public List<Route> getMiddleware() {
        return this.middleware;
    }

    /**
     * Sort of path
     *
     * @param uri    request uri
     * @param routes route list
     */
    private void giveMatch(final String uri, List<Route> routes) {
        routes.stream().sorted((o1, o2) -> {
            if (o2.getPath().equals(uri)) {
                return o2.getPath().indexOf(uri);
            }
            return -1;
        });
    }

    /**
     * Matching path
     *
     * @param routePath   route path
     * @param pathToMatch match path
     * @return return match is success
     */
    private boolean matchesPath(String routePath, String pathToMatch) {
        routePath = PathKit.VAR_REGEXP_PATTERN.matcher(routePath).replaceAll(PathKit.VAR_REPLACE);
        return pathToMatch.matches("(?i)" + routePath);
    }

    /**
     * Parse PathKit
     *
     * @param path route path
     * @return return parsed path
     */
    private String parsePath(String path) {
        path = PathKit.fixPath(path);
        try {
            URI uri = new URI(path);
            return uri.getPath();
        } catch (URISyntaxException e) {
            //log.error("parse [" + path + "] error", e);
            return path;
        }
    }

    /**
     * register route to container
     */
    public void register() {
        routes.values().forEach(route -> logAddRoute(log, route));
        hooks.values().stream().flatMap(Collection::stream).forEach(route -> logAddRoute(log, route));

        Stream.of(routes.values(), hooks.values().stream().findAny().orElse(new ArrayList<>()))
                .flatMap(Collection::stream).forEach(this::registerRoute);

        patternBuilders.keySet().stream()
                .filter(BladeKit::notIsWebHook)
                .forEach(httpMethod -> {
                    StringBuilder patternBuilder = patternBuilders.get(httpMethod);
                    if (patternBuilder.length() > 1) {
                        patternBuilder.setCharAt(patternBuilder.length() - 1, '$');
                    }
                    log.debug("Fast Route Method: {}, regex: {}", httpMethod, patternBuilder);
                    regexRoutePatterns.put(httpMethod, Pattern.compile(patternBuilder.toString()));
                });

        webSockets.forEach(path -> logWebSocket(log, path));
    }

    private void registerRoute(Route route) {
        String  path    = parsePath(route.getPath());
        Matcher matcher = null;
        if (path != null) {
            matcher = PATH_VARIABLE_PATTERN.matcher(path);
        }
        boolean      find             = false;
        List<String> uriVariableNames = new ArrayList<>();
        while (matcher != null && matcher.find()) {
            if (!find) {
                find = true;
            }
            String regexName  = matcher.group(1);
            String regexValue = matcher.group(2);

            // just a simple path param
            if (StringKit.isBlank(regexName)) {
                uriVariableNames.add(regexValue);
            } else {
                //regex path param
                uriVariableNames.add(regexName);
            }
        }
        HttpMethod httpMethod = route.getHttpMethod();
        if (find || BladeKit.isWebHook(httpMethod)) {
            if (regexRoutes.get(httpMethod) == null) {
                regexRoutes.put(httpMethod, new HashMap<>());
                patternBuilders.put(httpMethod, new StringBuilder("^"));
                indexes.put(httpMethod, 1);
            }
            int i = indexes.get(httpMethod);
            regexRoutes.get(httpMethod).put(i, new FastRouteMappingInfo(route, uriVariableNames));
            indexes.put(httpMethod, i + uriVariableNames.size() + 1);
            patternBuilders.get(httpMethod).append(new PathRegexBuilder().parsePath(path));
        } else {
            String routeKey = path + '#' + httpMethod.toString();
            staticRoutes.putIfAbsent(routeKey, route);
        }
    }

    public Map<String, Route> getRoutes() {
        return routes;
    }

    public List<String> getWebSockets() {
        return webSockets;
    }

    public Map<String, List<Route>> getHooks() {
        return hooks;
    }

    public Map<String, Route> getStaticRoutes() {
        return staticRoutes;
    }

    public void clear() {
        this.routes.clear();
        this.hooks.clear();
        this.classMethodPool.clear();
        this.controllerPool.clear();
        this.regexRoutePatterns.clear();
        this.staticRoutes.clear();
        this.regexRoutes.clear();
        this.indexes.clear();
        this.patternBuilders.clear();
    }

    public void initMiddleware(List<WebHook> hooks) {
        this.middleware = hooks.stream().map(webHook -> {
            Method method = ReflectKit.getMethod(WebHook.class, "before", Signature.class);
            return new Route(HttpMethod.BEFORE, "/.*", webHook, WebHook.class, method);
        }).collect(Collectors.toList());
    }

    public void addWebSocket(String path) {
        webSockets.add(path);
    }

    private class FastRouteMappingInfo {
        Route        route;
        List<String> variableNames;

        FastRouteMappingInfo(Route route, List<String> variableNames) {
            this.route = route;
            this.variableNames = variableNames;
        }

        public Route getRoute() {
            return route;
        }

        List<String> getVariableNames() {
            return variableNames;
        }
    }

}