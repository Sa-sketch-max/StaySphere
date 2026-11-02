package com.sahil.project.StaySphere.repository;

import com.sahil.project.StaySphere.dto.BookingDto;
import com.sahil.project.StaySphere.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
