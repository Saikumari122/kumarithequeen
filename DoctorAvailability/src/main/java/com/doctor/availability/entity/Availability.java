package com.doctor.availability.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.doctor.availability.converter.LocalTimeListConverter;
import com.example.demo.entity.Doctor;

@Entity
@Table(name = "availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    @Id
    private Long doctorId; // Using doctorId as the primary key
    
    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;
    
    // Adding date to the same table
    @Column(nullable = false)
    private LocalDate date;

    // Storing busy slots as a serialized list in the same table
    @Column(name = "busy_slots")
    @Convert(converter = LocalTimeListConverter.class)
    private List<LocalTime> busySlots;
}
