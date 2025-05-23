/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.airlift.http.server;

import com.facebook.airlift.configuration.ConfigurationFactory;
import com.facebook.airlift.configuration.ConfigurationModule;
import com.facebook.airlift.event.client.EventModule;
import com.facebook.airlift.event.client.InMemoryEventClient;
import com.facebook.airlift.event.client.InMemoryEventModule;
import com.facebook.airlift.http.client.HttpClient;
import com.facebook.airlift.http.client.HttpStatus;
import com.facebook.airlift.http.client.HttpUriBuilder;
import com.facebook.airlift.http.client.StatusResponseHandler.StatusResponse;
import com.facebook.airlift.http.client.StringResponseHandler.StringResponse;
import com.facebook.airlift.http.client.jetty.JettyHttpClient;
import com.facebook.airlift.log.Logging;
import com.facebook.airlift.node.NodeInfo;
import com.facebook.airlift.node.testing.TestingNodeModule;
import com.facebook.airlift.tracetoken.TraceTokenModule;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;
import com.google.common.net.MediaType;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.facebook.airlift.http.client.HttpUriBuilder.uriBuilderFrom;
import static com.facebook.airlift.http.client.Request.Builder.prepareGet;
import static com.facebook.airlift.http.client.Request.Builder.preparePost;
import static com.facebook.airlift.http.client.StaticBodyGenerator.createStaticBodyGenerator;
import static com.facebook.airlift.http.client.StatusResponseHandler.createStatusResponseHandler;
import static com.facebook.airlift.http.client.StringResponseHandler.createStringResponseHandler;
import static com.facebook.airlift.http.server.HttpServerBinder.httpServerBinder;
import static com.google.common.io.MoreFiles.deleteRecursively;
import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.common.net.HttpHeaders.REFERER;
import static com.google.common.net.HttpHeaders.USER_AGENT;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.io.OutputStream.nullOutputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.nCopies;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class TestHttpServerModule
{
    private File tempDir;

    @BeforeSuite
    public void setupSuite()
    {
        Logging.initialize();
    }

    @BeforeMethod
    public void setup()
            throws IOException
    {
        tempDir = Files.createTempDir().getCanonicalFile(); // getCanonicalFile needed to get around Issue 365 (http://code.google.com/p/guava-libraries/issues/detail?id=365)
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown()
            throws IOException
    {
        deleteRecursively(tempDir.toPath(), ALLOW_INSECURE);
    }

    @Test
    public void testCanConstructServer()
            throws Exception
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("node.environment", "test")
                .put("http-server.http.port", "0")
                .put("http-server.log.path", tempDir.toPath().resolve("http-request.log").toAbsolutePath().toString())
                .build();

        ConfigurationFactory configFactory = new ConfigurationFactory(properties);
        Injector injector = Guice.createInjector(new HttpServerModule(),
                new TestingNodeModule(),
                new ConfigurationModule(configFactory),
                new EventModule(),
                binder -> binder.bind(Servlet.class).annotatedWith(TheServlet.class).to(DummyServlet.class));

        HttpServer server = injector.getInstance(HttpServer.class);
        assertNotNull(server);
    }

    @Test
    public void testHttpServerUri()
            throws Exception
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("node.environment", "test")
                .put("http-server.http.port", "0")
                .put("http-server.log.path", tempDir.toPath().resolve("http-request.log").toAbsolutePath().toString())
                .build();

        ConfigurationFactory configFactory = new ConfigurationFactory(properties);
        Injector injector = Guice.createInjector(new HttpServerModule(),
                new TestingNodeModule(),
                new ConfigurationModule(configFactory),
                new EventModule(),
                binder -> binder.bind(Servlet.class).annotatedWith(TheServlet.class).to(DummyServlet.class));

        NodeInfo nodeInfo = injector.getInstance(NodeInfo.class);
        HttpServer server = injector.getInstance(HttpServer.class);
        assertNotNull(server);
        server.start();
        try {
            HttpServerInfo httpServerInfo = injector.getInstance(HttpServerInfo.class);
            assertNotNull(httpServerInfo);
            assertNotNull(httpServerInfo.getHttpUri());
            assertEquals(httpServerInfo.getHttpUri().getScheme(), "http");
            assertEquals(httpServerInfo.getHttpUri().getHost(), nodeInfo.getInternalAddress());
            assertNull(httpServerInfo.getHttpsUri());
        }
        catch (Exception e) {
            server.stop();
        }
    }

    @Test
    public void testServer()
            throws Exception
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("node.environment", "test")
                .put("http-server.http.port", "0")
                .put("http-server.log.path", tempDir.toPath().resolve("http-request.log").toAbsolutePath().toString())
                .build();

        ConfigurationFactory configFactory = new ConfigurationFactory(properties);
        Injector injector = Guice.createInjector(new HttpServerModule(),
                new TestingNodeModule(),
                new ConfigurationModule(configFactory),
                new EventModule(),
                binder -> {
                    binder.bind(Servlet.class).annotatedWith(TheServlet.class).to(DummyServlet.class);
                    newSetBinder(binder, Filter.class, TheServlet.class).addBinding().to(DummyFilter.class).in(Scopes.SINGLETON);
                    httpServerBinder(binder).bindResource("/", "webapp/user").withWelcomeFile("user-welcome.txt");
                    httpServerBinder(binder).bindResource("/", "webapp/user2")
                            .withExtraHeader("Content-Security-Policy", "frame-ancestors 'self'");
                    httpServerBinder(binder).bindResource("path", "webapp/user").withWelcomeFile("user-welcome.txt");
                    httpServerBinder(binder).bindResource("path", "webapp/user2");
                });

        HttpServerInfo httpServerInfo = injector.getInstance(HttpServerInfo.class);

        HttpServer server = injector.getInstance(HttpServer.class);
        server.start();

        try (HttpClient client = new JettyHttpClient()) {
            // test servlet bound correctly
            URI httpUri = httpServerInfo.getHttpUri();
            StatusResponse response = client.execute(prepareGet().setUri(httpUri).build(), createStatusResponseHandler());

            assertEquals(response.getStatusCode(), HttpServletResponse.SC_OK);

            // test filter bound correctly
            response = client.execute(prepareGet().setUri(httpUri.resolve("/filter")).build(), createStatusResponseHandler());

            assertEquals(response.getStatusCode(), HttpServletResponse.SC_PAYMENT_REQUIRED);
            Map<String, String> extraHeaders = new ImmutableMap.Builder<String, String>()
                    .put("Content-Security-Policy", "frame-ancestors 'self'")
                    .build();
            // test http resources
            assertResource(httpUri, client, "", "welcome user!");
            assertResource(httpUri, client, "user-welcome.txt", "welcome user!");
            assertResource(httpUri, client, "user.txt", "user");
            assertResource(httpUri, client, "user2.txt", "user2", extraHeaders);
            assertRedirect(httpUri, client, "path", "/path/");
            assertResource(httpUri, client, "path/", "welcome user!");
            assertResource(httpUri, client, "path/user-welcome.txt", "welcome user!");
            assertResource(httpUri, client, "path/user.txt", "user");
            assertResource(httpUri, client, "path/user2.txt", "user2");
        }
        finally {
            server.stop();
        }
    }

    private void assertResource(URI baseUri, HttpClient client, String path, String contents)
    {
        assertResource(baseUri, client, path, contents, ImmutableMap.of());
    }

    private void assertResource(URI baseUri, HttpClient client, String path, String contents, Map<String, String> hasHeaders)
    {
        HttpUriBuilder uriBuilder = uriBuilderFrom(baseUri);
        StringResponse response = client.execute(prepareGet().setUri(uriBuilder.appendPath(path).build()).build(), createStringResponseHandler());
        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        String contentType = response.getHeader(CONTENT_TYPE);
        assertNotNull(contentType, CONTENT_TYPE + " header is absent");
        MediaType mediaType = MediaType.parse(contentType);
        assertTrue(PLAIN_TEXT_UTF_8.is(mediaType), "Expected text/plain but got " + mediaType);
        assertEquals(response.getBody().trim(), contents);
        hasHeaders.forEach((name, value) -> {
            assertNotNull(response.getHeader(name), name + " header is absent");
            assertEquals(value, response.getHeader(name));
        });
    }

    private void assertRedirect(URI baseUri, HttpClient client, String path, String redirect)
    {
        HttpUriBuilder uriBuilder = uriBuilderFrom(baseUri);
        StringResponse response = client.execute(
                prepareGet()
                        .setFollowRedirects(false)
                        .setUri(uriBuilder.appendPath(path).build())
                        .build(),
                createStringResponseHandler());
        assertEquals(response.getStatusCode(), HttpStatus.TEMPORARY_REDIRECT.code());
        assertEquals(response.getHeader(LOCATION), redirect);
        assertNull(response.getHeader(CONTENT_TYPE), CONTENT_TYPE + " header should be absent");
        assertEquals(response.getBody(), "", "Response body");
    }

    @Test
    public void testHttpRequestEvent()
            throws Exception
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("node.environment", "test")
                .put("http-server.http.port", "0")
                .put("http-server.log.path", tempDir.toPath().resolve("http-request.log").toAbsolutePath().toString())
                .build();

        ConfigurationFactory configFactory = new ConfigurationFactory(properties);
        Injector injector = Guice.createInjector(new HttpServerModule(),
                new TestingNodeModule(),
                new ConfigurationModule(configFactory),
                new InMemoryEventModule(),
                new TraceTokenModule(),
                binder -> binder.bind(Servlet.class).annotatedWith(TheServlet.class).to(EchoServlet.class).in(Scopes.SINGLETON));

        HttpServerInfo httpServerInfo = injector.getInstance(HttpServerInfo.class);
        InMemoryEventClient eventClient = injector.getInstance(InMemoryEventClient.class);
        EchoServlet echoServlet = (EchoServlet) injector.getInstance(Key.get(Servlet.class, TheServlet.class));

        HttpServer server = injector.getInstance(HttpServer.class);
        server.start();

        URI requestUri = httpServerInfo.getHttpUri().resolve("/my/path");
        String userAgent = "my-user-agent";
        String referrer = "http://www.google.com";
        String token = "this is a trace token";
        String requestBody = Joiner.on(" ").join(nCopies(50, "request"));
        String requestContentType = "request/type";

        int responseCode = 555;
        String responseBody = Joiner.on(" ").join(nCopies(100, "response"));
        String responseContentType = "response/type";

        echoServlet.responseBody = responseBody;
        echoServlet.responseStatusCode = responseCode;
        echoServlet.responseHeaders.put("Content-Type", responseContentType);

        long beforeRequest = System.currentTimeMillis();
        long afterRequest;
        try (JettyHttpClient client = new JettyHttpClient()) {
            // test servlet bound correctly
            StringResponse response = client.execute(
                    preparePost().setUri(requestUri)
                            .addHeader(USER_AGENT, userAgent)
                            .addHeader(CONTENT_TYPE, requestContentType)
                            .addHeader(REFERER, referrer)
                            .addHeader("X-Airlift-TraceToken", token)
                            .setBodyGenerator(createStaticBodyGenerator(requestBody, UTF_8))
                            .build(),
                    createStringResponseHandler());

            afterRequest = System.currentTimeMillis();

            assertEquals(response.getStatusCode(), responseCode);
            assertEquals(response.getBody(), responseBody);
            assertEquals(response.getHeader("Content-Type"), responseContentType);
        }
        finally {
            server.stop();
        }

        List<Object> events = eventClient.getEvents();
        assertEquals(events.size(), 1);
        HttpRequestEvent event = (HttpRequestEvent) events.get(0);

        assertEquals(event.getClientAddress(), echoServlet.remoteAddress);
        assertEquals(event.getProtocol(), "http");
        assertEquals(event.getMethod(), "POST");
        assertEquals(event.getRequestUri(), requestUri.toString());
        assertNull(event.getUser());
        assertEquals(event.getAgent(), userAgent);
        assertEquals(event.getReferrer(), referrer);
        assertEquals(event.getTraceToken(), token);

        assertEquals(event.getRequestSize(), requestBody.length());
        assertEquals(event.getRequestContentType(), requestContentType);

        assertEquals(event.getResponseSize(), responseBody.length());
        assertEquals(event.getResponseCode(), responseCode);
        assertEquals(event.getResponseContentType(), responseContentType);

        assertTrue(event.getTimeStamp().toEpochMilli() >= beforeRequest);
        assertTrue(event.getTimeToLastByte() <= afterRequest - beforeRequest);
        assertNotNull(event.getTimeToFirstByte());
        assertTrue(event.getTimeToDispatch() <= event.getTimeToFirstByte());
        assertTrue(event.getTimeToFirstByte() <= event.getTimeToLastByte());
    }

    private static final class EchoServlet
            extends HttpServlet
    {
        private int responseStatusCode = 300;
        private final ListMultimap<String, String> responseHeaders = ArrayListMultimap.create();
        public String responseBody;
        private String remoteAddress;

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response)
                throws IOException
        {
            request.getInputStream().transferTo(nullOutputStream());

            remoteAddress = request.getRemoteAddr();
            for (Entry<String, String> entry : responseHeaders.entries()) {
                response.addHeader(entry.getKey(), entry.getValue());
            }

            response.setStatus(responseStatusCode);

            if (responseBody != null) {
                response.getOutputStream().write(responseBody.getBytes(UTF_8));
            }
        }
    }
}
