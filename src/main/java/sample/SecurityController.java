package sample;

import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 * @since 5.0
 */
@Controller
public class SecurityController {

	@PostMapping("/logout")
	String logout(WebSession session) {
		session.invalidate();
		return "redirect:/login?logout";
	}

	@PostMapping("/login")
	Mono<String> login(WebSession session, ServerWebExchange exchange) {
		return exchange.getFormData()
				.map(data -> login(session, data));
	}

	private String login(WebSession session, MultiValueMap<String, String> data) {
		String username = data.getFirst("username");
		String password = data.getFirst("password");
		if("user".equals(username) && "password".equals(password)) {
			session.getAttributes().put(SecurityWebFilter.USERNAME, username);
			return "redirect:/";
		}
		return "redirect:/login?error";
	}
}
