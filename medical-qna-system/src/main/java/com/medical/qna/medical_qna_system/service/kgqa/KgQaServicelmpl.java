package com.medical.qna.system.service.impl;

import com.medical.qna.system.dto.response.AnswerResponse;
import com.medical.qna.system.dto.response.SemanticUnderstandingResult;
import com.medical.qna.system.entity.neo4j.*; // 导入所有Neo4j实体类
import com.medical.qna.system.repository.*; // 导入所有Neo4j Repository

import com.medical.qna.system.service.KgQaService;
import com.medical.qna.system.service.SemanticUnderstandingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 用于Neo4j事务

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional // 确保Neo4j操作在事务中
public class KgQaServiceImpl implements KgQaService {

    @Autowired
    private SemanticUnderstandingService semanticUnderstandingService;

    // 注入所有相关的 Neo4j Repository
    @Autowired
    private DiseaseRepository diseaseRepository;
    @Autowired
    private SymptomRepository symptomRepository;
    @Autowired
    private DrugRepository drugRepository;
    @Autowired
    private CheckRepository checkRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private FoodRepository foodRepository;
    @Autowired
    private ProducerRepository producerRepository;

    // 定义意图到知识图谱查询逻辑的映射
    // 这是一个简化，实际中每个意图可能对应更复杂的Cypher查询
    private static final Map<String, String> INTENT_TO_KG_PROPERTY_MAP = Map.of(
        "query_desc", "desc",
        "query_prevent", "prevent",
        "query_cause", "cause",
        "query_easy_get", "easy_get",
        "query_cure_lasttime", "cure_lasttime",
        "query_cured_prob", "cured_prob"
        // "cure_way" 比较特殊，可能是一个List<String>，需要特殊处理
    );

    // 定义关系到问题模板的映射
    private static final Map<String, String> RELATION_TO_QUESTION_TEMPLATE = Map.of(
        "has_symptom", "{entity_name}有什么症状？",
        "acompany_with", "{entity_name}会引起什么并发症？",
        "belongs_to_department", "{entity_name}看哪个科室？", // 对Disease->Department
        "recommand_drug", "{entity_name}推荐用什么药？",
        "common_drug", "{entity_name}常用药有哪些？",
        "no_eat", "{entity_name}不能吃什么？",
        "do_eat", "{entity_name}宜吃什么？",
        "recommand_eat", "{entity_name}推荐食谱有哪些？",
        "need_check", "{entity_name}需要做哪些检查？",
        "produces_drug", "{entity_name}是哪个厂家生产的？", // 对Drug<-Producer
        "drug_common_for", "{entity_name}是哪些病的常用药？", // 对Drug<-common_drug<-Disease
        "drug_recommand_for", "{entity_name}是哪些病的推荐药？", // 对Drug<-recommand_drug<-Disease
        "symptom_for_disease", "什么病会有{entity_name}这个症状？", // 对Symptom<-has_symptom<-Disease
        "check_for_disease", "{entity_name}能诊断哪些疾病？", // 对Check<-need_check<-Disease
        "sub_department", "{entity_name}下面有哪些科室？" // 对Department->subDepartments (可能需要调整关系名)
        // 注意：关系名应与Neo4j中的实际关系类型匹配
    );

