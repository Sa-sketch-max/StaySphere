package com.sahil.project.StaySphere.controller;

import com.sahil.project.StaySphere.repository.WebhookEventRepository;
import com.sahil.project.StaySphere.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;
    private final WebhookEventRepository webhookEventRepository;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestHeader("x-razorpay-event-id") String eventId
    ) {
        paymentWebhookService.processWebhook(payload, signature, eventId);

        return ResponseEntity.ok("Webhook processed successfully");
    }
}