package com.sahil.project.StaySphere.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {
    private long hotelId;
    private long roomId;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer roomsCount;
}
