package com.sahil.project.StaySphere.dto;



import lombok.Data;
import java.math.BigDecimal;

@Data
public class HotelInfoDto {
    private Long id;
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
    private BigDecimal pricePerNight;
}