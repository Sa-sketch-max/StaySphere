package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.dto.HotelDto;
import com.sahil.project.StaySphere.entity.Hotel;

public interface HotelService {
    HotelDto createNewHotel(HotelDto hotelDto);

    HotelDto getHotelById(Long id);
}
