package com.car.parking.model;

import lombok.Data;

@Data
public class BookingResponse {

    private String registrationNumber;
    private String ownerName;
    private String phoneNumber;
    private String vehicleType;
    private String zone;
    private String slot;
    private String status;  // PAID/UNPAID
    private String entryTime;
    private String exitTime;

}
