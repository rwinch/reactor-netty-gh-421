/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class NettyHangsTests {
	private static NettyContext nettyContext;

	int port;

	@BeforeClass
	public static void startReactor() {
		DeadlockHandler handler = new DeadlockHandler();
		HttpServer httpServer = HttpServer.create("localhost", 0);
		NettyHangsTests.nettyContext = httpServer.newHandler(handler).block();
	}

	@AfterClass
	public static void shutdownReactor() {
		if(nettyContext != null) {
			nettyContext.dispose();
		}
	}

	@Before
	public void setup() {
		this.port = nettyContext.address().getPort();
	}

	@Test
	public void loginWhenInvalidUsernameThenError() throws Exception {
		runLoginFailedTest();
	}

	@Test
	public void loginAndInvalidAgain() throws Exception {
		runLoginFailedTest();
	}

	private void runLoginFailedTest() throws Exception {
		int timeoutInMs = 15000;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeoutInMs)
				.setConnectionRequestTimeout(timeoutInMs)
				.setSocketTimeout(timeoutInMs)
				.build();
		HttpHost host = new HttpHost("localhost", this.port, "http");
		CloseableHttpClient client = HttpClientBuilder.create()
				.setDefaultRequestConfig(config)
				.setRedirectStrategy(new DefaultRedirectStrategy())
				.build();
		HttpRequest context = new HttpGet("/login");
		String body = EntityUtils.toString(client.execute(host, context).getEntity());
		assertThat(body).contains("Please Log In");

		HttpPost login = new HttpPost("/login");
		login.setEntity(new UrlEncodedFormEntity(Arrays.asList(
				new BasicNameValuePair("username", "user"),
				new BasicNameValuePair("password", "invalid")
		)));

		CloseableHttpResponse loginFailed = client.execute(host, login);
		Header location = loginFailed.getFirstHeader("location");

		loginFailed = client.execute(host,
				new HttpGet(location.getValue()));
		assertThat(EntityUtils.toString(loginFailed.getEntity())).contains("role=\"alert\"");

	}
}
