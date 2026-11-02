package com.sahil.project.StaySphere.controler;

import com.sahil.project.StaySphere.dto.HotelDto;
import com.sahil.project.StaySphere.dto.HotelPriceDto;
import com.sahil.project.StaySphere.dto.HotelSearchRequest;
import com.sahil.project.StaySphere.service.HotelService;
import com.sahil.project.StaySphere.service.InventoryService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/hotel")
public class HotelBrowserController {
    private final InventoryService inventoryService;
    private final HotelService hotelService;

    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest) {

        Page<HotelPriceDto> page = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{hoteLId}/info")
    public ResponseEntity<HotelDto> getHotelInfo(@PathVariable Long hotelId) {
        return ResponseEntity.ok(hotelService.getHotelById(hotelId));

    }
}
