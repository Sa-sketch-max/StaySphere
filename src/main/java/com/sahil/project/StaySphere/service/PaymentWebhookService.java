package com.sahil.project.StaySphere.service;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentWebhookService {

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private final PaymentService paymentService;

    public void processWebhook(
            String payload,
            String signature,
            String eventId
    ) {
        {

            try {
                boolean isValid = Utils.verifyWebhookSignature(
                        payload,
                        signature,
                        webhookSecret
                );

                if (!isValid) {
                    throw new SecurityException("Invalid webhook signature");
                }

                JSONObject webhookData = new JSONObject(payload);

                String event = webhookData.getString("event");

                JSONObject paymentEntity = webhookData
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = paymentEntity.getString("order_id");

                switch (event) {

                    case "payment.failed":
                        paymentService.markPaymentFailed(razorpayOrderId);
                        break;

                    case "payment.captured":
                        paymentService.confirmPaymentFromWebhook(
                                razorpayOrderId,
                                paymentEntity.getString("id")
                        );
                        break;

                    default:
                        System.out.println("Unhandled webhook event: " + event);
                }

            } catch (RazorpayException e) {
                throw new RuntimeException(
                        "Webhook signature verification failed", e
                );
            }
        }
    }

}