package sample;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class MockUserDetailsRepositoryReactiveAuthenticationManager implements
		ReactiveAuthenticationManager {
	private final ReactiveUserDetailsService repository;

	public MockUserDetailsRepositoryReactiveAuthenticationManager(ReactiveUserDetailsService reactiveUserDetailsService) {
		Assert.notNull(reactiveUserDetailsService, "userDetailsRepository cannot be null");
		this.repository = reactiveUserDetailsService;
	}

	@Override
	public Mono<Authentication> authenticate(Authentication authentication) {
		final String username = authentication.getName();
		return this.repository.findByUsername(username)
				.publishOn(Schedulers.parallel())
				.filter( u -> u.getPassword().equals(authentication.getCredentials()))
				.switchIfEmpty(  Mono.error(new BadCredentialsException("Invalid Credentials")) )
				.map( u -> new UsernamePasswordAuthenticationToken(u, u.getPassword(), u.getAuthorities()) );
	}
}
