package com.sahil.project.StaySphere.repository;

import com.sahil.project.StaySphere.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
}