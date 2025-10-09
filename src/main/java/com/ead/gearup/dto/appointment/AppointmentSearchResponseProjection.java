package com.ead.gearup.dto.appointment;

import java.time.LocalDate;

public interface AppointmentSearchResponseProjection {
    Long getAppointmentId();

    LocalDate getDate();

    String getStatus();

    String getNotes();

}
