package com.sahil.project.StaySphere.controller;

import com.razorpay.RazorpayException;
import com.sahil.project.StaySphere.dto.CreatePaymentRequest;
import com.sahil.project.StaySphere.dto.CreatePaymentResponse;
import com.sahil.project.StaySphere.dto.VerifyPaymentRequest;
import com.sahil.project.StaySphere.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.*;

@EnableMethodSecurity
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/order")
    public CreatePaymentResponse createPaymentOrder(
            @Valid @RequestBody CreatePaymentRequest request
    ) throws RazorpayException {

        return paymentService.createPaymentOrder(request);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request
    ) throws RazorpayException {

        paymentService.verifyPayment(request);

        return ResponseEntity.ok("Payment verified and booking confirmed successfully");
    }

    @PostMapping("/refund/{paymentId}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN')")
    public ResponseEntity<String> initiateRefund(@PathVariable Long paymentId) throws RazorpayException {
        paymentService.initiateRefund(paymentId);
        return ResponseEntity.ok("Refund initiated successfully");
    }
}