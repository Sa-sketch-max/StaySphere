package com.sahil.project.StaySphere.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentResponse {

    private Long bookingId;

    private String razorpayOrderId;

    private String razorpayKeyId;

    private Long amount; // Amount in paise

    private String currency;

    private String paymentStatus;
}