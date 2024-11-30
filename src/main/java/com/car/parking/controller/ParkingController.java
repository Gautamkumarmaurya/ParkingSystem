package com.car.parking.controller;

import com.car.parking.model.*;
import com.car.parking.service.ParkingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Slf4j
public class ParkingController {

    private final ParkingService parkingService;

    // Initialize Parking
    @PostMapping("/initialize-parking")
    public ResponseEntity<String> initializeParking() {
        String response = parkingService.initializeParking();
        if (response.equals("Parking lot is already initialized.")) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerVehicle(@RequestBody VehicleRegistrationRequest vehicleRegistrationRequest) {
        try {
            // Extract zone and slot from request
            String zone = vehicleRegistrationRequest.getZone();
            String slot = vehicleRegistrationRequest.getSlot();

            // Map VehicleRegistrationRequest to Vehicle entity
            Vehicle vehicle = new Vehicle();
            vehicle.setRegistrationNumber(vehicleRegistrationRequest.getRegistrationNumber());
            vehicle.setOwnerName(vehicleRegistrationRequest.getOwnerName());
            vehicle.setPhoneNumber(vehicleRegistrationRequest.getPhoneNumber());
            vehicle.setVehicleType(vehicleRegistrationRequest.getVehicleType());

            // Call the service method
            String response = parkingService.registerVehicle(vehicle, zone, slot);

            // Check for errors in the response
            if (response.contains("does not exist") || response.contains("is already occupied")) {
                return ResponseEntity.badRequest().body("The selected parking spot is either invalid or already occupied for the registration number:" + vehicle.getRegistrationNumber());
            }
            return ResponseEntity.ok("Vehicle registered successfully!");
        } catch (Exception e) {
            // Log the error
            log.error("Error occurred during vehicle registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong during vehicle registration. Please try again later.");
        }
    }

    @GetMapping("/download-receipt")
    public ResponseEntity<?> downloadReceipt(@RequestParam String registrationNumber) {
        try {
            // Fetch the receipt details using the service
            Receipt receipt = parkingService.getReceiptByRegistrationNumber(registrationNumber);

            if (receipt == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Receipt not found for the provided registration number: " + registrationNumber);
            }

            // Generate PDF for the receipt
            ByteArrayInputStream pdfStream = parkingService.generateReceiptPdf(receipt);

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receipt_" + registrationNumber + ".pdf");

            // Return the PDF as a response
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(pdfStream));
        } catch (Exception e) {
            // Log the error
            log.error("Unexpected error occurred while generating the receipt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while processing the receipt request.");
        }
    }

    // Generate Bill for Exit
    @PostMapping("/exit")
    public ResponseEntity<String> generateBill(@RequestParam String registrationNumber) {
        try {
            String response = parkingService.generateBill(registrationNumber);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the error
            log.error("Error occurred while generating bill for registration number: {}", registrationNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while generating the bill for the registration number: " + registrationNumber + "Please try again later.");
        }
    }

    // Pay Bill and Release Parking Slot
    @PostMapping("/pay")
    public ResponseEntity<String> payBill(@RequestParam String registrationNumber) {
        try {
            String response = parkingService.payBill(registrationNumber);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Handle invalid registration number
            log.error("Invalid registration number: {}", registrationNumber, e);
            return ResponseEntity.badRequest().body("The provided registration number is invalid.");
        } catch (Exception e) {
            // Log the exception for debugging purposes
            log.error("Error occurred while processing the payment for registration number: {}", registrationNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the payment for the registration number " + registrationNumber + ". The payment may have already been completed or the registration number may not be available in the parking space.");
        }
    }

    // Get Vehicle History (Optional for Admin)
    @GetMapping("/history")
    public ResponseEntity<?> getVehicleHistory() {
        try {
            List<History> historyList = parkingService.getVehicleHistory();
            return ResponseEntity.ok(historyList);
        } catch (Exception e) {
            // Log the error
            log.error("Error occurred while fetching vehicle history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("There was an issue fetching the vehicle history. Please try again later.");
        }
    }

    @GetMapping("/available-parking")
    public ResponseEntity<?> getAvailableParking() {
        try {
            List<ParkingLot> parkingList = parkingService.getAvailableParkingSpaces();
            return ResponseEntity.ok(parkingList);
        } catch (Exception e) {
            // Log the error
            log.error("Error occurred while fetching available parking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("There was an issue fetching the available parking spaces. Please try again later.");
        }
    }

    @GetMapping("/receipt")
    public ResponseEntity<?> getReceipt(@RequestParam String registrationNumber) {
        try {
            Receipt receipt = parkingService.getReceiptByRegistrationNumber(registrationNumber);
            if (receipt == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Receipt not found for the provided registration number.");
            }
            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            // Log the error
            log.error("Error occurred while fetching receipt for registration number: {}", registrationNumber);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("There was an issue fetching the receipt for the registration number: " + registrationNumber + " Please try again later.");
        }
    }

    @GetMapping("/available-vehicles")
    public ResponseEntity<?> getAvailableVehicles() {
        try {
            List<Vehicle> vehicles = parkingService.getAvailableVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            // Log the error
            log.error("Error occurred while fetching available vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("There was an issue fetching the available vehicles. Please try again later.");
        }
    }

    @GetMapping("/bookings")
    public List<BookingResponse> getAllBookings() {
        return parkingService.getAllVehicleDetails();
    }
}
