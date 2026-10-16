package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.entity.Inventory;

import java.math.BigDecimal;

public class PricingService {

    public BigDecimal calculateDynamicPricing(Inventory inventory) {
        PricingStrategy pricingStrategy = new BasePricingStrategy();

        pricingStrategy = new OccupancyPricingStrategy(pricingStrategy);
        pricingStrategy = new SurgePricingStrategy(pricingStrategy);
        pricingStrategy = new HolidaypricingStrategy(pricingStrategy);
        pricingStrategy = new UrgentPricingStrategy(pricingStrategy);

        return pricingStrategy.calculatePrice(inventory);
    }
}
