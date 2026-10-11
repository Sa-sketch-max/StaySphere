package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.dto.HotelDto;
import com.sahil.project.StaySphere.dto.RoomDto;

import java.util.List;

public interface RoomService {

    RoomDto createNewRoom(Long hotelId, RoomDto roomDto );

    List<RoomDto> getAllRoomsInHotels(Long  hotelId);

    RoomDto getRoomById(Long RoomId);

    void deleteRoomById(Long roomId);
}
