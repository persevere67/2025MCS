package com.medical.qna.medical_qna_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class DiseaseInfoDto {
    private String name;
    private String description;
    private List<String> symptoms;
    private List<String> treatments;
    private List<String> goodFoods;
    private List<String> badFoods;
    private List<String> complications;
    private String department;
    private String departmentCategory;
    private Boolean insuranceCovered;
    private String treatmentDuration;
    private String cureRate;
    private String treatmentCost;
    private String nursing;
}