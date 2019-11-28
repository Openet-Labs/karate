package com.intuit.karate;

import com.google.common.base.Charsets;
import com.intuit.karate.netty.FeatureServer;
import com.intuit.karate.netty.ProxyServer;
import java.io.File;
import java.io.InputStream;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class ProxyServerTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ProxyServerTest.class);

    private static ProxyServer proxy;
    private static FeatureServer server;

    @BeforeClass
    public static void beforeClass() {
        proxy = new ProxyServer(0, null, null);
        File file = FileUtils.getFileRelativeTo(ProxyServerTest.class, "server.feature");
        server = FeatureServer.start(file, 0, false, null);
        int port = server.getPort();
        System.setProperty("karate.server.port", port + "");
        System.setProperty("karate.server.ssl", ""); // for ci
        System.setProperty("karate.server.proxy", "http://localhost:" + proxy.getPort());
    }

    @AfterClass
    public static void afterClass() {
        server.stop();
        proxy.stop();
    }

    @Test
    public void testProxy() throws Exception {
        String url = "http://localhost:" + server.getPort() + "/v1/cats";
        assertEquals(200, http(get(url)));
        assertEquals(200, http(post(url, "{ \"name\": \"Billie\" }")));
        Runner.runFeature("classpath:com/intuit/karate/client.feature", null, true);
    }

    private static HttpUriRequest get(String url) {
        return new HttpGet(url);
    }

    private static HttpUriRequest post(String url, String body) {
        HttpPost post = new HttpPost(url);
        HttpEntity entity = new StringEntity(body, ContentType.create("application/json", Charsets.UTF_8));
        post.setEntity(entity);
        return post;
    }

    private static int http(HttpUriRequest request) throws Exception {
        CloseableHttpClient client = HttpClients.custom()
                .setProxy(new HttpHost("localhost", proxy.getPort()))
                .build();
        CloseableHttpResponse response = client.execute(request);
        InputStream is = response.getEntity().getContent();
        String responseString = FileUtils.toString(is);
        logger.debug("response: {}", responseString);
        return response.getCode();
    }

}
