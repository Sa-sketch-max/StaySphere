package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.dto.HotelDto;
import com.sahil.project.StaySphere.dto.HotelSearchRequest;
import com.sahil.project.StaySphere.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {
    void initializeRoomForAYear(Room room);

    void deleteFutureInventories(Room room);

    Page<HotelDto> searchHotels(HotelSearchRequest hotelSearchRequest);
}
