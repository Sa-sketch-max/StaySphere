package com.sahil.project.StaySphere.controler;

import com.sahil.project.StaySphere.dto.HotelDto;
import com.sahil.project.StaySphere.dto.RoomDto;
import com.sahil.project.StaySphere.entity.Room;
import com.sahil.project.StaySphere.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomDto> createNewRoom(
            @PathVariable Long hotelId,
            @RequestBody RoomDto roomDto) {

        RoomDto room = roomService.createNewRoom(hotelId, roomDto);

        return new ResponseEntity<>(room, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getAllRoomsInHotels(
            @PathVariable Long hotelId) {

        return ResponseEntity.ok(
                roomService.getAllRoomsInHotels(hotelId)
        );
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDto> getRoomById(
            @PathVariable Long hotelId,
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
                roomService.getRoomById(roomId)
        );
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoomById(
            @PathVariable Long hotelId,
            @PathVariable Long roomId) {

        roomService.deleteRoomById(roomId);

        return ResponseEntity.noContent().build();
    }
}