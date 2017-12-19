package sample;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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
class DeadlockHandler
		implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

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

		return this.handle(adaptedRequest, adaptedResponse).onErrorResume(ex -> {
			logger.error("Could not complete request", ex);
			response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			return Mono.empty();
		}).doOnSuccess(aVoid -> logger.debug("Successfully completed request"));
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
		Class<String> type = String.class;
		ResolvableType resolvableType = ResolvableType.forType(type);
		Flux<DataBuffer> encoded = encoder
				.encode(result, response.bufferFactory(), resolvableType, MimeType.valueOf("text/html"),
						Collections.emptyMap());
		return response.writeWith(encoded);
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
