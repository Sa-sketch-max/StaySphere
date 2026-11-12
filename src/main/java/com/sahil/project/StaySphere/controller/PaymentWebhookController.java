package com.sahil.project.StaySphere.controller;

import com.sahil.project.StaySphere.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "x-razorpay-event-id", required = false) String eventIdLower,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventIdUpper
    ) {
        // Resolve eventId regardless of header casing
        String eventId = (eventIdLower != null && !eventIdLower.isBlank()) ? eventIdLower : eventIdUpper;

        try {
            paymentWebhookService.processWebhook(payload, signature, eventId);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (SecurityException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Unhandled error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing error");
        }
    }
}