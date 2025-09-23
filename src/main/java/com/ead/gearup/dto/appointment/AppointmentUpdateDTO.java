package com.ead.gearup.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

import com.ead.gearup.enums.AppointmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentUpdateDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String notes;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @Schema(example = "10:00:00", description = "Appointment start time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @Schema(example = "11:00:00", description = "Appointment end time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    private Long employeeId;
}
