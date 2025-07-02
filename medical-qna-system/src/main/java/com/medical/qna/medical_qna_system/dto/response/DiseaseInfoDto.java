package com.medical.qna.medical_qna_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseInfoDto {
    private String diseaseName;
    private String description;
    private List<String> symptoms;
    private List<String> treatments;
    private List<String> complications;
    private List<String> departments;
}