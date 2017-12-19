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

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientResponse;
import reactor.ipc.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class NettyDeadlockTests {
	private static NettyContext nettyContext;

	int port;

	@BeforeClass
	public static void startReactor() {
		DeadlockHandler handler = new DeadlockHandler();
		HttpServer httpServer = HttpServer.create("localhost", 0);
		NettyDeadlockTests.nettyContext = httpServer.newHandler(handler).block();
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

	private void runLoginFailedTest() {
		String loginUrl = "http://localhost:" + this.port + "/login";
		HttpClient client = HttpClient.create(ops -> ops.connectAddress(() -> nettyContext.address()));

		HttpClientResponse loginResponse = client.get(loginUrl).block();

		assertThat(loginResponse.status()).isEqualTo(HttpResponseStatus.OK);

		HttpClientResponse loginFailed = client.post(loginUrl, request -> request
				.sendForm(
					form -> form
							.attr("username", "user")
							.attr("password", "invalid")
				)
				.then())
				.block();

		assertThat(loginFailed.status()).isEqualTo(HttpResponseStatus.OK);
		assertThat(loginFailed.receive().aggregate().asString().block()).contains("role=\"alert\"");
	}
}
