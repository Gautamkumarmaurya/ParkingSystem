package com.car.parking.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VehicleRegistrationRequest {
    private String registrationNumber;
    private String ownerName;
    private String phoneNumber;
    private String vehicleType;
    private String zone;
    private String slot;

}
