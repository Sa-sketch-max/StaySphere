package com.sahil.project.StaySphere.service;

import com.sahil.project.StaySphere.entity.Room;

public interface InventoryService {
    void initializeRoomForAYear(Room room);

    void deleteFutureInventories(Room room);
}
