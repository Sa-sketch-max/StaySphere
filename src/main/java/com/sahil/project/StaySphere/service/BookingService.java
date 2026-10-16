package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.dto.BookingDto;
import com.sahil.project.StaySphere.dto.BookingRequest;
import jakarta.transaction.Transactional;

public interface BookingService {
    BookingDto initialiseBooking();

    @Transactional
    BookingDto initialiseBooking(BookingRequest bookingRequest);
}
