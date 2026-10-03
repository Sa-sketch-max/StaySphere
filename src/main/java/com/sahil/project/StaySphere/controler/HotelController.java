package com.sahil.project.StaySphere.controler;

import com.sahil.project.StaySphere.dto.HotelDto;
import com.sahil.project.StaySphere.entity.Hotel;
import com.sahil.project.StaySphere.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {
    private final HotelService hotelService;

    @PostMapping
    public ResponseEntity<HotelDto> createNewHotel(@RequestBody HotelDto hotelDto){
        Hotel hotel = hotelService.createNewHotel(hotelDto);
        return new ResponseEntity<>(hotelDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<HotelDto> getAllHotels(@PathVariable Long hotelId) {
        HotelDto hotelDto = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelDto);
    }
}
