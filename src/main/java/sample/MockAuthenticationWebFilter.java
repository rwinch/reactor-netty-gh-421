package sample;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

/**
 * @author Rob Winch
 */
class MockAuthenticationWebFilter implements WebFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if(isLogin(request)) {
			return Mono.just(exchange)
					.publishOn(Schedulers.parallel())
					.map(ServerWebExchange::getResponse)
					.flatMap(this::redirect);
		}
		return chain.filter(exchange);
	}

	private boolean isLogin(ServerHttpRequest request) {
		return request.getMethod().equals(HttpMethod.POST) && "/login".equals(request.getPath().pathWithinApplication().value());
	}

	private Mono<Void> redirect(ServerHttpResponse response) {
		return Mono.defer(() -> {
			response.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
			response.getHeaders().setLocation(URI.create("/login?error"));
			return response.setComplete();
		});
	}
}