package com.car.parking.service;

import com.car.parking.model.*;
import com.car.parking.repository.HistoryRepository;
import com.car.parking.repository.ParkingLotRepository;
import com.car.parking.repository.ReceiptRepository;
import com.car.parking.repository.VehicleRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private static final String AVAILABLE_STATUS = "Available";
    private static final String UNPAID_STATUS = "UNPAID";
    private static final String OCCUPIED_STATUS = "Occupied";
    private final VehicleRepository vehicleRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final HistoryRepository historyRepository;
    private final ReceiptRepository receiptRepository;

    private static BookingResponse getBookingResponse(Vehicle vehicle, ParkingLot matchingParkingLot) {
        BookingResponse bookingResponse = new BookingResponse();
        bookingResponse.setRegistrationNumber(vehicle.getRegistrationNumber());
        bookingResponse.setOwnerName(vehicle.getOwnerName());
        bookingResponse.setPhoneNumber(vehicle.getPhoneNumber());
        bookingResponse.setVehicleType(vehicle.getVehicleType());
        bookingResponse.setZone(matchingParkingLot != null ? matchingParkingLot.getZone() : null);
        bookingResponse.setSlot(matchingParkingLot != null ? matchingParkingLot.getSlot() : null);

        // Set additional fields
        String status = matchingParkingLot != null ? matchingParkingLot.getBookedSlotStatus() : AVAILABLE_STATUS;
        bookingResponse.setStatus(status);
        bookingResponse.setEntryTime(vehicle.getEntryTime() != null ? vehicle.getEntryTime().toString() : null);

        // Exit time logic can depend on your system; for now, set it as null or calculate based on conditions
        bookingResponse.setExitTime(null); // Add logic to populate this field if required
        return bookingResponse;
    }

    // Parking Initialization - can be done only once

    public String initializeParking() {
        List<ParkingLot> parkingLots = parkingLotRepository.findAll();
        if (!parkingLots.isEmpty()) {
            return "Parking lot is already initialized.";
        }

        // Initialize the parking slots
        for (char zone = 'A'; zone <= 'E'; zone++) {
            for (int i = 1; i <= 10; i++) {
                ParkingLot parkingLot = new ParkingLot();
                parkingLot.setZone(String.valueOf(zone));
                parkingLot.setSlot(zone + String.valueOf(i));
                parkingLot.setBookedSlotStatus(AVAILABLE_STATUS);
                parkingLotRepository.save(parkingLot);
            }
        }
        return "Parking lot initialized successfully.";
    }

    public String registerVehicle(Vehicle vehicle, String zone, String slot) {
        // Check if a vehicle with the same registration number already exists
        if (vehicleRepository.existsById(vehicle.getRegistrationNumber())) {
            return "Vehicle with registration number " + vehicle.getRegistrationNumber() + " is already registered.";
        }

        // Find the parking lot based on zone, slot, and availability
        ParkingLot parkingLot = parkingLotRepository.findByZoneAndSlotAndBookedSlotStatus(zone, slot, AVAILABLE_STATUS);

        if (parkingLot == null) {
            return "Parking slot does not exist or is already occupied.";
        }

        // Register the vehicle
        vehicle.setEntryTime(LocalDateTime.now());
        vehicleRepository.save(vehicle);

        // Assign parking slot to the vehicle
        parkingLot.setBookedSlotStatus(OCCUPIED_STATUS);
        parkingLot.setVehicleRegistrationNumber(vehicle.getRegistrationNumber());
        parkingLotRepository.save(parkingLot);

        return "Vehicle registered and assigned to Zone: " + zone + " and slot " + slot;
    }

    // Generate Bill for Exit
    public String generateBill(String registrationNumber) {
        Vehicle vehicle = vehicleRepository.findById(registrationNumber)
                .orElseThrow(() -> new RuntimeException("Vehicle not found!"));

        ParkingLot parkingLot = parkingLotRepository.findAll().stream()
                .filter(lot -> registrationNumber.equals(lot.getVehicleRegistrationNumber()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Parking slot not found!"));

        LocalDateTime exitTime = LocalDateTime.now();
        long duration = ChronoUnit.MINUTES.between(vehicle.getEntryTime(), exitTime);
        double amount = calculateBillingAmount(duration, vehicle.getVehicleType());

        // Save the bill history
        History history = new History();
        history.setRegistrationNumber(vehicle.getRegistrationNumber());
        history.setVehicleType(vehicle.getVehicleType());
        history.setOwnerName(vehicle.getOwnerName());
        history.setPhoneNumber(vehicle.getPhoneNumber());
        history.setEntryTime(vehicle.getEntryTime());
        history.setExitTime(exitTime);
        history.setTotalDuration(duration);
        history.setAmount(amount);
        history.setParkingZone(parkingLot.getZone());
        history.setParkingSlot(parkingLot.getSlot());
        history.setStatus(UNPAID_STATUS);
        historyRepository.save(history);

        // Save receipt
        Receipt receipt = new Receipt();
        receipt.setRegistrationNumber(vehicle.getRegistrationNumber());
        receipt.setVehicleType(vehicle.getVehicleType());
        receipt.setOwnerName(vehicle.getOwnerName());
        receipt.setPhoneNumber(vehicle.getPhoneNumber());
        receipt.setTotalDuration(duration);
        receipt.setAmount(amount);
        receipt.setReceiptDate(LocalDateTime.now());
        receipt.setStatus(UNPAID_STATUS);
        receiptRepository.save(receipt);

        amount = Math.round(amount * 100.0) / 100.0;
        return "Bill generated for " + vehicle.getVehicleType() + ": Rs " + amount + ". Please pay to release your vehicle.";
    }

    // Calculate Billing Amount based on Vehicle Type
    private double calculateBillingAmount(long duration, String vehicleType) {
        double ratePerHour = switch (vehicleType.toLowerCase()) {
            case "car" -> 30.0;
            case "motorcycle", "scooter" -> 10.0;
            case "van", "bus" -> 50.0;
            default -> throw new IllegalArgumentException("Unknown vehicle type: " + vehicleType);
        };

        return (duration / 60.0) * ratePerHour; // Calculate total cost
    }

    public String payBill(String registrationNumber) {
        // Fetch the unpaid history entry directly
        History history = historyRepository.findByRegistrationNumberAndStatus(registrationNumber, UNPAID_STATUS);

        // If history is not found, throw an error
        if (history == null) {
            throw new IllegalArgumentException("No unpaid bill found for the provided registration number.");
        }

        // Update the status to PAID
        history.setStatus("PAID");
        historyRepository.save(history);

        // Fetch the parking lot entry directly
        ParkingLot parkingLot = parkingLotRepository.findByVehicleRegistrationNumber(registrationNumber);

        // Update the parking lot status to available
        parkingLot.setBookedSlotStatus(AVAILABLE_STATUS);
        parkingLot.setVehicleRegistrationNumber(null); // Clear the vehicle's registration
        parkingLotRepository.save(parkingLot);

        Receipt receipt = new Receipt();
        receipt.setStatus("PAID");
        receiptRepository.save(receipt);

        return "Payment received. Vehicle released and parking slot is available.";

    }

    // Get Vehicle History

    public List<History> getVehicleHistory() {
        return historyRepository.findAll();
    }

    public List<ParkingLot> getAvailableParkingSpaces() {
        // Fetch all available parking spaces (status = "Available")
        return parkingLotRepository.findAll();
    }

    public Receipt getReceiptByRegistrationNumber(String registrationNumber) {
        return receiptRepository.findAll().stream()
                .filter(r -> registrationNumber.equals(r.getRegistrationNumber()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Receipt not found for registration number: " + registrationNumber));
    }

    public ByteArrayInputStream generateReceiptPdf(Receipt receipt) {
        try {
            // Format the receipt date
            String formattedDate = formatDateTime(receipt.getReceiptDate().toString());

            // HTML content with inline CSS and media queries for thermal receipt size and adjusted barcode size
            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<style>\n" +
                    "body {\n" +
                    "    font-family: Arial, sans-serif;\n" +
                    "    margin: 0;\n" +
                    "    padding: 0;\n" +
                    "    text-align: center;\n" +
                    "    max-width: 100%;\n" +
                    "    box-sizing: border-box;\n" +
                    "}\n" +
                    ".container {\n" +
                    "    display: flex;\n" +
                    "    flex-direction: column;\n" +
                    "    align-items: center;\n" +
                    "    justify-content: space-between;\n" +
                    "    padding: 5px;\n" +
                    "    width: 90%;\n" +
                    "    margin: 0 auto;\n" +
                    "    border: 1px solid #000;\n" +
                    "    box-sizing: border-box;\n" +
                    "    height: 100%;\n" +
                    "}\n" +
                    "h1 {\n" +
                    "    font-size: 14px;\n" +
                    "    margin-bottom: 5px;\n" +
                    "    padding-bottom: 5px;\n" +
                    "}\n" +
                    ".details {\n" +
                    "    text-align: left;\n" +
                    "    margin-bottom: 5px;\n" +
                    "}\n" +
                    ".details p {\n" +
                    "    margin: 2px 0;\n" +
                    "    font-size: 9px;\n" +
                    "}\n" +
                    ".barcode {\n" +
                    "    margin-top: 2px;\n" +
                    "    text-align: center;\n" +
                    "    padding: 0;\n" +
                    "    margin-bottom: 5px;\n" +
                    "}\n" +
                    "img {\n" +
                    "    width: 100%;\n" +
                    "    max-height: 25px;\n" +
                    "}\n" +
                    "@media print {\n" +
                    "  body { width: 100%; }\n" +
                    "  .container { width: 90%; }\n" +
                    "  h1, .details p { font-size: 12px; }\n" +
                    "}\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class='container'>\n" +
                    "    <h1>Parking Receipt</h1>\n" +
                    "    <div class='details'>\n" +
                    "        <p><strong>Registration Number:</strong> " + receipt.getRegistrationNumber() + "</p>\n" +
                    "        <p><strong>Vehicle Type:</strong> " + receipt.getVehicleType() + "</p>\n" +
                    "        <p><strong>Owner Name:</strong> " + receipt.getOwnerName() + "</p>\n" +
                    "        <p><strong>Phone Number:</strong> " + receipt.getPhoneNumber() + "</p>\n" +
                    "        <p><strong>Total Duration:</strong> " + receipt.getTotalDuration() + " minutes</p>\n" +
                    "        <p><strong>Amount:</strong> Rs " + receipt.getAmount() + "</p>\n" +
                    "        <p><strong>Receipt Date:</strong> " + formattedDate + "</p>\n" +
                    "    </div>\n" +
                    "    <div class='barcode'>\n" +
                    "        <img src='https://barcode.tec-it.com/barcode.ashx?data=" + receipt.getRegistrationNumber() + "&amp;code=Code128&amp;translate-esc=false' alt='Barcode' />\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>";
            // Generate PDF
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();

            // Set the page size for the thermal receipt
            float pageWidth = 90.5f;
            float pageHeight = 100f;  // Adjust height as needed for content

            builder.useDefaultPageSize(pageWidth, pageHeight, PdfRendererBuilder.PageSizeUnits.MM);

            builder.withHtmlContent(htmlContent, null);
            builder.toStream(byteArrayOutputStream);
            builder.run();

            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String formatDateTime(String receiptDate) {

        // Parse the receipt date (ISO 8601 format with milliseconds)
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        LocalDateTime dateTime = LocalDateTime.parse(receiptDate, inputFormatter);

        // Format the date into DD/MM/YYYY HH:mm:ss
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateTime.format(outputFormatter);


    }

    public List<Vehicle> getAvailableVehicles() {
        // Fetch parking lots where the status is "Available"
        return parkingLotRepository.findAll().stream()
                .filter(slot -> OCCUPIED_STATUS.equalsIgnoreCase(slot.getBookedSlotStatus()))  // Only occupied slots
                .map(slot -> vehicleRepository.findById(slot.getVehicleRegistrationNumber())
                        .orElse(null))  // Find the vehicle by registration number
                .filter(Objects::nonNull)  // Filter out null values if no vehicle is found
                .toList();  // Collect the available vehicles
    }

    public List<BookingResponse> getAllVehicleDetails() {
        List<Vehicle> vehicles = vehicleRepository.findAll(); // Fetch all vehicles
        List<ParkingLot> occupiedParkingLots = parkingLotRepository.findByVehicleRegistrationNumberIsNotNull(); // Fetch parking lots with vehicles

        List<BookingResponse> bookingResponses = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            ParkingLot matchingParkingLot = occupiedParkingLots.stream()
                    .filter(parkingLot -> parkingLot.getVehicleRegistrationNumber().equals(vehicle.getRegistrationNumber()))
                    .findFirst()
                    .orElse(null);

            BookingResponse bookingResponse = getBookingResponse(vehicle, matchingParkingLot);

            bookingResponses.add(bookingResponse);
        }
        // Filter the final response list to include only available slots
        return bookingResponses.stream()
                .filter(response -> OCCUPIED_STATUS.equalsIgnoreCase(response.getStatus())) // Keep only responses with status "AVAILABLE"
                .toList();
    }
}
