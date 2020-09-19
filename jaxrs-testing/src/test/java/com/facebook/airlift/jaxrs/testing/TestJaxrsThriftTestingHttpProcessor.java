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
import com.facebook.airlift.http.client.thrift.ThriftRequestUtils;
import com.facebook.airlift.http.client.thrift.ThriftResponse;
import com.facebook.airlift.http.client.thrift.ThriftResponseHandler;
import com.facebook.airlift.jaxrs.ParsingExceptionMapper;
import com.facebook.airlift.jaxrs.thrift.ThriftMapper;
import com.facebook.drift.codec.ThriftCodec;
import com.facebook.drift.codec.ThriftCodecManager;
import com.facebook.drift.codec.internal.compiler.CompilerThriftCodecFactory;
import com.facebook.drift.transport.netty.codec.Protocol;
import com.google.common.net.HttpHeaders;
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
import static com.facebook.airlift.http.client.thrift.ThriftRequestUtils.TYPE_BINARY;
import static com.facebook.airlift.http.client.thrift.ThriftRequestUtils.TYPE_COMPACT;
import static com.facebook.airlift.http.client.thrift.ThriftRequestUtils.TYPE_FBCOMPACT;
import static com.facebook.airlift.http.client.thrift.ThriftRequestUtils.prepareThriftGet;
import static com.facebook.airlift.http.client.thrift.ThriftRequestUtils.prepareThriftPost;
import static com.facebook.drift.protocol.TType.STOP;
import static com.facebook.drift.transport.netty.codec.Protocol.BINARY;
import static com.facebook.drift.transport.netty.codec.Protocol.COMPACT;
import static com.facebook.drift.transport.netty.codec.Protocol.FB_COMPACT;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test
public class TestJaxrsThriftTestingHttpProcessor
{
    private ThriftCodecManager codecManager;
    private TestingHttpClient httpClient;
    private ThriftCodec<TestThriftMessage> testThriftMessageThriftCodec;
    private ThriftResponseHandler<TestThriftMessage> testThriftMessageTestThriftResponseHandler;

    @BeforeClass
    public void setup()
    {
        codecManager = new ThriftCodecManager(new CompilerThriftCodecFactory(false));
        httpClient =
                new TestingHttpClient(
                        new JaxrsTestingHttpProcessor(
                            URI.create("http://fake.invalid/"),
                            new GetPostResource(),
                            new ThriftMapper(codecManager),
                            new ParsingExceptionMapper()));
        testThriftMessageThriftCodec = codecManager.getCodec(TestThriftMessage.class);
        testThriftMessageTestThriftResponseHandler = new ThriftResponseHandler<>(testThriftMessageThriftCodec);
    }

    @AfterClass(alwaysRun = true)
    public void teardown()
    {
        codecManager = null;
        httpClient = null;
        testThriftMessageThriftCodec = null;
        testThriftMessageTestThriftResponseHandler = null;
    }

    @Test
    public void testPostCompact()
    {
        Request request = prepareThriftPost(COMPACT, new TestThriftMessage("xyz", 1), testThriftMessageThriftCodec)
                .setUri(URI.create("http://fake.invalid/http-thrift/post/2"))
                .build();

        ThriftResponse<TestThriftMessage> response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertNotNull(response.getValue());
        assertEquals(response.getValue().getTestString(), "xyz");
        assertEquals(response.getValue().getTestLong(), 3);
    }

    @Test
    public void testPostBinary()
    {
        Request request = prepareThriftPost(BINARY, new TestThriftMessage("xyz", 1), testThriftMessageThriftCodec)
                .setUri(URI.create("http://fake.invalid/http-thrift/post/2"))
                .build();

        ThriftResponse<TestThriftMessage> response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertNotNull(response.getValue());
        assertEquals(response.getValue().getTestString(), "xyz");
        assertEquals(response.getValue().getTestLong(), 3);
    }

    @Test
    public void testPostFBCompact()
    {
        Request request = prepareThriftPost(FB_COMPACT, new TestThriftMessage("xyz", 1), testThriftMessageThriftCodec)
                .setUri(URI.create("http://fake.invalid/http-thrift/post/2"))
                .build();

        ThriftResponse<TestThriftMessage> response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertNotNull(response.getValue());
        assertEquals(response.getValue().getTestString(), "xyz");
        assertEquals(response.getValue().getTestLong(), 3);
    }

