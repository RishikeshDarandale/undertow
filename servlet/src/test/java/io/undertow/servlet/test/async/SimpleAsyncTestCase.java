/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.undertow.servlet.test.async;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.test.util.DeploymentUtils;
import io.undertow.servlet.test.util.MessageServlet;
import io.undertow.testutils.DefaultServer;
import io.undertow.testutils.HttpClientUtils;
import io.undertow.testutils.TestHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;

import static io.undertow.servlet.Servlets.servlet;

/**
 * @author Stuart Douglas
 */
@RunWith(DefaultServer.class)
public class SimpleAsyncTestCase {

    public static final String HELLO_WORLD = "Hello World";

    @BeforeClass
    public static void setup() throws ServletException {
        DeploymentUtils.setupServlet(new ServletExtension() {
            @Override
            public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
                deploymentInfo.addErrorPages(new ErrorPage("/500", 500));
            }
        },
                servlet("messageServlet", MessageServlet.class)
                        .addInitParam(MessageServlet.MESSAGE, HELLO_WORLD)
                        .setAsyncSupported(true)
                        .addMapping("/message"),
                servlet("500", MessageServlet.class)
                        .addInitParam(MessageServlet.MESSAGE, "500")
                        .setAsyncSupported(true)
                        .addMapping("/500"),
                servlet("asyncServlet", AsyncServlet.class)
                        .addInitParam(MessageServlet.MESSAGE, HELLO_WORLD)
                        .setAsyncSupported(true)
                        .addMapping("/async"),
                servlet("asyncServlet2", AnotherAsyncServlet.class)
                        .setAsyncSupported(true)
                        .addMapping("/async2"),
                servlet("error", AsyncErrorServlet.class)
                        .setAsyncSupported(true)
                        .addMapping("/error"));

    }

    @Test
    public void testSimpleHttpServlet() throws IOException {
        TestHttpClient client = new TestHttpClient();
        try {
            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/servletContext/async");
            HttpResponse result = client.execute(get);
            Assert.assertEquals(200, result.getStatusLine().getStatusCode());
            final String response = HttpClientUtils.readResponse(result);
            Assert.assertEquals(HELLO_WORLD, response);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void testSimpleHttpAsyncServletWithoutDispatch() throws IOException {
        TestHttpClient client = new TestHttpClient();
        try {
            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/servletContext/async2");
            HttpResponse result = client.execute(get);
            Assert.assertEquals(200, result.getStatusLine().getStatusCode());
            final String response = HttpClientUtils.readResponse(result);
            Assert.assertEquals(AnotherAsyncServlet.class.getSimpleName(), response);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void testErrorServlet() throws IOException {
        TestHttpClient client = new TestHttpClient();
        try {
            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/servletContext/error");
            HttpResponse result = client.execute(get);
            Assert.assertEquals(500, result.getStatusLine().getStatusCode());
            final String response = HttpClientUtils.readResponse(result);
            Assert.assertEquals("500", response);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void testErrorServletWithPostData() throws IOException {
        TestHttpClient client = new TestHttpClient();
        try {
            HttpPost post = new HttpPost(DefaultServer.getDefaultServerURL() + "/servletContext/error");
            post.setEntity(new StringEntity("Post body stuff"));
            HttpResponse result = client.execute(post);
            Assert.assertEquals(500, result.getStatusLine().getStatusCode());
            String response = HttpClientUtils.readResponse(result);
            Assert.assertEquals("500", response);

            post = new HttpPost(DefaultServer.getDefaultServerURL() + "/servletContext/error");
            post.setEntity(new StringEntity("Post body stuff"));
            result = client.execute(post);
            Assert.assertEquals(500, result.getStatusLine().getStatusCode());
            response = HttpClientUtils.readResponse(result);
            Assert.assertEquals("500", response);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

}
