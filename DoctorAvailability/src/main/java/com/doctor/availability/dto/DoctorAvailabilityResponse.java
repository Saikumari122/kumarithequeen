package com.doctor.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAvailabilityResponse {
    private Long doctorId;
    private LocalDate date;
    private List<String> availableSlots;
}
