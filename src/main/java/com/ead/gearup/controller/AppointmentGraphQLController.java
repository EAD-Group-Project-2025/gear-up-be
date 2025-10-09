package com.ead.gearup.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.ead.gearup.dto.appointment.AppointmentSearchResponseDTO;
import com.ead.gearup.service.AppointmentService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AppointmentGraphQLController {

    private final AppointmentService appointmentService;

    @QueryMapping
    public List<AppointmentSearchResponseDTO> searchAppointmentsByCustomerName(@Argument String name) {
        return appointmentService.searchAppointmentsByCustomerName(name);
    }
}
