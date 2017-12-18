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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;
import sample.webdriver.IndexPage;
import sample.webdriver.LoginPage;

import java.time.Duration;

/**
 * @author Rob Winch
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = WebfluxFormApplication.class)
@TestPropertySource(properties = "server.port=0")
public class WebfluxFormApplicationTests {
	WebDriver driver;

	NettyContext netty = netty();

	int port = this.netty.address().getPort();

	@Before
	public void setup() {
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

	private static NettyContext netty() {
		return HttpServer.create(0)
				.newHandler((req, res) -> {
					if (req.uri()
							.contains("/login") && req.method().equals(HttpMethod.POST)) {
						return Mono.just(res)
								.publishOn(Schedulers.parallel())
								.flatMap(r -> r.sendRedirect("http://localhost:9999/login"));
					}
					else {
						return res.status(200)
								.sendByteArray(content());
					}
				})
				.block(Duration.ofSeconds(300));
	}

	private static Mono<byte[]> content() {
		String body = "<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + "<head>\n"
				+ "\t<meta charset=\"utf-8\">\n"
				+ "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n"
				+ "\t<meta name=\"description\" content=\"\">\n"
				+ "\t<meta name=\"author\" content=\"\">\n"
				+ "\t<title>Please Log In</title>\n"
				+ "\t<link href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-/Y6pD6FV/Vv2HJnA6t+vslU6fwYXjCFtcEpHbNJ0lyAFsXTsjBbfaDjzALeQsN6M\" crossorigin=\"anonymous\">\n"
				+ "\t<link href=\"http://getbootstrap.com/docs/4.0/examples/signin/signin.css\" rel=\"stylesheet\" crossorigin=\"anonymous\"/>\n"
				+ "</head>\n" + "<body>\n" + "<div class=\"container\">\n"
				+ "\t<form class=\"form-signin\" method=\"post\" action=\"/login\">\n"
				+ "\t\t<h2 class=\"form-signin-heading\">Please Log In</h2>\n"
				+ error(true)
				+ "\t\t\t<label for=\"username\" class=\"sr-only\">Username</label>\n"
				+ "\t\t\t<input type=\"text\" id=\"username\" name=\"username\" class=\"form-control\" placeholder=\"Username\" required autofocus>\n"
				+ "\t\t</p>\n" + "\t\t<p>\n"
				+ "\t\t\t<label for=\"password\" class=\"sr-only\">Password</label>\n"
				+ "\t\t\t<input type=\"password\" id=\"password\" name=\"password\" class=\"form-control\" placeholder=\"Password\" required>\n"
				+ "\t\t</p>\n"
				+ "\t\t<button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Sign in</button>\n"
				+ "\t</form>\n" + "</div>\n" + "</body>\n" + "</html>";

		return Mono.just(body.getBytes());
	}

	private static String error(boolean error) {
		if(error) {
			return "\t\t<div class=\"alert alert-danger\" role=\"alert\">Invalid\n" + "\t\t\tusername and password.</div>\n";
		}
		return "";
	}
}
