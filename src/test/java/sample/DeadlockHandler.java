package sample;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

/**
 * @author Rob Winch
 * @since 5.0
 */
class DeadlockHandler
		implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

	@Override
	public Mono<Void> apply(HttpServerRequest request, HttpServerResponse response) {
		if (isLogin(request)) {
			return Mono.just(response)
					.publishOn(Schedulers.parallel())
					.flatMap(this::loginError);
		}
		return writeLoginErrorPage(response);
	}

	private Mono<Void> writeLoginErrorPage(HttpServerResponse response) {
		String loginErrorPage = loginErrorPage();
		ByteBuf buffer = response.alloc().buffer();
		buffer.writeBytes(loginErrorPage.getBytes(StandardCharsets.UTF_8));
		return response
				.status(HttpResponseStatus.OK)
				.addHeader(HttpHeaderNames.CONTENT_TYPE, "text/html")
				.send(Mono.just(buffer))
				.then();
	}

	private boolean isLogin(HttpServerRequest request) {
		return request.method().equals(HttpMethod.POST) &&
				"/login".equals(request.uri());
	}

	public Mono<Void> loginError(HttpServerResponse response) {
		return response
				.status(HttpResponseStatus.MOVED_PERMANENTLY)
				.header(HttpHeaderNames.LOCATION, "/login?error")
				.send();
	}

	private String loginErrorPage() {
		return "<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + "<head>\n"
				+ "\t<meta charset=\"utf-8\">\n"
				+ "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n"
				+ "\t<meta name=\"description\" content=\"\">\n"
				+ "\t<meta name=\"author\" content=\"\">\n"
				+ "\t<title>Please Log In</title>\n" + "</head>\n" + "<body>\n" + "<div class=\"container\">\n"
				+ "\t<form class=\"form-signin\" method=\"post\" action=\"/login\">\n"
				+ "\t\t<h2 class=\"form-signin-heading\">Please Log In</h2>\n"
				+ "\t\t<div class=\"alert alert-danger\" role=\"alert\">Invalid\n" + "\t\t\tusername and password.</div>\n"
				+ "\t\t\t<label for=\"username\" class=\"sr-only\">Username</label>\n"
				+ "\t\t\t<input type=\"text\" id=\"username\" name=\"username\" class=\"form-control\" placeholder=\"Username\" required autofocus>\n"
				+ "\t\t</p>\n" + "\t\t<p>\n"
				+ "\t\t\t<label for=\"password\" class=\"sr-only\">Password</label>\n"
				+ "\t\t\t<input type=\"password\" id=\"password\" name=\"password\" class=\"form-control\" placeholder=\"Password\" required>\n"
				+ "\t\t</p>\n"
				+ "\t\t<button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Sign in</button>\n"
				+ "\t</form>\n" + "</div>\n" + "</body>\n" + "</html>";
	}
}
