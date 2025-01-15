package org.yiqixue.cards;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
/*@ComponentScans({ @ComponentScan("com.eazybytes.cards.controller") })
@EnableJpaRepositories("com.eazybytes.cards.repository")
@EntityScan("com.eazybytes.cards.model")*/
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@OpenAPIDefinition(
		info = @Info(
				title = "信用卡 微服务 REST API 文档",
				description = "Fuzzy银行信用卡微服务 REST API 文档",
				version = "v1",
				contact = @Contact(
						name = "李老师",
						email = "yiqixuespring@yiqixue.org",
						url = "http://www.yiqixue.org"
				),
				license = @License(
						name = "Apache 2.0",
						url = "http://www.yiqixue.org"
				)
		),
		externalDocs = @ExternalDocumentation(
				description = "Fuzzy银行 信用卡 微服务 REST API 文档",
				url = "http://locahost:9000/swagger-ui.html"
		)
)
public class CardsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardsApplication.class, args);
	}
}