package com.ead.gearup.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.exception.EmployeeNotFoundException;
import com.ead.gearup.model.Appointment;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.Task;
import com.ead.gearup.model.Vehicle;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentDTOConverter {

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;

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
        dto.setTaskIds(appointment.getTasks() != null
                ? appointment.getTasks().stream().map(Task::getTaskId).toList()
                : List.of());
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
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + dto.getEmployeeId())));
        }

        if (dto.getTaskIds() != null && !dto.getTaskIds().isEmpty()) {
            List<Task> tasks = taskRepository.findAllById(dto.getTaskIds());

            if (tasks.size() != dto.getTaskIds().size()) {
                throw new IllegalArgumentException("One or more tasks not found: " + dto.getTaskIds());
            }

            // link tasks to this appointment
            tasks.forEach(task -> task.setAppointment(appointment));

            // replace tasks list in appointment
            appointment.setTasks(tasks);
        }

        return appointment;
    }
}
