package com.ead.gearup.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ead.gearup.dto.appointment.AppointmentCreateDTO;
import com.ead.gearup.dto.appointment.AppointmentResponseDTO;
import com.ead.gearup.dto.appointment.AppointmentUpdateDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.service.AppointmentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // @RequiresRole({ UserRole.CUSTOMER })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> createAppointment(
            @RequestBody @Valid AppointmentCreateDTO appointmentCreateDTO, HttpServletRequest request) {
        AppointmentResponseDTO appointmentResponseDTO = appointmentService.createAppointment(appointmentCreateDTO);

        ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                .status("success")
                .message("Appointment created successfully")
                .data(appointmentResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping(value = "/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> updateAppointment(@PathVariable Long id,
            @RequestBody @Valid AppointmentUpdateDTO appointmentUpdateDTO, HttpServletRequest request) {

        AppointmentResponseDTO appointmentResponseDTO = appointmentService.updateAppointment(id, appointmentUpdateDTO);

        ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                .status("success")
                .message("Appointment updated successfully")
                .data(appointmentResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<ApiResponseDTO<AppointmentResponseDTO>> getAppointmentById(@PathVariable Long id,
    HttpServletRequest request) {
        AppointmentResponseDTO appointmentResponseDTO = appointmentService.getAppointmentById(id);

        ApiResponseDTO<AppointmentResponseDTO> response = ApiResponseDTO.<AppointmentResponseDTO>builder()
                .status("success")
                .message("Appointment updated successfully")
                .data(appointmentResponseDTO)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<List<AppointmentResponseDTO>>> getAllAppointments(HttpServletRequest request) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAllAppointments();

        ApiResponseDTO<List<AppointmentResponseDTO>> response = ApiResponseDTO.<List<AppointmentResponseDTO>>builder()
                .status("success")
                .message("Appointments retrieved successfully")
                .data(appointments)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteAppointment(@PathVariable Long id, HttpServletRequest request) {
        appointmentService.deleteAppointment(id);

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Appointment canceled successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

}
