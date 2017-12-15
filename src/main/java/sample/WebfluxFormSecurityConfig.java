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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.web.server.MatcherSecurityWebFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerFormLoginAuthenticationConverter;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.authorization.AuthorizationWebFilter;
import org.springframework.security.web.server.authorization.DelegatingReactiveAuthorizationManager;
import org.springframework.security.web.server.authorization.ExceptionTranslationWebFilter;
import org.springframework.security.web.server.context.ReactorContextWebFilter;
import org.springframework.security.web.server.context.SecurityContextServerWebExchangeWebFilter;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcherEntry;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * @author Rob Winch
 * @since 5.0
 */
@Configuration
public class WebfluxFormSecurityConfig {

	@Bean
	WebFilter springSecurity() {
		ServerSecurityContextRepository securityContextRepository = new WebSessionServerSecurityContextRepository();
		ReactorContextWebFilter reactor = new ReactorContextWebFilter(securityContextRepository);

		WebFilter authentication = authentication();

		ExceptionTranslationWebFilter exception = new ExceptionTranslationWebFilter();
		exception.setAuthenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/login"));

		AuthorizationWebFilter authorization = authorization();

		return webFilterChainProxy(reactor, authentication, exception, authorization);
	}

	private AuthorizationWebFilter authorization() {
		PathPatternParserServerWebExchangeMatcher loginMatcher = new PathPatternParserServerWebExchangeMatcher("/login");
		ReactiveAuthorizationManager permitAll = (a, e) -> Mono
				.just(new AuthorizationDecision(true));
		DelegatingReactiveAuthorizationManager delegateAuthz = DelegatingReactiveAuthorizationManager.builder()
				.add(new ServerWebExchangeMatcherEntry(loginMatcher, permitAll))
				.add(new ServerWebExchangeMatcherEntry(ServerWebExchangeMatchers.anyExchange(), AuthenticatedReactiveAuthorizationManager
						.authenticated()))
				.build();

		return new AuthorizationWebFilter(delegateAuthz);
	}

	private WebFilter authentication() {
		return new MockAuthenticationWebFilter();
	}

	private WebFilter webFilterChainProxy(WebFilter... filters) {
		List<WebFilter> webFilters = Arrays.asList(filters);
		SecurityWebFilterChain chain = new MatcherSecurityWebFilterChain(
				ServerWebExchangeMatchers.anyExchange(), webFilters);
		return new WebFilterChainProxy(chain);
	}

	private RedirectServerAuthenticationSuccessHandler successHandler() {
		RedirectServerAuthenticationSuccessHandler successHandler = new RedirectServerAuthenticationSuccessHandler();
		successHandler.setRequestCache(NoOpServerRequestCache.getInstance());
		return successHandler;
	}
}
