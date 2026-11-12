package com.sahil.project.StaySphere.service;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.sahil.project.StaySphere.entity.WebhookEvent;
import com.sahil.project.StaySphere.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentWebhookService {

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private final PaymentService paymentService;
    private final WebhookEventRepository webhookEventRepository;

    public void processWebhook(
            String payload,
            String signature,
            String eventId
    ) throws RazorpayException {
        // 1. Verify cryptographic HMAC-SHA256 signature
        if (signature == null || signature.isBlank()) {
            throw new SecurityException("Missing X-Razorpay-Signature header");
        }

        boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        if (!isValid) {
            throw new SecurityException("Invalid webhook signature");
        }

        // 2. Event-level Deduplication (Idempotency)
        if (eventId != null && !eventId.isBlank()) {
            if (webhookEventRepository.existsByEventId(eventId)) {
                log.info("Duplicate webhook event ID: {}. Skipping.", eventId);
                return;
            }
        }

        // 3. Process Event Payload
        JSONObject webhookData = new JSONObject(payload);
        String event = webhookData.getString("event");
        JSONObject payloadObj = webhookData.getJSONObject("payload");

        log.info("Processing webhook event: {}", event);

        switch (event) {
            case "payment.failed" -> {
                JSONObject paymentEntity = payloadObj.getJSONObject("payment").getJSONObject("entity");
                String razorpayOrderId = paymentEntity.getString("order_id");
                paymentService.markPaymentFailed(razorpayOrderId);
            }

            case "payment.captured" -> {
                JSONObject paymentEntity = payloadObj.getJSONObject("payment").getJSONObject("entity");
                String razorpayOrderId = paymentEntity.getString("order_id");
                paymentService.confirmPaymentFromWebhook(
                        razorpayOrderId,
                        paymentEntity.getString("id")
                );
            }

            case "refund.processed" -> {
                JSONObject refundEntity = payloadObj.getJSONObject("refund").getJSONObject("entity");
                String razorpayPaymentId = refundEntity.getString("payment_id");
                String razorpayRefundId = refundEntity.getString("id");
                paymentService.processRefundSuccess(razorpayPaymentId, razorpayRefundId);
            }

            case "refund.failed" -> {
                JSONObject refundEntity = payloadObj.getJSONObject("refund").getJSONObject("entity");
                String razorpayPaymentId = refundEntity.getString("payment_id");
                String razorpayRefundId = refundEntity.getString("id");
                paymentService.processRefundFailure(razorpayPaymentId, razorpayRefundId);
            }

            default -> log.info("Unhandled webhook event: {}", event);
        }

        // 4. Save processed event ID
        if (eventId != null && !eventId.isBlank()) {
            WebhookEvent webhookEvent = new WebhookEvent();
            webhookEvent.setEventId(eventId);
            webhookEvent.setEventType(event);
            webhookEventRepository.save(webhookEvent);
        }
    }
}