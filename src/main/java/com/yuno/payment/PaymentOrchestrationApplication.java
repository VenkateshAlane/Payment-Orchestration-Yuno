package com.yuno.payment;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title       = "Payment Orchestration API",
        version     = "1.0",
        description = "Processes payments via multiple providers with idempotency, retry, and failover.",
        contact     = @Contact(name = "Yuno Engineering", email = "engineering@yuno.dev")
    )
)
public class PaymentOrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestrationApplication.class, args);
    }
}
