package com.facebook.airlift.jaxrs.testing;

import com.facebook.airlift.http.client.HttpStatus;
import com.facebook.airlift.http.client.Request;
import com.facebook.airlift.http.client.StringResponseHandler.StringResponse;
import com.facebook.airlift.http.client.testing.TestingHttpClient;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.testng.annotations.Test;

import java.net.URI;

import static com.facebook.airlift.http.client.Request.Builder.prepareGet;
import static com.facebook.airlift.http.client.StringResponseHandler.createStringResponseHandler;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class TestJaxrsTestingHttpProcessor
{
    private static final TestingHttpClient HTTP_CLIENT =
            new TestingHttpClient(new JaxrsTestingHttpProcessor(URI.create("http://fake.invalid/"), new GetItResource()));

    @Test
    public void test()
    {
        Request request = prepareGet()
                .setUri(URI.create("http://fake.invalid/get-it/get/xyz"))
                .build();

        StringResponse response = HTTP_CLIENT.execute(request, createStringResponseHandler());

        assertEquals(response.getStatusCode(), HttpStatus.OK.code());
        assertEquals(response.getBody(), "Got xyz");
    }

    @Test
    public void testException()
    {
        Request request = prepareGet()
                .setUri(URI.create("http://fake.invalid/get-it/fail/testException"))
                .build();

        try {
            HTTP_CLIENT.execute(request, createStringResponseHandler());
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

        StringResponse response = HTTP_CLIENT.execute(request, createStringResponseHandler());
        assertEquals(response.getStatusCode(), 404);
    }

    @Path("get-it")
    public static class GetItResource
    {
        @Path("get/{id}")
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String getId(@PathParam("id") String id)
        {
            return format("Got %s", id);
        }

        @Path("fail/{message}")
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String fail(@PathParam("message") String errorMessage)
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
