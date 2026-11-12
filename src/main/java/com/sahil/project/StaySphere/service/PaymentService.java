package com.sahil.project.StaySphere.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.sahil.project.StaySphere.entity.*;
import com.sahil.project.StaySphere.repository.BookingRepository;
import com.sahil.project.StaySphere.repository.InventoryRepository;
import com.sahil.project.StaySphere.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import com.sahil.project.StaySphere.dto.CreatePaymentRequest;
import com.sahil.project.StaySphere.dto.CreatePaymentResponse;
import com.sahil.project.StaySphere.entity.enums.BookingStatus;
import com.sahil.project.StaySphere.entity.enums.PaymentStatus;
import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.sahil.project.StaySphere.dto.VerifyPaymentRequest;
import com.sahil.project.StaySphere.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryRepository inventoryRepository;
    @Value("${razorpay.key.id}")
    private String keyId;
    @Value("${razorpay.key.secret}")
    private String keySecret;


    @Transactional
    public CreatePaymentResponse createPaymentOrder(CreatePaymentRequest request)
            throws RazorpayException {



        // 1. Find booking
        Booking booking = bookingRepository.findByIdForUpdate(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Get currently authenticated user
        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (!(principal instanceof User currentUser)) {
            throw new AccessDeniedException("User is not authenticated");
        }

// Verify booking ownership
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "You are not allowed to pay for this booking"
            );
        }

        // 2. Check booking status
        if (booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Booking is not available for payment");
        }



        // 3. Check payment deadline
        if (booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Booking reservation has expired");
        }


        // Check whether a pending payment already exists
        Payment existingPayment = booking.getPayment();

        if (existingPayment != null
                && existingPayment.getPaymentStatus() == PaymentStatus.PENDING
                && existingPayment.getRazorpayOrderId() != null) {

            long existingAmountInPaise = existingPayment.getAmount()
                    .multiply(new BigDecimal("100"))
                    .longValueExact();

            return CreatePaymentResponse.builder()
                    .bookingId(booking.getId())
                    .razorpayOrderId(existingPayment.getRazorpayOrderId())
                    .razorpayKeyId(keyId)
                    .amount(existingAmountInPaise)
                    .currency("INR")
                    .paymentStatus("PENDING")
                    .build();
        }

        // 4. Convert rupees into paise
        long amountInPaise = booking.getAmount()
                .multiply(new BigDecimal("100"))
                .longValueExact();

        // 5. Create Razorpay order
        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "booking_" + booking.getId());

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        String razorpayOrderId = razorpayOrder.get("id");

        // 6. Create payment record
        Payment payment = new Payment();

        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAmount(booking.getAmount());
        payment.setPaymentStatus(PaymentStatus.PENDING);

        paymentRepository.save(payment);

        // 7. Link payment with booking
        booking.setPayment(payment);
        bookingRepository.save(booking);

        // 8. Return response
        return CreatePaymentResponse.builder()
                .bookingId(booking.getId())
                .razorpayOrderId(razorpayOrderId)
                .razorpayKeyId(keyId)
                .amount(amountInPaise)
                .currency("INR")
                .paymentStatus("PENDING")
                .build();
    }

    public boolean verifySignature(
            String orderId,
            String paymentId,
            String signature
    ) throws RazorpayException {

        String payload = orderId + "|" + paymentId;

        return Utils.verifySignature(
                payload,
                signature,
                keySecret
        );
    }


    @Transactional
    public void verifyPayment(VerifyPaymentRequest request)
            throws RazorpayException {

        // 1. Find the payment record
        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 2. Find and lock the associated booking
        Booking booking = bookingRepository
                .findByPaymentIdForUpdate(payment.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // 3. Verify booking ownership
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        if (!(principal instanceof User currentUser)
                || !booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Unauthorized payment verification");
        }

        // Check whether this payment was already confirmed
        if (payment.getPaymentStatus() == PaymentStatus.CONFIRMED) {

            if (request.getRazorpayPaymentId()
                    .equals(payment.getRazorpayPaymentId())) {
                return; // Same successful payment: safely ignore duplicate
            }

            throw new IllegalStateException(
                    "This order has already been paid using another payment"
            );
        }

        // 4. Verify order ID
        if (!payment.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new IllegalArgumentException("Invalid Razorpay order");
        }

        // 5. Verify signature
        boolean valid = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        // 6. Fetch actual payment details from Razorpay
        com.razorpay.Payment razorpayPayment =
                razorpayClient.payments.fetch(request.getRazorpayPaymentId());

        // 7. Verify order ID
        if (!request.getRazorpayOrderId()
                .equals(razorpayPayment.get("order_id"))) {
            throw new IllegalArgumentException("Payment order mismatch");
        }

// 8. Verify payment ID
        if (!request.getRazorpayPaymentId()
                .equals(razorpayPayment.get("id"))) {
            throw new IllegalArgumentException("Payment ID mismatch");
        }

// 9. Verify amount
        long expectedAmount = payment.getAmount()
                .multiply(new BigDecimal("100"))
                .longValueExact();

        if (expectedAmount != ((Number) razorpayPayment.get("amount")).longValue()) {
            throw new IllegalArgumentException("Payment amount mismatch");
        }

// 10. Verify currency
        if (!"INR".equals(razorpayPayment.get("currency"))) {
            throw new IllegalArgumentException("Invalid payment currency");
        }

// 11. Verify captured status
        if (!"captured".equals(razorpayPayment.get("status"))) {
            throw new IllegalStateException("Payment has not been captured");
        }

        // 12. Ensure booking is still reserved
        if (booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("Booking is no longer available");
        }

// 13. Lock the inventory rows
        List<Inventory> inventoryList =
                inventoryRepository.findAndLockInventoryForBooking(
                        booking.getRoom().getId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate()
                );

// Verify all booking nights are present
        long nights = ChronoUnit.DAYS.between(
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        // 14. Transfer inventory
        for (Inventory inventory : inventoryList) {

            if (inventory.getReservedCount() < booking.getRoomsCount()) {
                throw new IllegalStateException(
                        "Insufficient reserved inventory for booking "
                                + booking.getId()
                );
            }

            inventory.setReservedCount(
                    inventory.getReservedCount() - booking.getRoomsCount()
            );

            inventory.setBookedCount(
                    inventory.getBookedCount() + booking.getRoomsCount()
            );
        }

        inventoryRepository.saveAll(inventoryList);

        // 15. Update payment details
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setPaymentStatus(PaymentStatus.CONFIRMED);

        paymentRepository.save(payment);

// 16. Confirm booking
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);

        if (inventoryList.size() != nights) {
            throw new IllegalStateException("Booking inventory is incomplete");
        }

        if (!valid) {
            throw new IllegalArgumentException("Invalid payment signature");
        }

        // Payment confirmation logic comes next.
    }



    @Transactional
    public void markPaymentFailed(String razorpayOrderId) {

        Payment payment = paymentRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Booking booking = bookingRepository
                .findByPaymentIdForUpdate(payment.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (payment.getPaymentStatus() == PaymentStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "A confirmed payment cannot be marked as failed"
            );
        }

        if (booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException(
                    "Booking is no longer reserved"
            );
        }

        payment.setPaymentStatus(PaymentStatus.FAILED);

        paymentRepository.save(payment);
    }



    @Transactional
    public void confirmPaymentFromWebhook(
            String razorpayOrderId,
            String razorpayPaymentId
    ) throws RazorpayException {

        Optional<Payment> optionalPayment = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
        if (optionalPayment.isEmpty()) {
            log.warn("Payment record not found for Razorpay Order ID: {}. Webhook skipped.", razorpayOrderId);
            return;
        }
        Payment payment = optionalPayment.get();

        Booking booking = bookingRepository
                .findByPaymentIdForUpdate(payment.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // 1. Prevent duplicate confirmation
        if (payment.getPaymentStatus() == PaymentStatus.CONFIRMED) {
            return;
        }

        // 2. Fetch actual payment details from Razorpay
        com.razorpay.Payment razorpayPayment =
                razorpayClient.payments.fetch(razorpayPaymentId);

        // 3. Validate payment details
        if (!razorpayOrderId.equals(razorpayPayment.get("order_id"))
                || !razorpayPaymentId.equals(razorpayPayment.get("id"))
                || !"captured".equals(razorpayPayment.get("status"))
                || !"INR".equals(razorpayPayment.get("currency"))
                || ((Number) razorpayPayment.get("amount")).longValue()
                != booking.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValueExact()) {

            throw new IllegalStateException(
                    "Invalid captured payment details"
            );
        }

        // 4. Handle payment received after reservation expiry
        if (booking.getBookingStatus() != BookingStatus.RESERVED) {

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);

            paymentRepository.save(payment);

            return;
        }

        // 5. Lock inventory records
        List<Inventory> inventoryList =
                inventoryRepository.findAndLockInventoryForBooking(
                        booking.getRoom().getId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate()
                );

        long nights = ChronoUnit.DAYS.between(
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        if (inventoryList.size() != nights) {
            throw new IllegalStateException(
                    "Inventory records are incomplete"
            );
        }

        // 6. Move reserved inventory to booked inventory
        for (Inventory inventory : inventoryList) {

            if (inventory.getReservedCount() < booking.getRoomsCount()) {
                throw new IllegalStateException(
                        "Reserved inventory is inconsistent"
                );
            }

            inventory.setReservedCount(
                    inventory.getReservedCount() - booking.getRoomsCount()
            );

            inventory.setBookedCount(
                    inventory.getBookedCount() + booking.getRoomsCount()
            );
        }

        inventoryRepository.saveAll(inventoryList);

        // 7. Confirm payment
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaymentStatus(PaymentStatus.CONFIRMED);

        paymentRepository.save(payment);

        // 8. Confirm booking
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);
    }



    @Transactional
    public void initiateRefund(Long paymentId) throws RazorpayException {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getPaymentStatus() != PaymentStatus.REFUND_PENDING) {
            throw new IllegalStateException("Payment is not awaiting refund");
        }

        if (payment.getRazorpayPaymentId() == null) {
            throw new IllegalStateException("Razorpay payment ID is missing");
        }

        JSONObject refundRequest = new JSONObject();

        refundRequest.put(
                "amount",
                payment.getAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .longValueExact()
        );

        com.razorpay.Refund refund =
                razorpayClient.payments.refund(
                        payment.getRazorpayPaymentId(),
                        refundRequest
                );

        String refundStatus = refund.get("status");

        if ("processed".equals(refundStatus)) {

            payment.setPaymentStatus(PaymentStatus.REFUNDED);

        } else if ("pending".equals(refundStatus)) {

            payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);

        } else if ("failed".equals(refundStatus)) {

            throw new IllegalStateException("Razorpay refund failed");
        }

        paymentRepository.save(payment);
    }

    @Transactional
    public void processRefundSuccess(String razorpayPaymentId, String razorpayRefundId) {
        Payment payment = paymentRepository
                .findByRazorpayPaymentId(razorpayPaymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for refund confirmation"));

        payment.setRazorpayRefundId(razorpayRefundId);
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);


        Booking booking = bookingRepository.findByPayment(payment)
                .orElseThrow(() -> new RuntimeException("Booking not found for payment ID: "+payment.getId()));
        if(booking.getBookingStatus() != BookingStatus.CANCELLED) {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);


            List<Inventory> inventories = inventoryRepository.findAndLockInventoryForBooking(
                    booking.getRoom().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );

            for (Inventory inventory : inventories) {
                inventory.setBookedCount(
                        Math.max(0, inventory.getBookedCount() - booking.getRoomsCount())
                );
            }

            inventoryRepository.saveAll(inventories);

        }
    }

    @Transactional
    public void processRefundFailure(String razorpayPaymentId, String razorpayRefundId) {
        Payment payment = paymentRepository
                .findByRazorpayPaymentId(razorpayPaymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for refund failure update"));

        if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }


    @Transactional
    public void processPaymentFailure(String razorpayOrderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + razorpayOrderId));

        if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

}