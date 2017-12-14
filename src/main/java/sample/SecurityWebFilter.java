package sample;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.Principal;

/**
 * @author Rob Winch
 */
@Component
public class SecurityWebFilter implements WebFilter {

	public static final String USERNAME = "username";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		if(path.equals("/login") || path.equals("/logout")) {
			return chain.filter(exchange);
		}

		return exchange.getSession()
				.flatMap(s -> Mono.justOrEmpty(s.getAttributes().get(USERNAME)))
				.cast(String.class)
				.switchIfEmpty(redirectToLogin(exchange).then(Mono.empty()))
				.flatMap(username -> authenticated(exchange, chain, username));
	}

	private Mono<Void> authenticated(ServerWebExchange exchange, WebFilterChain chain, String username) {
		Mono<Principal> principal = Mono.just(() -> username);
		ServerWebExchange authenticatedExchange = exchange.mutate().principal(principal).build();
		return chain.filter(authenticatedExchange);
	}

	private Mono<Void> redirectToLogin(ServerWebExchange exchange) {
		return Mono.defer(() -> {
			ServerHttpResponse response = exchange.getResponse();
			response.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
			response.getHeaders().setLocation(URI.create("/login"));
			return response.setComplete();
		});
	}
}
