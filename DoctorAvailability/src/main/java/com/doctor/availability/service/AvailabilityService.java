package com.doctor.availability.service;

import com.doctor.availability.dto.SetAvailabilityResponse;
import com.doctor.availability.entity.Availability;
import com.doctor.availability.repository.AvailabilityRepository;
import com.example.demo.entity.Doctor;
import com.example.demo.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    // Working hours and schedule constants
    private final LocalTime WORK_START = LocalTime.of(9, 30);
    private final LocalTime WORK_END = LocalTime.of(18, 0);
    private final LocalTime LUNCH_START = LocalTime.of(13, 0);
    private final LocalTime LUNCH_END = LocalTime.of(14, 30);
    private final LocalTime FOLLOWUP_ALLOWED_START = LocalTime.of(16, 0);
    private final int SLOT_DURATION_MINUTES = 30;

    /**
     * Blocks the specified busy slots for a doctor on a given date.
     * If an availability record already exists, it merges the new busy slots (avoiding duplicates).
     * Otherwise, it creates a new record for the doctor.
     *
     * @param doctorId     the doctor's ID.
     * @param date         the date for which to block slots.
     * @param busySlotsStr the list of busy slot strings (e.g., ["11:00", "11:30"]).
     * @return a response DTO containing the updated busy slots for the doctor.
     */
    public SetAvailabilityResponse blockAvailability(Long doctorId, LocalDate date, List<String> busySlotsStr) {
        // Convert the list of busy slot strings to LocalTime objects.
        List<LocalTime> busySlotTimes = busySlotsStr.stream()
                .map(LocalTime::parse)
                .collect(Collectors.toList());
        for(LocalTime lt: busySlotTimes) {
        	if(lt.isAfter(LUNCH_START) && lt.isBefore(LUNCH_END)) {
        		throw new RuntimeException("cannot block a slot during lunch hours");
        	}
        	if(lt.isBefore(WORK_START)) {
        		throw new RuntimeException("cannot block a slot before start of work");
        	}
        	if(lt.isAfter(WORK_END)) {
        		throw new RuntimeException("cannot block a slot after working hours");
        	}
        }
        Collections.sort(busySlotTimes);

        // Retrieve the availability record for the given doctor and date.
        Optional<Availability> optionalAvailability = availabilityRepository.findByDoctorIdAndDate(doctorId, date);
        Availability availability;
        if (optionalAvailability.isPresent()) {
            // Update the existing record by merging busy slots.
            availability = optionalAvailability.get();
            // For safety, ensure the date is updated (if needed).
            availability.setDate(date);
            List<LocalTime> existingBusySlots = availability.getBusySlots();
            if (existingBusySlots == null) {
                existingBusySlots = new ArrayList<>();
            }
            // Merge busy slots to avoid duplicates.
            Set<LocalTime> mergedBusySlots = new HashSet<>(existingBusySlots);
            mergedBusySlots.addAll(busySlotTimes);
            availability.setBusySlots(new ArrayList<>(mergedBusySlots));
        } else {
            // Create a new availability record.
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
            availability = new Availability();
            availability.setDoctor(doctor);
            availability.setDate(date);
            availability.setBusySlots(busySlotTimes);
        }

        // Save the updated or new availability record.
        Availability savedAvailability = availabilityRepository.save(availability);

        // Prepare the response DTO (converting LocalTime slots to String).
        SetAvailabilityResponse response = new SetAvailabilityResponse();
        response.setDoctorId(savedAvailability.getDoctor().getDoctorId());
        response.setDate(savedAvailability.getDate());
        response.setBusySlots(savedAvailability.getBusySlots().stream()
                .map(LocalTime::toString)
                .collect(Collectors.toList()));
        return response;
    }

    /**
     * Generates and returns a list of available appointment slots for a doctor on a specified date.
     * Slots are generated within the permitted working hours (excluding lunch time and follow-up periods)
     * and then filtered by removing those already marked as busy.
     *
     * @param doctorId the doctor's ID.
     * @param date     the date for which to get available slots.
     * @return a list of available time slots (LocalTime).
     */
    public List<LocalTime> getAvailableSlots(Long doctorId, LocalDate date) {
        List<LocalTime> potentialSlots = new ArrayList<>();

        // Generate morning slots starting at WORK_START until a slot would extend past LUNCH_START.
        LocalTime slot = WORK_START;
        while (!slot.plusMinutes(SLOT_DURATION_MINUTES).isAfter(LUNCH_START)) {
            potentialSlots.add(slot);
            slot = slot.plusMinutes(SLOT_DURATION_MINUTES);
        }

        // Generate afternoon slots starting at LUNCH_END until a slot would extend past FOLLOWUP_ALLOWED_START.
        slot = LUNCH_END;
        while (!slot.plusMinutes(SLOT_DURATION_MINUTES).isAfter(FOLLOWUP_ALLOWED_START)) {
            potentialSlots.add(slot);
            slot = slot.plusMinutes(SLOT_DURATION_MINUTES);
        }

        // Retrieve the busy slots from the availability record.
        Optional<Availability> availabilityOpt = availabilityRepository.findByDoctorIdAndDate(doctorId, date);
        List<LocalTime> busySlots = availabilityOpt.map(Availability::getBusySlots)
                .orElse(Collections.emptyList());

        // Filter out busy slots.
        return potentialSlots.stream()
                .filter(slotTime -> !busySlots.contains(slotTime))
                .collect(Collectors.toList());
    }

    /**
     * Unblocks (removes) the specified busy slots from the availability record.
     * If any provided slot is not currently blocked, an error is thrown.
     *
     * @param doctorId     the doctor's ID.
     * @param date         the date of the availability record.
     * @param unblockSlots the list of slot strings to unblock (e.g., ["11:00", "11:30"]).
     * @throws RuntimeException if the availability record is not found or a given slot is not blocked.
     */
    public void unblockAvailability(Long doctorId, LocalDate date, List<String> unblockSlots) {
        // Convert the list of slot strings into LocalTime objects.
        List<LocalTime> slotsToUnblock = unblockSlots.stream()
                .map(LocalTime::parse)
                .collect(Collectors.toList());

        // Retrieve the availability record for the given doctor and date.
        Optional<Availability> optionalAvailability = availabilityRepository.findByDoctorIdAndDate(doctorId, date);
        if (optionalAvailability.isEmpty()) {
            throw new RuntimeException("No availability record found for doctor ID " 
                    + doctorId + " on date " + date);
        }
        Availability availability = optionalAvailability.get();

        List<LocalTime> currentBusySlots = availability.getBusySlots();
        if (currentBusySlots == null || currentBusySlots.isEmpty()) {
            throw new RuntimeException("No busy slots to remove for doctor ID " + doctorId + " on date " + date);
        }

        // Verify that each slot to unblock is currently marked as busy.
        for (LocalTime slot : slotsToUnblock) {
            if (!currentBusySlots.contains(slot)) {
                throw new RuntimeException("Slot " + slot + " is not blocked and cannot be unblocked.");
            }
        }

        // Remove the specified slots.
        currentBusySlots.removeAll(slotsToUnblock);
        availability.setBusySlots(currentBusySlots);

        // Save the updated availability record.
        availabilityRepository.saveAndFlush(availability);
    }
}
