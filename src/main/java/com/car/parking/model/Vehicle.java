    package com.car.parking.model;
    import lombok.Data;
    import jakarta.persistence.*;
    import java.time.LocalDateTime;

    @Data
    @Entity
    @Table(name = "vehicles")
    public class Vehicle {

        @Id
        @Column(name = "registration_number")
        private String registrationNumber;

        @Column(name = "owner_name")
        private String ownerName;

        @Column(name = "phone_number")
        private String phoneNumber;

        @Column(name = "vehicle_type")
        private String vehicleType;  // Car, Motorcycle, Van, Bus

        @Column(name = "entry_time")
        private LocalDateTime entryTime;
    }

