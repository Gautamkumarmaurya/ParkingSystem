package com.car.parking.model;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "parking_lots")
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone")
    private String zone;  // A, B, C, D, E

    @Column(name = "slot")
    private String slot;  // A1, A2, A3...A10, etc.

    @Column(name = "booked_slot_status")
    private String bookedSlotStatus;  // Occupied or Available

    @Column(name = "vehicle_registration_number")
    private String vehicleRegistrationNumber;  // Assigned vehicle's registration number
}
