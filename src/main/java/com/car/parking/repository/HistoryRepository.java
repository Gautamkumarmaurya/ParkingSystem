package com.car.parking.repository;

import com.car.parking.model.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {
    History findByRegistrationNumberAndStatus(String registrationNumber, String status);

}

