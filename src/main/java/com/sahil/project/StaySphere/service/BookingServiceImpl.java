
package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.dto.BookingDto;
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
import com.sahil.project.StaySphere.dto.BookingRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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



    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {

        if (bookingRequest.getCheckInDate() == null
                || bookingRequest.getCheckOutDate() == null
                || !bookingRequest.getCheckOutDate().isAfter(bookingRequest.getCheckInDate())) {
            throw new IllegalArgumentException(
                    "Check-out date must be after check-in date"
            );
        }

        if (bookingRequest.getRoomsCount() <= 0) {
            throw new IllegalArgumentException(
                    "Rooms count must be greater than zero"
            );
        }

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

        if (room.getHotel() == null
                || !room.getHotel().getId().equals(hotel.getId())) {
            throw new IllegalArgumentException(
                    "Room does not belong to the selected hotel"
            );
        }

        List<Inventory> inventoryList =
                inventoryRepository.findAndLockAvailableInventory(
                        room.getId(),
                        bookingRequest.getCheckInDate(),
                        bookingRequest.getCheckOutDate(),
                        bookingRequest.getRoomsCount()
                );



        long nights = ChronoUnit.DAYS.between(
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate()
        );

        System.out.println("INVENTORY RECORDS FOUND: " + inventoryList.size());
        System.out.println("EXPECTED NIGHTS: " + nights);

        if (inventoryList.size() != nights) {
            throw new IllegalStateException(
                    "Room is not available for the selected dates"
            );
        }

        BigDecimal totalAmount = inventoryList.stream()
                .map(Inventory::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(
                        BigDecimal.valueOf(
                                bookingRequest.getRoomsCount()
                        )
                );

        for (Inventory inventory : inventoryList) {
            inventory.setReservedCount(
                    inventory.getReservedCount()
                            + bookingRequest.getRoomsCount()
            );
        }

        inventoryRepository.saveAll(inventoryList);


        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User user = (User) authentication.getPrincipal();

        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(user)
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalAmount)
                .build();

        booking = bookingRepository.save(booking);

        log.info("Booking created with id {}", booking.getId());

        return modelMapper.map(booking, BookingDto.class);
    }

    @Override
    @Transactional
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
                    "Booking has expired"
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
            booking.setGuests(new HashSet<>());
        }

        // TEMPORARY: Replace with the authenticated user.
        User user = userRepository.findById(1L)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id 1"
                        )
                );

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
        return booking.getCreatedAt() != null
                && booking.getCreatedAt()
                .plusMinutes(10)
                .isBefore(LocalDateTime.now());
    }
}
