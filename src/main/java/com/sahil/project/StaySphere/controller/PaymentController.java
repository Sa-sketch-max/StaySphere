package com.sahil.project.StaySphere.controller;

import com.razorpay.RazorpayException;
import com.sahil.project.StaySphere.dto.CreatePaymentRequest;
import com.sahil.project.StaySphere.dto.CreatePaymentResponse;
import com.sahil.project.StaySphere.dto.VerifyPaymentRequest;
import com.sahil.project.StaySphere.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}