    @Override
    public AnswerResponse answerQuestion(String question) {
        SemanticUnderstandingResult semanticResult = semanticUnderstandingService.getSemanticUnderstanding(question);

        String mainEntityName = null;
        String mainEntityType = null;

        if (semanticResult.getExtractedEntities() != null && !semanticResult.getExtractedEntities().isEmpty()) {
            List<SemanticUnderstandingResult.ExtractedEntity> entities = semanticResult.getExtractedEntities();
            // 优先选择疾病作为主要实体
            mainEntityName = entities.stream()
                .filter(e -> "Disease".equals(e.getType()))
                .map(SemanticUnderstandingResult.ExtractedEntity::getText)
                .findFirst()
                .orElse(entities.stream() // 如果没有疾病，再找症状
                    .filter(e -> "Symptom".equals(e.getType()))
                    .map(SemanticUnderstandingResult.ExtractedEntity::getText)
                    .findFirst()
                    .orElse(entities.stream() // 再找药物
                        .filter(e -> "Drug".equals(e.getType()))
                        .map(SemanticUnderstandingResult.ExtractedEntity::getText)
                        .findFirst()
                        .orElse(entities.stream() // 再找检查
                            .filter(e -> "Check".equals(e.getType()))
                            .map(SemanticUnderstandingResult.ExtractedEntity::getText)
                            .findFirst()
                            .orElse(null) // 实在没有就为空
                        )
                    )
                );
            if (mainEntityName != null) {
                 mainEntityType = entities.stream()
                    .filter(e -> e.getText().equals(mainEntityName))
                    .map(SemanticUnderstandingResult.ExtractedEntity::getType)
                    .findFirst()
                    .orElse(null);
            }
        }

        String answerContent = "抱歉，我未能理解您的问题或找到相关信息。";
        List<String> relatedQuestions = new ArrayList<>();

        if (mainEntityName != null && mainEntityType != null) {
            System.out.println("Main Entity identified: " + mainEntityName + " (Type: " + mainEntityType + ")");
            System.out.println("Predicted Intent: " + semanticResult.getPredictedIntent());

            // 核心知识图谱查询逻辑
            switch (semanticResult.getPredictedIntent()) {
                case "query_desc":
                case "query_prevent":
                case "query_cause":
                case "query_easy_get":
                case "query_cure_lasttime":
                case "query_cured_prob":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseProperty(mainEntityName, INTENT_TO_KG_PROPERTY_MAP.get(semanticResult.getPredictedIntent()));
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询" + INTENT_TO_KG_PROPERTY_MAP.get(semanticResult.getPredictedIntent()) + "。";
                    }
                    break;
                case "query_cure_way":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseCureWay(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询治疗方式。";
                    }
                    break;
                case "query_symptom":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseSymptoms(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询症状。";
                    }
                    break;
                case "query_acompany_disease":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseAcompanyDiseases(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询并发症。";
                    }
                    break;
                case "query_belongs_to_department":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseDepartments(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询所属科室。";
                    }
                    break;
                case "query_recommand_drug":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseRecommandDrugs(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询推荐药品。";
                    }
                    break;
                case "query_common_drug":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseCommonDrugs(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询常用药品。";
                    }
                    break;
                case "query_no_eat":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseNoEatFoods(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询忌吃食物。";
                    }
                    break;
                case "query_do_eat":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseDoEatFoods(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询宜吃食物。";
                    }
                    break;
                case "query_recommand_eat":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseRecommandEatFoods(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询推荐食谱。";
                    }
                    break;
                case "query_need_check":
                    if ("Disease".equals(mainEntityType)) {
                        answerContent = queryDiseaseChecks(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有疾病实体支持查询诊断检查。";
                    }
                    break;
                case "query_drug_producer":
                    if ("Drug".equals(mainEntityType)) {
                        answerContent = queryDrugProducer(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有药物实体支持查询生产厂家。";
                    }
                    break;
                case "query_drug_effect_disease": // 药物可以治疗哪些疾病 (common_drug 和 recommand_drug的反向)
                    if ("Drug".equals(mainEntityType)) {
                        answerContent = queryDrugEffectDiseases(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有药物实体支持查询治疗疾病。";
                    }
                    break;
                case "query_symptom_disease": // 症状可以是什么病
                    if ("Symptom".equals(mainEntityType)) {
                        answerContent = querySymptomDiseases(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有症状实体支持查询相关疾病。";
                    }
                    break;
                case "query_check_diagnose_disease": // 检查能诊断什么病
                    if ("Check".equals(mainEntityType)) {
                        answerContent = queryCheckDiagnoseDiseases(mainEntityName);
                    } else {
                        answerContent = "抱歉，只有检查实体支持查询诊断疾病。";
                    }
                    break;
                // 其他意图，如 "query_department_subdepartment" (科室下属科室，需要Department实体)
                // case "query_department_subdepartment":
                //     if ("Department".equals(mainEntityType)) {
                //         answerContent = querySubDepartments(mainEntityName);
                //     } else {
                //         answerContent = "抱歉，只有科室实体支持查询下属科室。";
                //     }
                //     break;

                case "unknown_intent":
                    answerContent = "抱歉，我识别出了实体 '" + mainEntityName + "' 但未能明确您的意图，请尝试换种问法。";
                    break;
                default:
                    answerContent = "我识别出了实体 '" + mainEntityName + "' 和意图 '" + semanticResult.getPredictedIntent() + "'。但目前我还没有针对该意图的精确回答逻辑。";
                    break;
            }

            // 生成推荐问题
            List<Neo4jRelationTuple> graphRelations = getGraphRelationsForEntity(mainEntityName, mainEntityType);
            relatedQuestions = generateRelatedQuestionsFromGraph(mainEntityName, graphRelations, 3);

        } else if ("greeting".equals(semanticResult.getPredictedIntent())) {
            answerContent = "您好！我是医疗问答机器人，请问有什么可以帮助您的？";
            relatedQuestions.add("感冒有什么症状？");
            relatedQuestions.add("高血压怎么治疗？");
            relatedQuestions.add("糖尿病不能吃什么？");
        } else {
            answerContent = "抱歉，我未能识别出您问题中的主要医疗实体，也无法理解您的意图。";
        }

        AnswerResponse response = new AnswerResponse();
        response.setAnswer(answerContent);
        response.setRelatedQuestions(relatedQuestions);
        return response;
    }

    // ======================== Neo4j 查询辅助方法 ========================
    // 这些方法将直接调用您的 Spring Data Neo4j Repository
    // 我只给出示例结构，您需要根据实际的 Repository 方法和 Cypher 查询来实现它们

    // 辅助类，用于封装Neo4j查询返回的关系信息
    private static class Neo4jRelationTuple {
        public String relationType; // 关系类型
        public String targetName;   // 目标节点名称
        public String targetType;   // 目标节点类型 (标签)

        public Neo4jRelationTuple(String relationType, String targetName, String targetType) {
            this.relationType = relationType;
            this.targetName = targetName;
            this.targetType = targetType;
        }
    }

    /**
     * 查询疾病的某个属性 (desc, prevent, cause, easy_get, cure_lasttime, cured_prob)
     */
    private String queryDiseaseProperty(String diseaseName, String propertyName) {
        return diseaseRepository.findDiseasePropertyByName(diseaseName, propertyName)
                .map(propValue -> diseaseName + "的" + getPropertyNameChinese(propertyName) + "是：" + propValue + "。")
                .orElse("抱歉，未能找到" + diseaseName + "的" + getPropertyNameChinese(propertyName) + "信息。");
    }

    // 辅助方法，将属性名转换为中文
    private String getPropertyNameChinese(String propertyName) {
        return switch (propertyName) {
            case "desc" -> "描述";
            case "prevent" -> "预防方法";
            case "cause" -> "病因";
            case "easy_get" -> "易感人群";
            case "cure_lasttime" -> "治疗持续时间";
            case "cured_prob" -> "治愈概率";
            default -> propertyName;
        };
    }

    /**
     * 查询疾病的治疗方式 (cure_way 属性，List<String>)
     */
    private String queryDiseaseCureWay(String diseaseName) {
        return diseaseRepository.findDiseaseCureWayByName(diseaseName)
                .map(cureWays -> {
                    if (cureWays != null && !cureWays.isEmpty()) {
                        return diseaseName + "的治疗方法包括：" + String.join("、", cureWays) + "。";
                    }
                    return "抱歉，未能找到" + diseaseName + "的治疗方法信息。";
                })
                .orElse("抱歉，未能找到" + diseaseName + "的治疗方法信息。");
    }


    /**
     * 查询疾病的症状
     */
    private String queryDiseaseSymptoms(String diseaseName) {
        Set<Symptom> symptoms = diseaseRepository.findSymptomsByDiseaseName(diseaseName);
        if (symptoms != null && !symptoms.isEmpty()) {
            return diseaseName + "的常见症状有：" +
                   symptoms.stream().map(Symptom::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + diseaseName + "的症状信息。";
    }

    /**
     * 查询疾病的并发症
     */
    private String queryDiseaseAcompanyDiseases(String diseaseName) {
        Set<Disease> acompanyDiseases = diseaseRepository.findAcompanyDiseasesByDiseaseName(diseaseName);
        if (acompanyDiseases != null && !acompanyDiseases.isEmpty()) {
            return diseaseName + "可能伴随的并发症有：" +
                   acompanyDiseases.stream().map(Disease::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + diseaseName + "的并发症信息。";
    }

    /**
     * 查询疾病所属科室
     */
    private String queryDiseaseDepartments(String diseaseName) {
        Set<Department> departments = diseaseRepository.findDepartmentsByDiseaseName(diseaseName);
        if (departments != null && !departments.isEmpty()) {
            return diseaseName + "通常属于：" +
                   departments.stream().map(Department::getName).collect(Collectors.joining("、")) + "科。";
        }
        return "抱歉，未能找到" + diseaseName + "所属的科室信息。";
    }

    /**
     * 查询疾病推荐药品
     */
    private String queryDiseaseRecommandDrugs(String diseaseName) {
        Set<Drug> drugs = diseaseRepository.findRecommandDrugsByDiseaseName(diseaseName);
        if (drugs != null && !drugs.isEmpty()) {
            return diseaseName + "推荐的药品有：" +
                   drugs.stream().map(Drug::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + diseaseName + "的推荐药品信息。";
    }

    /**
     * 查询疾病常用药品
     */
    private String queryDiseaseCommonDrugs(String diseaseName) {
        Set<Drug> drugs = diseaseRepository.findCommonDrugsByDiseaseName(diseaseName);
        if (drugs != null && !drugs.isEmpty()) {
            return diseaseName + "常用的药品有：" +
                   drugs.stream().map(Drug::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + diseaseName + "的常用药品信息。";
    }

    /**
     * 查询疾病忌吃食物
     */
    private String queryDiseaseNoEatFoods(String diseaseName) {
        Set<Food> foods = diseaseRepository.findNoEatFoodsByDiseaseName(diseaseName);
        if (foods != null && !foods.isEmpty()) {
            return diseaseName + "忌吃的食物有：" +
                   foods.stream().map(Food::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + diseaseName + "忌吃的食物信息。";
    }

    /**
     * 查询疾病宜吃食物
     */
    private String queryDiseaseDoEatFoods(String diseaseName) {
        Set<Food> foods = diseaseRepository.findDoEatFoodsByDiseaseName(diseaseName);
        if (foods != null && !foods.isEmpty()) {
            return diseaseName + "宜吃的食物有：" +
                   foods.stream().map(Food::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + diseaseName + "宜吃的食物信息。";
    }

    /**
     * 查询疾病推荐食谱
     */
    private String queryDiseaseRecommandEatFoods(String diseaseName) {
        Set<Food> foods = diseaseRepository.findRecommandEatFoodsByDiseaseName(diseaseName);
        if (foods != null && !foods.isEmpty()) {
            return diseaseName + "推荐的食谱有：" +
                   foods.stream().map(Food::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + diseaseName + "的推荐食谱信息。";
    }

    /**
     * 查询疾病需要做的检查
     */
    private String queryDiseaseChecks(String diseaseName) {
        Set<Check> checks = diseaseRepository.findChecksByDiseaseName(diseaseName);
        if (checks != null && !checks.isEmpty()) {
            return diseaseName + "需要做的检查有：" +
                   checks.stream().map(Check::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + diseaseName + "的检查信息。";
    }

    /**
     * 查询药物的生产商
     */
    private String queryDrugProducer(String drugName) {
        return drugRepository.findProducerByDrugName(drugName)
                .map(producer -> drugName + "的生产厂家是：" + producer.getName() + "。")
                .orElse("抱歉，未能找到" + drugName + "的生产厂家信息。");
    }

    /**
     * 查询药物可以治疗哪些疾病 (common_drug 和 recommand_drug 的反向关系)
     */
    private String queryDrugEffectDiseases(String drugName) {
        Set<Disease> commonDiseases = drugRepository.findCommonForDiseasesByDrugName(drugName);
        Set<Disease> recommandDiseases = drugRepository.findRecommandForDiseasesByDrugName(drugName);

        Set<String> allDiseases = new HashSet<>();
        if (commonDiseases != null) {
            commonDiseases.stream().map(Disease::getName).forEach(allDiseases::add);
        }
        if (recommandDiseases != null) {
            recommandDiseases.stream().map(Disease::getName).forEach(allDiseases::add);
        }

        if (!allDiseases.isEmpty()) {
            return drugName + "常用于治疗/推荐用于治疗：" + String.join("、", allDiseases) + "等疾病。";
        }
        return "抱歉，未能找到" + drugName + "可以治疗的疾病信息。";
    }

    /**
     * 查询症状可能是什么病 (症状反向查疾病)
     */
    private String querySymptomDiseases(String symptomName) {
        Set<Disease> diseases = symptomRepository.findDiseasesWithThisSymptom(symptomName);
        if (diseases != null && !diseases.isEmpty()) {
            return symptomName + "可能是以下疾病的症状：" +
                   diseases.stream().map(Disease::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到以" + symptomName + "为症状的疾病信息。";
    }

    /**
     * 查询检查能诊断哪些疾病 (检查反向查疾病)
     */
    private String queryCheckDiagnoseDiseases(String checkName) {
        Set<Disease> diseases = checkRepository.findDiseasesRequiringCheck(checkName);
        if (diseases != null && !diseases.isEmpty()) {
            return checkName + "可以用于诊断：" +
                   diseases.stream().map(Disease::getName).collect(Collectors.joining("、")) + "等疾病。";
        }
        return "抱歉，未能找到" + checkName + "可以诊断的疾病信息。";
    }

    // 您可能需要添加一个 querySubDepartments 方法
    /*
    private String querySubDepartments(String departmentName) {
        Set<Department> subDepartments = departmentRepository.findSubDepartmentsByName(departmentName);
        if (subDepartments != null && !subDepartments.isEmpty()) {
            return departmentName + "下属的科室有：" +
                   subDepartments.stream().map(Department::getName).collect(Collectors.joining("、")) + "。";
        }
        return "抱歉，未能找到" + departmentName + "下属的科室信息。";
    }
    */

    /**
     * 获取与主要实体相关的 Neo4j 关系，用于生成推荐问题。
     * 这需要通过 Repository 实际查询图谱。
     * 返回的是关系类型、目标节点名称和目标节点类型。
     */
    private List<Neo4jRelationTuple> getGraphRelationsForEntity(String entityName, String entityType) {
        List<Neo4jRelationTuple> results = new ArrayList<>();
        // 这里需要针对不同类型的实体，编写不同的Cypher查询来获取相关关系。
        // 伪代码示例：
        if ("Disease".equals(entityType)) {
            // 查询疾病发出的所有关系
            diseaseRepository.findDiseaseRelationships(entityName).forEach(record -> {
                // record 应该包含 relationType, targetName, targetType
                // 您需要在 DiseaseRepository 中定义一个返回List<Map<String,Object>> 或自定义DTO的方法
                // 例如：MATCH (n:Disease {name: $name})-[r]-(m) RETURN type(r) as relationType, m.name as targetName, labels(m)[0] as targetType
                String relType = record.get("relationType", String.class);
                String targetName = record.get("targetName", String.class);
                String targetLabel = record.get("targetType", String.class); // 通常label的第一个就是主类型
                results.add(new Neo4jRelationTuple(relType, targetName, targetLabel));
            });
        } else if ("Drug".equals(entityType)) {
            // 查询药物相关的关系 (例如生产商，治疗的疾病)
             drugRepository.findDrugRelationships(entityName).forEach(record -> {
                String relType = record.get("relationType", String.class);
                String targetName = record.get("targetName", String.class);
                String targetLabel = record.get("targetType", String.class);
                results.add(new Neo4jRelationTuple(relType, targetName, targetLabel));
            });
        } else if ("Symptom".equals(entityType)) {
            // 查询症状相关的关系 (例如哪些疾病有此症状)
            symptomRepository.findSymptomRelationships(entityName).forEach(record -> {
                String relType = record.get("relationType", String.class);
                String targetName = record.get("targetName", String.class);
                String targetLabel = record.get("targetType", String.class);
                results.add(new Neo4jRelationTuple(relType, targetName, targetLabel));
            });
        }
        // ... 对其他实体类型添加类似逻辑 ...

        return results;
    }

    /**
     * 根据知识图谱检索到的关系和模板生成推荐问题。
     */
    private List<String> generateRelatedQuestionsFromGraph(String mainEntityName, List<Neo4jRelationTuple> graphRelations, int numQuestions) {
        Set<String> generatedQuestions = new LinkedHashSet<>(); // 使用LinkedHashSet保持顺序并去重

        for (Neo4jRelationTuple tuple : graphRelations) {
            String templateKey;
            // 修正关系类型到模板键的映射，使其与 `RELATION_TO_QUESTION_TEMPLATE` 的键匹配
            // 注意：这里需要根据您的实际Cypher查询返回的关系类型进行映射
            if ("has_symptom".equals(tuple.relationType)) {
                templateKey = "has_symptom"; // Disease -> Symptom
            } else if ("acompany_with".equals(tuple.relationType)) {
                templateKey = "acompany_with"; // Disease -> Disease
            } else if ("belongs_to".equals(tuple.relationType) && "Department".equals(tuple.targetType)) {
                templateKey = "belongs_to_department"; // Disease -> Department
            } else if ("recommand_drug".equals(tuple.relationType) && "Drug".equals(tuple.targetType)) {
                templateKey = "recommand_drug"; // Disease -> Drug
            } else if ("common_drug".equals(tuple.relationType) && "Drug".equals(tuple.targetType)) {
                templateKey = "common_drug"; // Disease -> Drug
            } else if ("no_eat".equals(tuple.relationType) && "Food".equals(tuple.targetType)) {
                templateKey = "no_eat"; // Disease -> Food
            } else if ("do_eat".equals(tuple.relationType) && "Food".equals(tuple.targetType)) {
                templateKey = "do_eat"; // Disease -> Food
            } else if ("recommand_eat".equals(tuple.relationType) && "Food".equals(tuple.targetType)) {
                templateKey = "recommand_eat"; // Disease -> Food
            } else if ("need_check".equals(tuple.relationType) && "Check".equals(tuple.targetType)) {
                templateKey = "need_check"; // Disease -> Check
            } else if ("produces".equals(tuple.relationType) && "Producer".equals(tuple.targetType)) {
                templateKey = "produces_drug"; // Drug <- Producer (关系方向需要反向思考)
            } else if ("common_drug".equals(tuple.relationType) && "Disease".equals(tuple.targetType)) {
                 templateKey = "drug_common_for"; // Drug <- common_drug <- Disease (反向问法)
            } else if ("recommand_drug".equals(tuple.relationType) && "Disease".equals(tuple.targetType)) {
                 templateKey = "drug_recommand_for"; // Drug <- recommand_drug <- Disease (反向问法)
            } else if ("has_symptom".equals(tuple.relationType) && "Disease".equals(tuple.targetType)) {
                 templateKey = "symptom_for_disease"; // Symptom <- has_symptom <- Disease
            } else if ("need_check".equals(tuple.relationType) && "Disease".equals(tuple.targetType)) {
                 templateKey = "check_for_disease"; // Check <- need_check <- Disease
            } else if ("belongs_to".equals(tuple.relationType) && "Department".equals(tuple.targetType)) {
                 templateKey = "sub_department"; // Department <- belongs_to <- Department (如果这个关系是 department to sub_department)
            }
            else {
                continue; // 不支持的关系类型
            }


            if (RELATION_TO_QUESTION_TEMPLATE.containsKey(templateKey)) {
                String questionTemplate = RELATION_TO_QUESTION_TEMPLATE.get(templateKey);
                String question = questionTemplate.replace("{entity_name}", mainEntityName);
                
                generatedQuestions.add(question);
                if (generatedQuestions.size() >= numQuestions) {
                    break;
                }
            }
        }
        return new ArrayList<>(generatedQuestions);
    }
}