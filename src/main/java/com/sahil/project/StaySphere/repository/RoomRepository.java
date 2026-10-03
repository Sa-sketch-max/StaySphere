package com.sahil.project.StaySphere.repository;

import com.sahil.project.StaySphere.entity.Hotel;
import com.sahil.project.StaySphere.entity.Room;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RoomRepository extends JpaRepository<Room, Long> {





}
