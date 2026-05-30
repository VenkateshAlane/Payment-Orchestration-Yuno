package com.yuno.payment.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yuno.payment.application.command.PaymentResult;
import com.yuno.payment.domain.exception.PaymentFailedException;
import com.yuno.payment.domain.exception.PaymentNotFoundException;
import com.yuno.payment.domain.model.PaymentId;
import com.yuno.payment.domain.port.inbound.CreatePaymentUseCase;
import com.yuno.payment.domain.port.inbound.GetPaymentUseCase;
import com.yuno.payment.infrastructure.web.dto.CreatePaymentRequest;
import com.yuno.payment.domain.model.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({PaymentMapper.class, GlobalExceptionHandler.class})
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatePaymentUseCase createPaymentUseCase;

    @MockBean
    private GetPaymentUseCase getPaymentUseCase;

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private PaymentResult sampleResult(String paymentId, String status, String provider) {
        return new PaymentResult(
                paymentId, status, "CARD", provider,
                new BigDecimal("100.00"), "USD", 1,
                Instant.parse("2026-04-11T10:00:00Z"),
                Instant.parse("2026-04-11T10:00:01Z")
        );
    }

    private String body(CreatePaymentRequest req) throws Exception {
        return json.writeValueAsString(req);
    }

    // ── POST /payments ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /payments")
    class Create {

        @Test
        @DisplayName("returns 201 with PaymentHttpResponse on success")
        void successReturns201() throws Exception {
            when(createPaymentUseCase.execute(any()))
                    .thenReturn(sampleResult("pay-001", "SUCCESS", "PROVIDER_A"));

            mockMvc.perform(post("/payments")
                            .header("Idempotency-Key", "key-001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(new CreatePaymentRequest(
                                    new BigDecimal("100.00"), "USD", PaymentMethod.CARD, "cust-1"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value("pay-001"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.assignedProvider").value("PROVIDER_A"));
        }

        @Test
        @DisplayName("returns 400 when Idempotency-Key header is missing")
        void missingIdempotencyKeyReturns400() throws Exception {
            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(new CreatePaymentRequest(
                                    new BigDecimal("100.00"), "USD", PaymentMethod.CARD, "cust-1"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when request body has validation errors")
        void invalidBodyReturns400() throws Exception {
            // null amount triggers validation
            mockMvc.perform(post("/payments")
                            .header("Idempotency-Key", "key-002")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currency\":\"USD\",\"method\":\"CARD\",\"customerId\":\"cust-1\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("returns 422 when all providers fail")
        void allProvidersFailReturns422() throws Exception {
            when(createPaymentUseCase.execute(any()))
                    .thenThrow(new PaymentFailedException(PaymentId.generate()));

            mockMvc.perform(post("/payments")
                            .header("Idempotency-Key", "key-003")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(new CreatePaymentRequest(
                                    new BigDecimal("100.00"), "USD", PaymentMethod.CARD, "cust-1"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("PAYMENT_FAILED"));
        }

        @Test
        @DisplayName("returns 400 when amount is zero")
        void zeroAmountReturns400() throws Exception {
            mockMvc.perform(post("/payments")
                            .header("Idempotency-Key", "key-004")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":0,\"currency\":\"USD\",\"method\":\"CARD\",\"customerId\":\"cust-1\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 for invalid payment method enum")
        void invalidMethodReturns400() throws Exception {
            mockMvc.perform(post("/payments")
                            .header("Idempotency-Key", "key-005")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":100,\"currency\":\"USD\",\"method\":\"CRYPTO\",\"customerId\":\"cust-1\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /payments/{id} ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /payments/{id}")
    class Get {

        @Test
        @DisplayName("returns 200 with PaymentHttpResponse for known payment")
        void foundReturns200() throws Exception {
            String paymentId = PaymentId.generate().toString();
            when(getPaymentUseCase.execute(any()))
                    .thenReturn(sampleResult(paymentId, "SUCCESS", "PROVIDER_B"));

            mockMvc.perform(get("/payments/" + paymentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(paymentId))
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("returns 404 for unknown payment ID")
        void unknownIdReturns404() throws Exception {
            when(getPaymentUseCase.execute(any()))
                    .thenThrow(new PaymentNotFoundException(PaymentId.generate()));

            mockMvc.perform(get("/payments/" + PaymentId.generate()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("PAYMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("returns 400 for malformed payment ID")
        void malformedIdReturns400() throws Exception {
            mockMvc.perform(get("/payments/not-a-valid-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }
}
