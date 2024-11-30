package com.car.parking.repository;

import com.car.parking.model.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    ParkingLot findByVehicleRegistrationNumber(String registrationNumber);

    List<ParkingLot> findByVehicleRegistrationNumberIsNotNull();

    ParkingLot findByZoneAndSlotAndBookedSlotStatus(String zone, String slot, String bookedSlotStatus);
}
