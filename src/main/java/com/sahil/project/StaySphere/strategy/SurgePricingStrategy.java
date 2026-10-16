package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.entity.Inventory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class SurgePricingStrategy implements PricingStrategy{

    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = inventory.getPrice();

        return price.multiply(inventory.getSurgeFactor());



    }
}
