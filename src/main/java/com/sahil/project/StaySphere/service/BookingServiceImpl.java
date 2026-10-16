
package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.dto.BookingDto;
import com.sahil.project.StaySphere.dto.BookingRequest;
import com.sahil.project.StaySphere.dto.GuestDto;
import com.sahil.project.StaySphere.entity.Booking;
import com.sahil.project.StaySphere.entity.Guest;
import com.sahil.project.StaySphere.entity.Hotel;
import com.sahil.project.StaySphere.entity.Inventory;
import com.sahil.project.StaySphere.entity.Room;
import com.sahil.project.StaySphere.entity.User;
import com.sahil.project.StaySphere.entity.enums.BookingStatus;
import com.sahil.project.StaySphere.exception.ResourceNotFoundException;
import com.sahil.project.StaySphere.repository.BookingRepository;
import com.sahil.project.StaySphere.repository.GuestRepository;
import com.sahil.project.StaySphere.repository.HotelRepository;
import com.sahil.project.StaySphere.repository.InventoryRepository;
import com.sahil.project.StaySphere.repository.RoomRepository;
import com.sahil.project.StaySphere.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final InventoryRepository inventoryRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {

        Hotel hotel = hotelRepository.findById(
                bookingRequest.getHotelId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Hotel not found with id "
                                + bookingRequest.getHotelId()
                )
        );

        Room room = roomRepository.findById(
                bookingRequest.getRoomId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Room not found with id "
                                + bookingRequest.getRoomId()
                )
        );

        if (bookingRequest.getCheclInDate() == null
                || bookingRequest.getCheclOutDate() == null
                || !bookingRequest.getCheclOutDate()
                .isAfter(bookingRequest.getCheclInDate())) {
            throw new IllegalArgumentException(
                    "Check-out date must be after check-in date"
            );
        }

        if (bookingRequest.getRoomsCount() <= 0) {
            throw new IllegalArgumentException(
                    "Rooms count must be greater than zero"
            );
        }

        List<Inventory> inventoryList =
                inventoryRepository.findAndLockAvailableInventory(
                        room.getId(),
                        bookingRequest.getCheclInDate(),
                        bookingRequest.getCheclOutDate(),
                        bookingRequest.getRoomsCount()
                );

        long daysCount = ChronoUnit.DAYS.between(
                bookingRequest.getCheclInDate(),
                bookingRequest.getCheclOutDate()
        );

        if (inventoryList.size() != daysCount) {
            throw new IllegalStateException(
                    "Room is not available for the selected dates"
            );
        }

        for (Inventory inventory : inventoryList) {
            inventory.setBookedCount(
                    inventory.getBookedCount()
                            + bookingRequest.getRoomsCount()
            );
        }

        inventoryRepository.saveAll(inventoryList);

        // TODO: Replace with the authenticated user.
        User user = userRepository.getReferenceById(1L);

        // TODO: Calculate the actual booking price.
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheclInDate())
                .checkOutDate(bookingRequest.getCheclOutDate())
                .user(user)
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN)
                .build();

        booking = bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDto.class);
    }

    @Transactional
    @Override
    public BookingDto addGuests(
            Long bookingId,
            List<GuestDto> guestDtoList
    ) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id " + bookingId
                        )
                );

        if (hasBookingExpired(booking)) {
            throw new IllegalStateException(
                    "Booking has already expired"
            );
        }

        if (booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException(
                    "Booking is not in RESERVED state"
            );
        }

        if (guestDtoList == null || guestDtoList.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one guest is required"
            );
        }

        if (booking.getGuests() == null) {
            booking.setGuests(new ArrayList<>());
        }

        // TODO: Replace with the authenticated user.
        User user = userRepository.getReferenceById(1L);

        for (GuestDto guestDto : guestDtoList) {
            Guest guest = modelMapper.map(guestDto, Guest.class);
            guest.setUser(user);
            guest = guestRepository.save(guest);

            booking.getGuests().add(guest);
        }

        booking = bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDto.class);
    }

    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt()
                .plusMinutes(10)
                .isBefore(LocalDateTime.now());
    }
}
