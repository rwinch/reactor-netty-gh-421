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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Rob Winch
 * @since 5.0
 */
@Controller
public class IndexController {

	@GetMapping("/login")
	@ResponseBody
	public String login() {
		return login(false);
	}

	@GetMapping(path="/login",params = "error")
	@ResponseBody
	public String loginError() {
		return login(true);
	}

	private  String login(boolean error) {
		return "<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + "<head>\n"
				+ "\t<meta charset=\"utf-8\">\n"
				+ "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n"
				+ "\t<meta name=\"description\" content=\"\">\n"
				+ "\t<meta name=\"author\" content=\"\">\n"
				+ "\t<title>Please Log In</title>\n"
				+ "</head>\n" + "<body>\n" + "<div class=\"container\">\n"
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
		if(error) {
			return "\t\t<div class=\"alert alert-danger\" role=\"alert\">Invalid\n" + "\t\t\tusername and password.</div>\n";
		}
		return "";
	}
}
