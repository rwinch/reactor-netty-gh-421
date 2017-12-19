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

import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;
import sample.webdriver.LoginPage;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class NettyDeadlockTests {
	private static NettyContext nettyContext;

	WebDriver driver;

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
		this.driver = new HtmlUnitDriver(BrowserVersion.CHROME);
	}

	@Test
	public void loginWhenInvalidUsernameThenError() throws Exception {
		LoginPage login = LoginPage.to(this.driver, this.port);
		login.assertAt();

		login
			.loginForm()
			.username("user")
			.password("invalid")
			.submit(LoginPage.class)
			.assertError();
	}

	@Test
	public void loginAndInvalidAgain() throws Exception {
		LoginPage login = LoginPage.to(this.driver, this.port);
		login.assertAt();

		login
				.loginForm()
				.username("user")
				.password("invalid")
				.submit(LoginPage.class)
				.assertError();

	}
}
