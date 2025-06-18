package com.doctor.availability.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.appointment.dto.AppointmentUpdateDTO;
import com.appointment.entity.Appointment;
import com.appointment.service.AppointmentService;
import com.doctor.availability.dto.AppointmentStatusUpdateDTO;
import com.doctor.availability.dto.DoctorAvailabilityResponse;
import com.doctor.availability.dto.DoctorScheduleDTO;
import com.doctor.availability.dto.DoctorUnblockScheduleDTO;
import com.doctor.availability.dto.SetAvailabilityResponse;
import com.doctor.availability.entity.Availability;
import com.doctor.availability.service.AvailabilityService;

@RestController
@RequestMapping("/api/availability")
public class DoctorAvailabilityController {
	
	@Autowired
	private AppointmentService appointmentService;
	
    @Autowired
    private AvailabilityService availabilityService;
    
    /**
     * Public endpoint to retrieve a doctor's availability for a specified date.
     * Example: GET /api/availability?doctorId=10&date=2026-08-15
     */
    @GetMapping("/getAvailability")
    public ResponseEntity<DoctorAvailabilityResponse> getAvailability(
            @RequestParam Long doctorId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        // The service method returns available slots (excluding busy slots and disallowed periods)
        List<LocalTime> availableSlots = availabilityService.getAvailableSlots(doctorId, localDate);

        // Map LocalTime to String for the JSON response
        List<String> availableSlotStrings = availableSlots.stream()
                .map(LocalTime::toString)
                .collect(Collectors.toList());

        // Prepare the response DTO
        DoctorAvailabilityResponse response = new DoctorAvailabilityResponse(doctorId, localDate, availableSlotStrings);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Protected endpoint for a doctor to set or update availability.
     * This endpoint is secured; only an authenticated doctor should be allowed to update their own availability.
     * Example: POST /api/availability with a JSON body containing doctor details, date, and available time slots.
     */
    @PostMapping("/setAvailability")
    public ResponseEntity<SetAvailabilityResponse> blockAvailability(@RequestBody DoctorScheduleDTO doctorScheduleDTO) {
    	SetAvailabilityResponse updated = availabilityService.blockAvailability(
                doctorScheduleDTO.getDoctorId(),
                doctorScheduleDTO.getDate(),
                doctorScheduleDTO.getBusySlots());
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Protected endpoint to update the status of an appointment.
     * For example, after an appointment you may mark it as COMPLETED or CANCELLED.
     * Example: PUT /api/availability/appointments/42?status=COMPLETED
     */
    @PutMapping("/unblock")
    public ResponseEntity<String> unblockAvailabilitySlots(
            @RequestBody DoctorUnblockScheduleDTO doctorUnblockScheduleDTO) {
        
        availabilityService.unblockAvailability(
                doctorUnblockScheduleDTO.getDoctorId(),
                doctorUnblockScheduleDTO.getDate(),
                doctorUnblockScheduleDTO.getUnblockSlots());
        
        return ResponseEntity.ok("Slots unblocked successfully");
    }

    
    @PutMapping("/followup/{appointmentId}")
    public ResponseEntity<Appointment> createFollowUpAppointment(@PathVariable Long appointmentId, @RequestBody AppointmentStatusUpdateDTO requestDTO) {
        // Force the followUp flag to true for follow-up appointments
        AppointmentUpdateDTO newRequestDTO  = new AppointmentUpdateDTO();
        newRequestDTO.setAppointmentDate(requestDTO.getAppointmentDate());
        newRequestDTO.setAppointmentTime(requestDTO.getAppointmentTime());
        newRequestDTO.setFollowUp(requestDTO.isFollowUp());
        
        // Delegate appointment creation to your existing service
        Appointment followUpAppointment = appointmentService.partialUpdateAppointment(appointmentId,newRequestDTO);
        return ResponseEntity.ok(followUpAppointment);
    }

}

