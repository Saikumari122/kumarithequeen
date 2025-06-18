package com.doctor.availability.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import com.appointment.entity.AppointmentStatus;
import lombok.Data;

@Data
public class AppointmentStatusUpdateDTO {
    // The ID of the appointment to update
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    // The new status to apply to the appointment
    private boolean followUp;
}
