package org.continuity.wessbas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Henning Schulz
 *
 */
@Controller
@EnableAutoConfiguration
public class WessbasApplication {

	@RequestMapping("/hello")
	@ResponseBody
	public String hello(String name) {
		return "Hello " + name + "!";
	}

	public static void main(String[] args) {
		SpringApplication.run(WessbasApplication.class, args);
	}

}
