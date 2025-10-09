package com.ead.gearup.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.ead.gearup.model.Appointment;
import com.ead.gearup.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AppointmentGraphQLController {

    private final AppointmentRepository appointmentRepository;

    @QueryMapping
    public List<Appointment> searchAppointmentsByCustomerName(@Argument String name) {
        return appointmentRepository.findByCustomerUserName(name);
    }
}
