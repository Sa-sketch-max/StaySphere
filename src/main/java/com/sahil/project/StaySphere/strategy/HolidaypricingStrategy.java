package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.entity.Inventory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class HolidaypricingStrategy implements PricingStrategy {

    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = inventory.getPrice();

        Boolean isTodayHoliday = true;

        if(isTodayHoliday){
            price = price.multiply(BigDecimal.valueOf(1.5));
        }
        return price;
    }
}
