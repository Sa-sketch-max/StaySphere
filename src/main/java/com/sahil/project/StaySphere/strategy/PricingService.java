package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.entity.Inventory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
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
