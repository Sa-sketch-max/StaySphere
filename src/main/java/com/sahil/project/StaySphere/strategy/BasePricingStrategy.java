package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.entity.Inventory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@AllArgsConstructor
public class BasePricingStrategy implements PricingStrategy {
    private static PricingStrategy wrapper;


    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        return inventory.getRoom().getBasePrice();
    }
}
