package com.sahil.project.StaySphere.repository;

import com.sahil.project.StaySphere.dto.BookingDto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingDto, Integer> {
}
