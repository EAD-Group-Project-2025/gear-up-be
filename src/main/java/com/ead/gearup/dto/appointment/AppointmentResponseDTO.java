package com.ead.gearup.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDTO {
    private Long id;
    private LocalDate date;
    private String status;
    private String notes;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long vehicleId;
    private Long customerId;
    private Long employeeId;
}