    @Test
    public void testGetCompact()
    {
        Request request = prepareThriftGet(COMPACT)
                .setUri(URI.create("http://fake.invalid/http-thrift/get/2"))
                .build();

        ThriftResponse<TestThriftMessage> response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertNotNull(response.getValue());
        assertEquals(response.getValue().getTestString(), "abc");
        assertEquals(response.getValue().getTestLong(), 2);
    }

    @Test
    public void testGetBinary()
    {
        Request request = prepareThriftGet(BINARY)
                .setUri(URI.create("http://fake.invalid/http-thrift/get/2"))
                .build();

        ThriftResponse<TestThriftMessage> response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertNotNull(response.getValue());
        assertEquals(response.getValue().getTestString(), "abc");
        assertEquals(response.getValue().getTestLong(), 2);
    }

    @Test
    public void testGetFBCompact()
    {
        Request request = prepareThriftGet(FB_COMPACT)
                .setUri(URI.create("http://fake.invalid/http-thrift/get/2"))
                .build();

        ThriftResponse<TestThriftMessage> response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertNotNull(response.getValue());
        assertEquals(response.getValue().getTestString(), "abc");
        assertEquals(response.getValue().getTestLong(), 2);
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
        ThriftResponse response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);
        assertEquals(response.getStatusCode(), HttpStatus.UNSUPPORTED_MEDIA_TYPE.code());
    }

    @Test
    public void testInvalidRequestBody()
    {
        Request request = preparePost()
                .setHeader(ACCEPT, ThriftRequestUtils.TYPE_COMPACT)
                .setHeader(HttpHeaders.CONTENT_TYPE, ThriftRequestUtils.TYPE_COMPACT)
                //Setting an invalid request body
                .setBodyGenerator(out -> out.write(new byte[] {'C', 'A', 'F', 'E', 'B', 'A', 'B', 'E', STOP}))
                .setUri(URI.create("http://fake.invalid/http-thrift/post/2"))
                .build();

        ThriftResponse<TestThriftMessage> response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);
        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST.code());
        assertTrue(response.getErrorMessage().startsWith("com.facebook.airlift.jaxrs.thrift.ThriftMapperParsingException"));
    }

    private ThriftBodyGenerator<TestThriftMessage> createThriftBodyGenerator(Protocol protocol)
    {
        return new ThriftBodyGenerator<>(new TestThriftMessage("xyz", 1), testThriftMessageThriftCodec, protocol);
    }

    @Test
    public void testException()
    {
        Request request = prepareGet()
                .setUri(URI.create("http://fake.invalid/http-thrift/fail/testException"))
                .build();

        try {
            httpClient.execute(request, testThriftMessageTestThriftResponseHandler);
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

        ThriftResponse<TestThriftMessage> response = httpClient.execute(request, testThriftMessageTestThriftResponseHandler);
        assertEquals(response.getStatusCode(), 404);
    }

    @Path("http-thrift")
    public static class GetPostResource
    {
        @Path("get/{id}")
        @GET
        @Produces({TYPE_BINARY, TYPE_COMPACT, TYPE_FBCOMPACT})
        public TestThriftMessage getTestMessage(@PathParam("id") long id)
        {
            return new TestThriftMessage("abc", id);
        }

        @Path("post/{id}")
        @POST
        @Consumes({TYPE_BINARY, TYPE_COMPACT, TYPE_FBCOMPACT})
        @Produces({TYPE_BINARY, TYPE_COMPACT, TYPE_FBCOMPACT})
        public TestThriftMessage postTestMessage(@PathParam("id") long id, TestThriftMessage testThriftMessage)
        {
            return new TestThriftMessage(testThriftMessage.getTestString(), id + testThriftMessage.getTestLong());
        }

        @Path("fail/{message}")
        @GET
        @Consumes({TYPE_BINARY, TYPE_COMPACT, TYPE_FBCOMPACT})
        @Produces({TYPE_BINARY, TYPE_COMPACT, TYPE_FBCOMPACT})
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
