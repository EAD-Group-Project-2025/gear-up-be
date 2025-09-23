package com.ead.gearup.util;

import org.springframework.stereotype.Component;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.model.Appointment;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentDTOConverter {

    private final EmployeeRepository employeeRepository;

    // Convert AppointmentDTO to Appointment entity
    public Appointment convertToEntity(AppointmentCreateDTO dto, Vehicle vehicle, Customer customer) {
        Appointment appointment = new Appointment();

        appointment.setDate(dto.getDate());
        appointment.setNotes(dto.getNotes());
        appointment.setVehicle(vehicle);
        appointment.setCustomer(customer);

        return appointment;
    }

    public AppointmentResponseDTO convertToResponseDto(Appointment appointment) {
        AppointmentResponseDTO dto = new AppointmentResponseDTO();

        dto.setId(appointment.getAppointmentId());
        dto.setDate(appointment.getDate());
        dto.setNotes(appointment.getNotes());
        dto.setStatus(appointment.getStatus().name());
        dto.setVehicleId(appointment.getVehicle().getVehicleId());
        dto.setCustomerId(appointment.getCustomer().getCustomerId());
        dto.setEmployeeId(appointment.getEmployee() != null ? appointment.getEmployee().getEmployeeId() : null);
        dto.setStartTime(appointment.getStartTime() != null ? appointment.getStartTime() : null);
        dto.setEndTime(appointment.getEndTime() != null ? appointment.getEndTime() : null);
        return dto;
    }

    public Appointment updateEntityFromDto(Appointment appointment, AppointmentUpdateDTO dto) {

        if (dto.getDate() != null) {
            appointment.setDate(dto.getDate());
        }

        if (dto.getNotes() != null) {
            appointment.setNotes(dto.getNotes());
        }

        if (dto.getStatus() != null) {
            appointment.setStatus(dto.getStatus());
        }

        if (dto.getStartTime() != null) {
            appointment.setStartTime(dto.getStartTime());
        }

        if (dto.getEndTime() != null) {
            appointment.setEndTime(dto.getEndTime());
        }

        if (dto.getEmployeeId() != null) {
            appointment.setEmployee(employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + dto.getEmployeeId())));
        }

        return appointment;
    }
}
