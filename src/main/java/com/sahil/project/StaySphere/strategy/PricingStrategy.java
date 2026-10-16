package com.sahil.project.StaySphere.strategy;

import com.sahil.project.StaySphere.dto.HotelDto;
import com.sahil.project.StaySphere.entity.Inventory;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculatePrice(Inventory inventory);
}
