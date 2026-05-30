package com.yuno.payment.infrastructure.web;

import com.yuno.payment.application.command.CreatePaymentCommand;
import com.yuno.payment.application.command.GetPaymentQuery;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.model.PaymentId;
import com.yuno.payment.domain.port.inbound.CreatePaymentUseCase;
import com.yuno.payment.domain.port.inbound.GetPaymentUseCase;
import com.yuno.payment.infrastructure.web.dto.CreatePaymentRequest;
import com.yuno.payment.infrastructure.web.dto.PaymentHttpResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

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

    /**
     * POST /payments
     *
     * Creates a payment. Idempotent: repeating the request with the same
     * Idempotency-Key returns the original response without side effects.
     *
     * @param idempotencyKey  Required header. UUID format recommended.
     * @param request         Payment details.
     * @return 201 Created with the payment result, or 200 OK on idempotency hit.
     */
    @PostMapping
    public ResponseEntity<PaymentHttpResponse> createPayment(
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        MDC.put("idempotencyKey", idempotencyKey);
        log.info("POST /payments method={} amount={} currency={} idempotencyKey={}",
                request.method(), request.amount(), request.currency(), idempotencyKey);

        try {
            CreatePaymentCommand command = mapper.toCommand(request, idempotencyKey);
            PaymentResult result = createPaymentUseCase.execute(command);
            PaymentHttpResponse response = mapper.toHttpResponse(result);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            MDC.remove("idempotencyKey");
        }
    }

    /**
     * GET /payments/{id}
     *
     * Fetches a payment by its ID.
     *
     * @param id  UUID string of the payment.
     * @return 200 OK with payment details, or 404 if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentHttpResponse> getPayment(@PathVariable String id) {
        log.info("GET /payments/{}", id);
        PaymentId paymentId = PaymentId.of(id); // throws IllegalArgumentException on bad format
        PaymentResult result = getPaymentUseCase.execute(new GetPaymentQuery(paymentId));
        return ResponseEntity.ok(mapper.toHttpResponse(result));
    }
}
