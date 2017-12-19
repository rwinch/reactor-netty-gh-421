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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.function.BiFunction;

/**
 * @author Rob Winch
 * @since 5.0
 */
@Configuration
@EnableWebFlux
@ComponentScan
public class WebfluxFormApplication {
	@Value("${server.port:8080}")
	private int port = 8080;

	public static void main(String[] args) throws Exception {
		try(AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
			WebfluxFormApplication.class)) {
			context.getBean(NettyContext.class).onClose().block();
		}
	}

	@Profile("default")
	@Bean
	public NettyContext nettyContext(ApplicationContext context) {
		DeadlockHandler handler = new DeadlockHandler();
		HttpServer httpServer = HttpServer.create("localhost", port);
		return httpServer.newHandler(handler).block();
	}

	static class DeadlockHandler implements
			BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

		private static final Log logger = LogFactory.getLog(DeadlockHandler.class);

		@Override
		public Mono<Void> apply(HttpServerRequest request, HttpServerResponse response) {

			NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(response.alloc());
			ServerHttpRequest adaptedRequest;
			ServerHttpResponse adaptedResponse;
			try {
				adaptedRequest = new ReactorServerHttpRequest(request, bufferFactory);
				adaptedResponse = new ReactorServerHttpResponse(response, bufferFactory);
			}
			catch (URISyntaxException ex) {
				logger.error("Invalid URL " + ex.getMessage(), ex);
				response.status(HttpResponseStatus.BAD_REQUEST);
				return Mono.empty();
			}

			if (HttpMethod.HEAD.equals(adaptedRequest.getMethod())) {
				adaptedResponse = new HttpHeadResponseDecorator(adaptedResponse);
			}

			return this.handle(adaptedRequest, adaptedResponse)
					.onErrorResume(ex -> {
						logger.error("Could not complete request", ex);
						response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
						return Mono.empty();
					})
					.doOnSuccess(aVoid -> logger.debug("Successfully completed request"));
		}

		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			if (isLogin(request)) {
				return Mono.just(response).publishOn(Schedulers.parallel())
						.flatMap(this::redirect);
			}
			boolean error = request.getQueryParams().containsKey("error");
			return write(error, request, response);
		}

		private Mono<Void> write(boolean error, ServerHttpRequest request,
				ServerHttpResponse response) {
			Mono<String> result = Mono.justOrEmpty(login(error));
			CharSequenceEncoder encoder = CharSequenceEncoder.allMimeTypes();
			EncoderHttpMessageWriter writer = new EncoderHttpMessageWriter(encoder);
			Class<String> type = String.class;
			ResolvableType resolvableType = ResolvableType.forType(type);
			return writer.write(result, resolvableType, resolvableType, MediaType.TEXT_HTML,
					request, response, Collections.emptyMap());
		}

		private boolean isLogin(ServerHttpRequest request) {
			return request.getMethod().equals(HttpMethod.POST) && "/login"
					.equals(request.getPath().pathWithinApplication().value());
		}

		public Mono<Void> redirect(ServerHttpResponse response) {
			return Mono.defer(() -> {
				response.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
				response.getHeaders().setLocation(URI.create("/login?error"));
				return response.setComplete();
			});
		}

		private String login(boolean error) {
			return "<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + "<head>\n"
					+ "\t<meta charset=\"utf-8\">\n"
					+ "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n"
					+ "\t<meta name=\"description\" content=\"\">\n"
					+ "\t<meta name=\"author\" content=\"\">\n"
					+ "\t<title>Please Log In</title>\n" + "</head>\n" + "<body>\n" + "<div class=\"container\">\n"
					+ "\t<form class=\"form-signin\" method=\"post\" action=\"/login\">\n"
					+ "\t\t<h2 class=\"form-signin-heading\">Please Log In</h2>\n"
					+ error(error)
					+ "\t\t\t<label for=\"username\" class=\"sr-only\">Username</label>\n"
					+ "\t\t\t<input type=\"text\" id=\"username\" name=\"username\" class=\"form-control\" placeholder=\"Username\" required autofocus>\n"
					+ "\t\t</p>\n" + "\t\t<p>\n"
					+ "\t\t\t<label for=\"password\" class=\"sr-only\">Password</label>\n"
					+ "\t\t\t<input type=\"password\" id=\"password\" name=\"password\" class=\"form-control\" placeholder=\"Password\" required>\n"
					+ "\t\t</p>\n"
					+ "\t\t<button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Sign in</button>\n"
					+ "\t</form>\n" + "</div>\n" + "</body>\n" + "</html>";
		}

		private String error(boolean error) {
			if (error) {
				return "\t\t<div class=\"alert alert-danger\" role=\"alert\">Invalid\n" + "\t\t\tusername and password.</div>\n";
			}
			return "";
		}
	}
}
