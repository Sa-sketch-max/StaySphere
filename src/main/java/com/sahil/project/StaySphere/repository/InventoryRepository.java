package com.sahil.project.StaySphere.repository;

import com.sahil.project.StaySphere.entity.Hotel;
import com.sahil.project.StaySphere.entity.Inventory;
import com.sahil.project.StaySphere.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    void  deleteByRoom(Room room);

    @Query("""
    SELECT i.hotel
    FROM Inventory i
    WHERE i.city = :city
      AND i.date >= :startDate
      AND i.date <= :endDate
      AND i.closed = false
      AND (i.totalCount - i.reservedCount) >= :roomCount
    GROUP BY i.hotel
    HAVING COUNT(DISTINCT i.date) = :dateCount
    """)
    Page<Hotel> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomCount") Integer roomCount,
            @Param("dateCount") Integer dateCount,
            Pageable pageable
    );

    @Query("""
           SELECT i
           FROM inventory i
           WHERE i.room.id = :roomId
           AND i.date BETWEEN :startDate AND :endDate
           AND i.closed = false
           AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
           """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomCount") Integer roomCount
    );


    List<Inventory> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);
}
