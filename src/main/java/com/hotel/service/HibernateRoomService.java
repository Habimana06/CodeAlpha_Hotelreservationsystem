package com.hotel.service;

import com.hotel.model.Room;
import com.hotel.model.RoomCategory;
import com.hotel.repository.HibernateRoomRepository;

import java.time.LocalDate;
import java.util.List;

public class HibernateRoomService {
    private final HibernateRoomRepository roomRepository;

    public HibernateRoomService(HibernateRoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        return roomRepository.findAvailableRooms(checkIn, checkOut);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<RoomCategory> getAllCategories() {
        return roomRepository.findAllCategories();
    }

    public Room getRoomById(long id) {
        return roomRepository.findById(id).orElse(null);
    }
}

