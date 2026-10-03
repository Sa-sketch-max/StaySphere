package com.sahil.project.StaySphere.repository;

import com.sahil.project.StaySphere.entity.Hotel;
import com.sahil.project.StaySphere.entity.HotelContactInfo;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface HotelRepository extends JpaRepository<Hotel, Long> {



}
