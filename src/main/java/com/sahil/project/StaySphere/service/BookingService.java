
package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.dto.BookingDto;
import com.sahil.project.StaySphere.dto.BookingRequest;
import com.sahil.project.StaySphere.dto.GuestDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BookingService {

    BookingDto initialiseBooking(BookingRequest bookingRequest);

    @Transactional
    BookingDto addGuests(
            Long bookingId,
            List<GuestDto> guestDtoList
    );
}
