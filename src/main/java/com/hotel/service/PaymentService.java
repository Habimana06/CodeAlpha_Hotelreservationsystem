package com.hotel.service;

import com.hotel.model.Payment;
import com.hotel.model.Reservation;
import com.hotel.repository.HibernatePaymentRepository;
import com.hotel.repository.HibernateReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class PaymentService {
    private final HibernatePaymentRepository paymentRepository;
    private final HibernateReservationRepository reservationRepository;
    private final Random random = new Random();

    public PaymentService(HibernatePaymentRepository paymentRepository, 
                         HibernateReservationRepository reservationRepository) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
    }

    public Payment processPayment(long reservationId, double amount, String method) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus("CAPTURED");
        payment.setTransactionRef(generateTransactionReference());
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Payment simulatePayment(Reservation reservation, double amount, String method) {
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus("CAPTURED");
        payment.setTransactionRef(generateTransactionReference());
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    private String generateTransactionReference() {
        return "HRB-" + (100000 + random.nextInt(900000));
    }

    public Payment latestPayment(long reservationId) {
        List<Payment> payments = paymentRepository.findByReservation(reservationId);
        return payments.isEmpty() ? null : payments.get(payments.size() - 1);
    }
}
