package sample;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.ServerFormLoginAuthenticationConverter;
import org.springframework.security.web.server.ServerHttpBasicAuthenticationConverter;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.function.Function;

/**
 * @author Rob Winch
 * @since 5.0
 */
class MockAuthenticationWebFilter implements WebFilter {

	private Function<ServerWebExchange, Mono<Authentication>> authenticationConverter = new ServerHttpBasicAuthenticationConverter();

	private ServerAuthenticationFailureHandler authenticationFailureHandler = new ServerAuthenticationEntryPointFailureHandler(new HttpBasicServerAuthenticationEntryPoint());

	private ServerWebExchangeMatcher requiresAuthenticationMatcher = ServerWebExchangeMatchers
			.anyExchange();

	public MockAuthenticationWebFilter() {
		setAuthenticationConverter(new ServerFormLoginAuthenticationConverter());
		setAuthenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/login?error"));
		setRequiresAuthenticationMatcher(new PathPatternParserServerWebExchangeMatcher("/login", HttpMethod.POST));
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return this.requiresAuthenticationMatcher.matches(exchange)
				.filter( matchResult -> matchResult.isMatch())
				.flatMap( matchResult -> this.authenticationConverter.apply(exchange))
				.switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
				.flatMap( token -> authenticate(exchange, chain, token));
	}

	private Mono<Void> authenticate(ServerWebExchange exchange,
			WebFilterChain chain, Authentication token) {
		WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain);
		return this.authenticate(token)
				.flatMap(authentication -> onAuthenticationSuccess(authentication, webFilterExchange))
				.onErrorResume(AuthenticationException.class, e -> this.authenticationFailureHandler
						.onAuthenticationFailure(webFilterExchange, e));
	}

	private Mono<Void> onAuthenticationSuccess(Authentication authentication, WebFilterExchange webFilterExchange) {
		ServerWebExchange exchange = webFilterExchange.getExchange();
		return Mono.defer(() -> {
				ServerHttpResponse response = exchange.getResponse();
				response.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
				response.getHeaders().setLocation(URI.create("/"));
				return response.setComplete();
			});
	}

	public void setAuthenticationConverter(Function<ServerWebExchange, Mono<Authentication>> authenticationConverter) {
		this.authenticationConverter = authenticationConverter;
	}

	public void setAuthenticationFailureHandler(
			ServerAuthenticationFailureHandler authenticationFailureHandler) {
		Assert.notNull(authenticationFailureHandler, "authenticationFailureHandler cannot be null");
		this.authenticationFailureHandler = authenticationFailureHandler;
	}

	public void setRequiresAuthenticationMatcher(
			ServerWebExchangeMatcher requiresAuthenticationMatcher) {
		Assert.notNull(requiresAuthenticationMatcher, "requiresAuthenticationMatcher cannot be null");
		this.requiresAuthenticationMatcher = requiresAuthenticationMatcher;
	}

	private Mono<Authentication> authenticate(Authentication authentication) {
		return Mono.just(User.withUsername(authentication.getName()).password("password").roles("USER").build())
				.publishOn(Schedulers.parallel())
				.filter( u -> u.getPassword().equals(authentication.getCredentials()))
				.switchIfEmpty(  Mono.error(new BadCredentialsException("Invalid Credentials")) )
				.map( u -> new UsernamePasswordAuthenticationToken(u, u.getPassword(), u.getAuthorities()) );
	}
}