package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.entity.Hotel;
import com.sahil.project.StaySphere.entity.Inventory;
import com.sahil.project.StaySphere.repository.HotelRepository;
import com.sahil.project.StaySphere.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class PricingUpdateService {
    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;


    public void updatePrices() {
        int page = 0;
        int batchSize = 100;

        while (true) {
            Page<Hotel> hotelPage =
                    hotelRepository.findAll(PageRequest.of(page, batchSize));

            if (hotelPage.isEmpty()) {
                break;
            }

            hotelPage.getContent().forEach(this::updateHotelPrices);
            page++;
        }
    }

    private void updateHotelPrices(Hotel hotel) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusYears(1);

        List<Inventory> inventoryList =
                inventoryRepository.findByHotelAndDateBetween(
                        hotel, startDate, endDate
                );

        updateInventoryPrices(inventoryList);

        updateHotelMinPrice(hotel, inventoryList, startDate, endDate);
    }

    private void updateHotelMinPrice(
            Hotel hotel,
            List<Inventory> inventoryList,
            LocalDate startDate,
            LocalDate endDate
    ) {


        // Compute minimum price per day for the hotel
        Map<LocalDate, BigDecimal> dailyMinPrices = inventories.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        Collectors.mapping(
                                Inventory::getPrice,
                                Collectors.minBy(Comparator.naturalOrder())
                        )
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().orElse(BigDecimal.ZERO)
                ));

// Prepare HotelPrice entities
        List<HotelPrice> hotelPrices = new ArrayList<>();

        dailyMinPrices.forEach((date, price) -> {
            HotelPrice hotelPrice = hotelPriceRepository
                    .findByHotelAndDate(hotel, date)
                    .orElse(new HotelPrice(hotel, date));

            hotelPrice.setPrice(price);
            hotelPrices.add(hotelPrice);
        });

// Save all HotelPrice entities in bulk
        hotelPriceRepository.saveAll(hotelPrices);
    }

    private void updateInventoryPrices(List<Inventory> inventoryList) {
        inventoryList.forEach(inventory -> {
            BigDecimal dynamicPrice =
                    pricingService.calculateDynamicPricing(inventory);

            inventory.setPrice(dynamicPrice);
        });

        inventoryRepository.saveAll(inventoryList);
    }
}
