package com.hotel.service;

import com.hotel.model.Reservation;
import com.hotel.model.Room;
import com.hotel.repository.HibernateReservationRepository;
import com.hotel.repository.HibernateRoomRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class HibernateReservationService {
    private final HibernateReservationRepository reservationRepository;
    private final HibernateRoomRepository roomRepository;

    public HibernateReservationService(HibernateReservationRepository reservationRepository, 
                                      HibernateRoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public Reservation book(long userId, long roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in");
        }
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        
        Reservation reservation = new Reservation();
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuestCount(guests);
        reservation.setStatus("CONFIRMED");
        reservation.setRoom(room);
        
        // Note: User should be set by the caller
        Reservation saved = reservationRepository.save(reservation);
        roomRepository.updateStatus(roomId, "RESERVED");
        return saved;
    }

    public void cancel(long reservationId, long roomId) {
        reservationRepository.cancelReservation(reservationId);
        roomRepository.updateStatus(roomId, "AVAILABLE");
    }

    public List<Reservation> findCustomerReservations(long userId) {
        return reservationRepository.findByUser(userId);
    }

    public List<Reservation> findAllReservations() {
        return reservationRepository.findAll();
    }

    public double calculateStayCost(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return nights * room.getNightlyRate();
    }
}

