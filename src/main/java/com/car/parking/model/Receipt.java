package com.car.parking.model;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "receipts")
public class Receipt {

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

    @Column(name = "total_duration")
    private long totalDuration;  // In minutes

    @Column(name = "amount")
    private double amount;

    @Column(name = "receipt_date")
    private LocalDateTime receiptDate;  // Date when the receipt is generated

    @Column(name = "status")
    private String status;  // Paid or Unpaid
}
