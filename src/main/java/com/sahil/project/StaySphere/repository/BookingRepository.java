package com.sahil.project.StaySphere.repository;

import com.sahil.project.StaySphere.dto.BookingDto;
import com.sahil.project.StaySphere.entity.Booking;
import com.sahil.project.StaySphere.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sahil.project.StaySphere.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT b
       FROM Booking b
       WHERE b.bookingStatus = :status
       AND b.createdAt <= :expiryTime
       """)
    List<Booking> findExpiredBookings(
            @Param("status") BookingStatus status,
            @Param("expiryTime") LocalDateTime expiryTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :bookingId")
    Optional<Booking> findByIdForUpdate(
            @Param("bookingId") Long bookingId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT b
    FROM Booking b
    WHERE b.payment.id = :paymentId
    """)
    Optional<Booking> findByPaymentIdForUpdate(
            @Param("paymentId") Long paymentId
    );

    Optional<Booking> findByPayment(Payment payment);
}
