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

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.resources.PoolResources;

import java.time.Duration;

/**
 * @author Rob Winch
 */
public class ReactorNettyHangTests {

	@Test
	public void deadlockWhenRedirectsToSameUrl(){
		redirectTests("/login");
	}

	@Test
	public void okWhenRedirectsToOther(){
		redirectTests("/other");
	}

	public void redirectTests(String url) {
		NettyContext server = HttpServer.create(9999)
				.newHandler((req, res) -> {
					if (req.uri()
							.contains("/login") && req.method().equals(HttpMethod.POST)) {
						return res.sendRedirect
								("http://localhost:9999" + url);
					}
					else {
						return res.status(200)
								.send();
					}
				})
				.block(Duration.ofSeconds(300));

		PoolResources pool = PoolResources.fixed("test", 1);

		HttpClient client =
				HttpClient.create(ops -> ops.connectAddress(() -> server.address())
						.poolResources(pool));

		try {
			Flux.range(1, 1000)
					.concatMap(i -> client.post("/login", r -> r.followRedirect() )
							.flatMap(r -> r.receive().then()))
					.blockLast();
		}
		finally {
			server.dispose();
		}

	}
}
