package com.doctor.availability.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class SetAvailabilityResponse {
	private Long doctorId;
	private LocalDate date;
	private List<String> busySlots;
}
