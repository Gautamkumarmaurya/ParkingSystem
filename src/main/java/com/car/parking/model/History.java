package com.car.parking.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "history")
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "total_duration")
    private long totalDuration;  // In minutes

    @Column(name = "amount")
    private double amount;

    @Column(name = "status")
    private String status;  // UNPAID, PAID

    // New fields added for parking zone and slot details
    @Column(name = "parking_zone")
    private String parkingZone;

    @Column(name = "parking_slot")
    private String parkingSlot;
}
