package com.sahil.project.StaySphere.dto;

import com.sahil.project.StaySphere.entity.Hotel;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HotelPriceDto {

    private Hotel hotel;
    private BigDecimal price;

    public HotelPriceDto(Hotel hotel, Double price) {
        this.hotel = hotel;
        this.price = BigDecimal.valueOf(price);
    }
}