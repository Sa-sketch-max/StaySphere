package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.entity.Booking;
import com.sahil.project.StaySphere.entity.Inventory;
import com.sahil.project.StaySphere.entity.enums.BookingStatus;
import com.sahil.project.StaySphere.repository.BookingRepository;
import com.sahil.project.StaySphere.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingExpirationService {

    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public void expireReservations() {

        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(10);

        List<Booking> expiredBookings =
                bookingRepository.findExpiredBookings(
                        BookingStatus.RESERVED,
                        expiryTime
                );

        for (Booking booking : expiredBookings) {

            List<Inventory> inventoryList =
                    inventoryRepository.findAndLockInventoryForBooking(
                            booking.getRoom().getId(),
                            booking.getCheckInDate(),
                            booking.getCheckOutDate()
                    );

            for (Inventory inventory : inventoryList) {

                if (inventory.getReservedCount() < booking.getRoomsCount()) {
                    throw new IllegalStateException(
                            "Reserved inventory is inconsistent for booking "
                                    + booking.getId()
                    );
                }

                inventory.setReservedCount(
                        inventory.getReservedCount() - booking.getRoomsCount()
                );
            }

            inventoryRepository.saveAll(inventoryList);

            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        }
    }
}