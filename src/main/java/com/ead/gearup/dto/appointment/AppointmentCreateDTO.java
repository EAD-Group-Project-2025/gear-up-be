package com.ead.gearup.dto.appointment;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentCreateDTO {

    private LocalDate date;
    private String notes;
    private Long vehicleId;

}
