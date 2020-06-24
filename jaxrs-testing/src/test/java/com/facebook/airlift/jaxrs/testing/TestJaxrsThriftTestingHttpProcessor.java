/*
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
package com.facebook.airlift.jaxrs.testing;

import com.facebook.airlift.http.client.HttpStatus;
import com.facebook.airlift.http.client.Request;
import com.facebook.airlift.http.client.testing.TestingHttpClient;
import com.facebook.airlift.http.client.thrift.ThriftBodyGenerator;
import com.facebook.airlift.http.client.thrift.ThriftResponseHandler;
import com.facebook.airlift.jaxrs.thrift.ThriftMapper;
import com.facebook.drift.codec.ThriftCodec;
import com.facebook.drift.codec.ThriftCodecManager;
import com.facebook.drift.codec.internal.compiler.CompilerThriftCodecFactory;
import com.facebook.drift.transport.netty.codec.Protocol;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import java.net.URI;

import static com.facebook.airlift.http.client.Request.Builder.prepareGet;
import static com.facebook.airlift.http.client.Request.Builder.preparePost;
import static com.facebook.airlift.http.client.thrift.ThriftRequestUtils.prepareThriftGet;
import static com.facebook.airlift.http.client.thrift.ThriftRequestUtils.prepareThriftPost;
import static com.facebook.drift.transport.netty.codec.Protocol.BINARY;
import static com.facebook.drift.transport.netty.codec.Protocol.COMPACT;
import static com.facebook.drift.transport.netty.codec.Protocol.FB_COMPACT;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test
public class TestJaxrsThriftTestingHttpProcessor
{
    private ThriftCodecManager codecManager;
    private TestingHttpClient httpCient;
    private ThriftCodec<TestThriftMessage> testThriftMessageThriftCodec;
    private ThriftResponseHandler<TestThriftMessage> testThriftMessageTestThriftResponseHandler;

    @BeforeClass
    public void setup()
    {
        codecManager = new ThriftCodecManager(new CompilerThriftCodecFactory(false));
        httpCient =
                new TestingHttpClient(new JaxrsTestingHttpProcessor(URI.create("http://fake.invalid/"), new GetItResource(), new ThriftMapper(codecManager)));
        testThriftMessageThriftCodec = codecManager.getCodec(TestThriftMessage.class);
        testThriftMessageTestThriftResponseHandler = new ThriftResponseHandler<>(testThriftMessageThriftCodec);
    }

    @AfterClass(alwaysRun = true)
    public void teardown()
    {
        codecManager = null;
        httpCient = null;
        testThriftMessageThriftCodec = null;
        testThriftMessageTestThriftResponseHandler = null;
    }

    @Test
    public void testPostCompact()
    {
        Request request = prepareThriftPost(COMPACT, new TestThriftMessage("xyz", 1), testThriftMessageThriftCodec)
                .setUri(URI.create("http://fake.invalid/http-thrift/post/2"))
                .build();

        ThriftResponseHandler.ThriftResponse response = httpCient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertTrue(response.getValue() instanceof TestThriftMessage);
        assertEquals(((TestThriftMessage) response.getValue()).getTestString(), "xyz");
        assertEquals(((TestThriftMessage) response.getValue()).getTestLong(), 3);
    }

    @Test
    public void testPostBinary()
    {
        Request request = prepareThriftPost(BINARY, new TestThriftMessage("xyz", 1), testThriftMessageThriftCodec)
                .setUri(URI.create("http://fake.invalid/http-thrift/post/2"))
                .build();

        ThriftResponseHandler.ThriftResponse response = httpCient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertTrue(response.getValue() instanceof TestThriftMessage);
        assertEquals(((TestThriftMessage) response.getValue()).getTestString(), "xyz");
        assertEquals(((TestThriftMessage) response.getValue()).getTestLong(), 3);
    }

    @Test
    public void testPostFBCompact()
    {
        Request request = prepareThriftPost(FB_COMPACT, new TestThriftMessage("xyz", 1), testThriftMessageThriftCodec)
                .setUri(URI.create("http://fake.invalid/http-thrift/post/2"))
                .build();

        ThriftResponseHandler.ThriftResponse response = httpCient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertTrue(response.getValue() instanceof TestThriftMessage);
        assertEquals(((TestThriftMessage) response.getValue()).getTestString(), "xyz");
        assertEquals(((TestThriftMessage) response.getValue()).getTestLong(), 3);
    }

    @Test
    public void testGetCompact()
    {
        Request request = prepareThriftGet(COMPACT)
                .setUri(URI.create("http://fake.invalid/http-thrift/get/2"))
                .build();

        ThriftResponseHandler.ThriftResponse response = httpCient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertTrue(response.getValue() instanceof TestThriftMessage);
        assertEquals(((TestThriftMessage) response.getValue()).getTestString(), "abc");
        assertEquals(((TestThriftMessage) response.getValue()).getTestLong(), 2);
    }

    @Test
    public void testGetBinary()
    {
        Request request = prepareThriftGet(BINARY)
                .setUri(URI.create("http://fake.invalid/http-thrift/get/2"))
                .build();

        ThriftResponseHandler.ThriftResponse response = httpCient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertTrue(response.getValue() instanceof TestThriftMessage);
        assertEquals(((TestThriftMessage) response.getValue()).getTestString(), "abc");
        assertEquals(((TestThriftMessage) response.getValue()).getTestLong(), 2);
    }

    @Test
    public void testGetFBCompact()
    {
        Request request = prepareThriftGet(FB_COMPACT)
                .setUri(URI.create("http://fake.invalid/http-thrift/get/2"))
                .build();

        ThriftResponseHandler.ThriftResponse response = httpCient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertTrue(response.getValue() instanceof TestThriftMessage);
        assertEquals(((TestThriftMessage) response.getValue()).getTestString(), "abc");
        assertEquals(((TestThriftMessage) response.getValue()).getTestLong(), 2);
    }

    @Test
    public void testInvalidFormat()
    {
        Request request = preparePost()
                .setHeader(ACCEPT, "application/x-thrift; t=nonbinary")
                .setHeader(CONTENT_TYPE, "application/x-thrift")
                .setBodyGenerator(createThriftBodyGenerator(COMPACT))
                .setUri(URI.create("http://fake.invalid/http-thrift/post/2"))
                .build();
        try {
            ThriftResponseHandler.ThriftResponse response = httpCient.execute(request, testThriftMessageTestThriftResponseHandler);
            fail("expected exception");
        }
        catch (Exception e) {
            assertEquals(e.getMessage(), "com.facebook.airlift.jaxrs.thrift.ThriftMapperParsingException: " +
                    "Invalid thrift input for Java type class com.facebook.airlift.jaxrs.testing.TestThriftMessage");
        }
    }

    private ThriftBodyGenerator createThriftBodyGenerator(Protocol protocol)
    {
        return new ThriftBodyGenerator(new TestThriftMessage("xyz", 1), testThriftMessageThriftCodec, protocol);
    }

    @Test
    public void testException()
    {
        Request request = prepareGet()
                .setUri(URI.create("http://fake.invalid/http-thrift/fail/testException"))
                .build();

        try {
            httpCient.execute(request, testThriftMessageTestThriftResponseHandler);
            fail("expected exception");
        }
        catch (TestingException e) {
            assertEquals(e.getMessage(), "testException");
        }
    }

    @Test
    public void testUndefinedResource()
    {
        Request request = prepareGet()
                .setUri(URI.create("http://fake.invalid/unknown"))
                .build();

        ThriftResponseHandler.ThriftResponse response = httpCient.execute(request, testThriftMessageTestThriftResponseHandler);
        assertEquals(response.getStatusCode(), 404);
    }

    @Path("http-thrift")
    public static class GetItResource
    {
        @Path("get/{id}")
        @GET
        @Produces("application/x-thrift")
        public TestThriftMessage getTestThriftMessage(@PathParam("id") long id)
        {
            return new TestThriftMessage("abc", id);
        }

        @Path("post/{id}")
        @POST
        @Consumes("application/x-thrift")
        @Produces("application/x-thrift")
        public TestThriftMessage postTestThriftMessage(@PathParam("id") long id, TestThriftMessage testThriftMessage)
        {
            return new TestThriftMessage(testThriftMessage.getTestString(), id + testThriftMessage.getTestLong());
        }

        @Path("fail/{message}")
        @GET
        @Consumes("application/x-thrift")
        @Produces("application/x-thrift")
        public TestThriftMessage fail(@PathParam("message") String errorMessage)
        {
            throw new TestingException(errorMessage);
        }
    }

    private static class TestingException
            extends RuntimeException
    {
        public TestingException(String message)
        {
            super(message);
        }
    }
}
