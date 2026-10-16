package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.entity.Inventory;

import java.math.BigDecimal;

public class BasePricingStrategy implements PricingStrategy {
    private static PricingStrategy wrapper;


    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        return inventory.getRoom().getBasePrice();
    }
}
