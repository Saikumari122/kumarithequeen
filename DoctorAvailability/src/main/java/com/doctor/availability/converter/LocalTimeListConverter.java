package com.doctor.availability.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class LocalTimeListConverter implements AttributeConverter<List<LocalTime>, String> {

    private static final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(List<LocalTime> attribute) {
        return attribute != null ? attribute.stream()
                .map(LocalTime::toString)
                .collect(Collectors.joining(SEPARATOR)) : null;
    }

    @Override
    public List<LocalTime> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return List.of(); // or new ArrayList<>(), as preferred
        }
        return Arrays.stream(dbData.split(SEPARATOR))
                     .map(LocalTime::parse)
                     .collect(Collectors.toList());
    }

}
