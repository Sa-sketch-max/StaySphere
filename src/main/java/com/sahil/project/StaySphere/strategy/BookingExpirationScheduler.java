package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.service.BookingExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpirationScheduler {
    private final BookingExpirationService bookingExpirationService;

    @Scheduled(fixedRate = 60000)
    public void expirationBooking() {
        log.info("Check for expiration booking......");
        bookingExpirationService.expireReservations();
    }

}
