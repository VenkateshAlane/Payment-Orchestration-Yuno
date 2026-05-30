package com.yuno.payment.infrastructure.web;

import com.yuno.payment.application.command.CreatePaymentCommand;
import com.yuno.payment.application.command.GetPaymentQuery;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.PaymentId;
import com.yuno.payment.domain.port.inbound.CreatePaymentUseCase;
import com.yuno.payment.domain.port.inbound.GetPaymentUseCase;
import com.yuno.payment.infrastructure.web.dto.CreatePaymentRequest;
import com.yuno.payment.infrastructure.web.dto.PaymentHttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Inbound adapter: translates HTTP requests into use-case commands.
 *
 * Responsibilities:
 *   - Extract and validate the Idempotency-Key header
 *   - Deserialize and validate the request body (via @Valid)
 *   - Delegate to the appropriate use case
 *   - Map the result to an HTTP response
 *
 * The controller has no business logic. It knows nothing about routing,
 * providers, retries, or the domain model.
 */
@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Payment creation and retrieval")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final int    MAX_IDEMPOTENCY_KEY_LENGTH = 255;

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final PaymentMapper mapper;

    public PaymentController(CreatePaymentUseCase createPaymentUseCase,
                             GetPaymentUseCase getPaymentUseCase,
                             PaymentMapper mapper) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.getPaymentUseCase    = getPaymentUseCase;
        this.mapper               = mapper;
    }

    @Operation(summary = "Create a payment",
               description = "Initiates a payment. Idempotent: repeat with the same Idempotency-Key to receive the original result.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created"),
        @ApiResponse(responseCode = "400", description = "Validation error or missing header"),
        @ApiResponse(responseCode = "422", description = "All providers failed")
    })
    @PostMapping
    public ResponseEntity<PaymentHttpResponse> createPayment(
            @Parameter(description = "Client-generated unique key (max 255 chars). UUID recommended.", required = true)
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key must not be blank");
        }
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must not exceed " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }

        // MDC is seeded by PaymentTraceFilter — no need to set it here
        log.info("POST /payments method={} amount={} currency={}",
                request.method(), request.amount(), request.currency());

        CreatePaymentCommand command = mapper.toCommand(request, idempotencyKey);
        PaymentResult result = createPaymentUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toHttpResponse(result));
    }

    @Operation(summary = "Get a payment by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "400", description = "Malformed payment ID"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentHttpResponse> getPayment(
            @Parameter(description = "Payment UUID", required = true)
            @PathVariable String id) {
        log.info("GET /payments/{}", id);
        PaymentId paymentId = PaymentId.of(id); // throws IllegalArgumentException on bad format
        PaymentResult result = getPaymentUseCase.execute(new GetPaymentQuery(paymentId));
        return ResponseEntity.ok(mapper.toHttpResponse(result));
    }
}
