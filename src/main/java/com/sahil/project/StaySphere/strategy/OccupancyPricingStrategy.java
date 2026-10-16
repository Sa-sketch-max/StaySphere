package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.entity.Inventory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor

public class OccupancyPricingStrategy implements PricingStrategy {

    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);
        double occupancy = (double) inventory.getBookedCount()/inventory.getTotalCount();

        if(occupancy>0.8){
            price = price.multiply(BigDecimal.valueOf(1.5));
        }

        return price;
    }
}
