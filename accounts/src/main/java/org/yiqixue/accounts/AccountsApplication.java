package org.yiqixue.accounts;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@OpenAPIDefinition(
		info = @Info(
				title="Accounts微服务 REST API 文档",
				description = "Accounts 微服务 REST API 文档说明",
				version="V1",
				contact=@Contact(
						name ="老李",
						email="laoli@yiqixue.org",
						url = "http://www.yiqixue.org"
				),
				license = @License(
						name="Apache 2.0",
						url="http://www.yiqixue.org"

				)
		),
		externalDocs = @ExternalDocumentation(
				description = "银行账号系统微服务文档",
				url = "http://www.yiqixue.org"
		)
)
@EnableFeignClients(basePackages = {"com.yuqixue.common.feign"})
public class AccountsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccountsApplication.class, args);
	}

}
