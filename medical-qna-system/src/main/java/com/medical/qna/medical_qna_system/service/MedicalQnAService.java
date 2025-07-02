package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.entity.neo4j.*;
import com.medical.qna.medical_qna_system.repository.neo4j.*; // 导入所有 Repository

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MedicalQnAService {

    private final DiseaseRepository diseaseRepository;
    private final SymptomRepository symptomRepository;
    private final DrugRepository drugRepository;
    private final FoodRepository foodRepository;
    private final CheckRepository checkRepository;
    private final DepartmentRepository departmentRepository;
    private final ProducerRepository producerRepository;

    // 注入所有 Repository
    public MedicalQnAService(DiseaseRepository diseaseRepository,
                             SymptomRepository symptomRepository,
                             DrugRepository drugRepository,
                             FoodRepository foodRepository,
                             CheckRepository checkRepository,
                             DepartmentRepository departmentRepository,
                             ProducerRepository producerRepository) {
        this.diseaseRepository = diseaseRepository;
        this.symptomRepository = symptomRepository;
        this.drugRepository = drugRepository;
        this.foodRepository = foodRepository;
        this.checkRepository = checkRepository;
        this.departmentRepository = departmentRepository;
        this.producerRepository = producerRepository;
    }

    /**
     * 根据关键词查询知识图谱，返回相关信息。
     * 这是您知识图谱查询的核心方法。
     *
     * @param keyword 用户输入的关键词 (例如 "高血压", "咳嗽", "阿司匹林")
     * @return 包含相关信息的 Map，键是信息类型，值是具体内容。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> queryKnowledgeGraphByKeyword(String keyword) {
        Map<String, Object> result = new HashMap<>();

        // 1. 尝试查询疾病
        Optional<Disease> diseaseOpt = diseaseRepository.findByName(keyword);
        if (diseaseOpt.isPresent()) {
            Disease disease = diseaseOpt.get();
            result.put("type", "疾病");
            result.put("name", disease.getName());
            result.put("description", disease.getDesc());
            result.put("prevent", disease.getPrevent());
            result.put("cause", disease.getCause());
            result.put("easy_get", disease.getEasy_get());
            result.put("cure_lasttime", disease.getCure_lasttime());
            result.put("cured_prob", disease.getCured_prob());
            result.put("cure_way", disease.getCure_way());

            // 获取关联信息
            result.put("symptoms", diseaseRepository.findSymptomsByDiseaseName(disease.getName())
                    .stream().map(Symptom::getName).collect(Collectors.toSet()));
            result.put("common_drugs", diseaseRepository.findCommonDrugsByDiseaseName(disease.getName())
                    .stream().map(Drug::getName).collect(Collectors.toSet()));
            result.put("recommand_drugs", diseaseRepository.findRecommandDrugsByDiseaseName(disease.getName())
                    .stream().map(Drug::getName).collect(Collectors.toSet()));
            result.put("no_eat_foods", diseaseRepository.findNoEatFoodsByDiseaseName(disease.getName())
                    .stream().map(Food::getName).collect(Collectors.toSet()));
            result.put("do_eat_foods", diseaseRepository.findDoEatFoodsByDiseaseName(disease.getName())
                    .stream().map(Food::getName).collect(Collectors.toSet()));
            result.put("recommand_eat_foods", diseaseRepository.findRecommandEatFoodsByDiseaseName(disease.getName())
                    .stream().map(Food::getName).collect(Collectors.toSet()));
            result.put("needed_checks", diseaseRepository.findChecksByDiseaseName(disease.getName())
                    .stream().map(Check::getName).collect(Collectors.toSet()));
            result.put("departments", diseaseRepository.findDepartmentsByDiseaseName(disease.getName())
                    .stream().map(Department::getName).collect(Collectors.toSet()));
            result.put("acompany_diseases", diseaseRepository.findAcompanyDiseasesByDiseaseName(disease.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));

            return result; // 找到了疾病，直接返回
        }

        // 2. 如果不是疾病，尝试查询症状
        Optional<Symptom> symptomOpt = symptomRepository.findByName(keyword);
        if (symptomOpt.isPresent()) {
            Symptom symptom = symptomOpt.get();
            result.put("type", "症状");
            result.put("name", symptom.getName());
            result.put("caused_by_diseases", symptomRepository.findDiseasesBySymptomName(symptom.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));
            return result;
        }

        // 3. 尝试查询药品
        Optional<Drug> drugOpt = drugRepository.findByName(keyword);
        if (drugOpt.isPresent()) {
            Drug drug = drugOpt.get();
            result.put("type", "药品");
            result.put("name", drug.getName());
            drugRepository.findProducerByDrugName(drug.getName()).ifPresent(p -> result.put("produced_by", p.getName()));
            result.put("common_for_diseases", drugRepository.findCommonForDiseasesByDrugName(drug.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));
            result.put("recommand_for_diseases", drugRepository.findRecommandForDiseasesByDrugName(drug.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));
            return result;
        }

        // 4. 尝试查询食物
        Optional<Food> foodOpt = foodRepository.findByName(keyword);
        if (foodOpt.isPresent()) {
            Food food = foodOpt.get();
            result.put("type", "食物");
            result.put("name", food.getName());
            result.put("do_eat_for_diseases", foodRepository.findDoEatForDiseadesByFoodName(food.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));
            result.put("no_eat_for_diseases", foodRepository.findNoEatForDiseasesByFoodName(food.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));
            result.put("recommand_eat_for_diseases", foodRepository.findRecommandEatForDiseasesByFoodName(food.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));
            return result;
        }

        // 5. 尝试查询检查项目
        Optional<Check> checkOpt = checkRepository.findByName(keyword);
        if (checkOpt.isPresent()) {
            Check check = checkOpt.get();
            result.put("type", "检查项目");
            result.put("name", check.getName());
            result.put("diseases_requiring_check", checkRepository.findDiseasesRequiringCheckByCheckName(check.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));
            return result;
        }

        // 6. 尝试查询科室
        Optional<Department> departmentOpt = departmentRepository.findByName(keyword);
        if (departmentOpt.isPresent()) {
            Department department = departmentOpt.get();
            result.put("type", "科室");
            result.put("name", department.getName());
            result.put("diseases_treated_here", departmentRepository.findDiseasesBelongingHereByDepartmentName(department.getName())
                    .stream().map(Disease::getName).collect(Collectors.toSet()));
            result.put("sub_departments", departmentRepository.findSubDepartmentsByDepartmentName(department.getName())
                    .stream().map(Department::getName).collect(Collectors.toSet()));
            return result;
        }

        // 7. 尝试查询生产商
        Optional<Producer> producerOpt = producerRepository.findByName(keyword);
        if (producerOpt.isPresent()) {
            Producer producer = producerOpt.get();
            result.put("type", "生产商");
            result.put("name", producer.getName());
            result.put("produced_drugs", producerRepository.findProducedDrugsByProducerName(producer.getName())
                    .stream().map(Drug::getName).collect(Collectors.toSet()));
            return result;
        }

        // 如果没有找到任何匹配的节点
        result.put("status", "未找到相关信息");
        return result;
    }
}