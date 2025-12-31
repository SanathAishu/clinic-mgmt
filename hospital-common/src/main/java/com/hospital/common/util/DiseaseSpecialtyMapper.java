package com.hospital.common.util;

import com.hospital.common.enums.Disease;
import com.hospital.common.enums.Specialty;

import java.util.Map;

/**
 * Maps diseases to appropriate medical specialties
 */
public class DiseaseSpecialtyMapper {

    private static final Map<Disease, Specialty> DISEASE_TO_SPECIALTY = Map.ofEntries(
        Map.entry(Disease.DIABETES, Specialty.ENDOCRINOLOGY),
        Map.entry(Disease.HYPERTENSION, Specialty.CARDIOLOGY),
        Map.entry(Disease.ASTHMA, Specialty.PULMONOLOGY),
        Map.entry(Disease.HEART_DISEASE, Specialty.CARDIOLOGY),
        Map.entry(Disease.ARTHRITIS, Specialty.ORTHOPEDICS),
        Map.entry(Disease.CANCER, Specialty.ONCOLOGY),
        Map.entry(Disease.TUBERCULOSIS, Specialty.PULMONOLOGY),
        Map.entry(Disease.COVID_19, Specialty.PULMONOLOGY),
        Map.entry(Disease.PNEUMONIA, Specialty.PULMONOLOGY),
        Map.entry(Disease.MALARIA, Specialty.GENERAL_MEDICINE),
        Map.entry(Disease.DENGUE, Specialty.GENERAL_MEDICINE),
        Map.entry(Disease.TYPHOID, Specialty.GENERAL_MEDICINE),
        Map.entry(Disease.KIDNEY_DISEASE, Specialty.NEPHROLOGY),
        Map.entry(Disease.LIVER_DISEASE, Specialty.GASTROENTEROLOGY),
        Map.entry(Disease.THYROID_DISORDER, Specialty.ENDOCRINOLOGY),
        Map.entry(Disease.MENTAL_HEALTH_DISORDER, Specialty.PSYCHIATRY),
        Map.entry(Disease.SKIN_DISEASE, Specialty.DERMATOLOGY),
        Map.entry(Disease.EYE_DISEASE, Specialty.OPHTHALMOLOGY),
        Map.entry(Disease.ENT_DISORDER, Specialty.ENT),
        Map.entry(Disease.NEUROLOGICAL_DISORDER, Specialty.NEUROLOGY),
        Map.entry(Disease.GASTROINTESTINAL_DISORDER, Specialty.GASTROENTEROLOGY),
        Map.entry(Disease.RESPIRATORY_DISORDER, Specialty.PULMONOLOGY),
        Map.entry(Disease.BONE_FRACTURE, Specialty.ORTHOPEDICS),
        Map.entry(Disease.OTHER, Specialty.GENERAL_MEDICINE)
    );

    public static Specialty getSpecialtyForDisease(Disease disease) {
        return DISEASE_TO_SPECIALTY.getOrDefault(disease, Specialty.GENERAL_MEDICINE);
    }

    public static boolean isSpecialtyMatchingDisease(Disease disease, Specialty specialty) {
        Specialty expectedSpecialty = getSpecialtyForDisease(disease);
        return expectedSpecialty.equals(specialty) || specialty.equals(Specialty.GENERAL_MEDICINE);
    }
}
