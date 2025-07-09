# 文件路径: AC_AUTOMATON_SERVICE/utils/intent_classifier.py

class RuleBasedIntentClassifier:
    def __init__(self):
        # 定义意图关键词和模式
        # 关键词可以扩展，也可以使用正则表达式
        self.intent_rules = {
            "query_symptom": ["症状", "表现", "有什么症状", "什么表现"],
            "query_cure_way": ["治疗", "怎么办", "怎么治", "如何治疗", "用药", "吃什么药", "怎么医治", "方法"],
            "query_cause": ["原因", "引起", "导致", "为什么会得", "怎么来的"],
            "query_easy_get": ["人群", "易感", "哪些人", "谁会得", "哪些人容易得"],
            "query_cure_lasttime": ["多久", "时间", "多长", "持续多久"],
            "query_cured_prob": ["概率", "治愈率", "能治好吗", "痊愈可能性"],
            "query_desc": ["是什么", "什么是", "解释一下", "定义"],
            "query_prevent": ["预防", "避免", "怎么防止", "如何预防"],
            "query_recommand_drug": ["推荐药", "什么药好", "好药", "用药", "吃什么药"],
            "query_common_drug": ["常用药"],
            "query_no_eat": ["不能吃", "忌口", "禁忌", "不宜吃", "什么不能吃"],
            "query_do_eat": ["可以吃", "吃什么好", "宜吃", "吃什么对", "适合吃什么"],
            "query_recommand_eat": ["食谱", "推荐吃", "膳食"],
            "query_need_check": ["检查", "做哪些检查", "需要检查吗", "怎么检查"],
            "query_belongs_to_department": ["科室", "看哪个", "挂什么号", "去哪个科", "哪个科室"],
            "query_symptom_disease": ["可能是", "什么病", "得了什么", "会是什么病", "诊断为"], # 反向：症状查病
            "query_drug_producer": ["厂家", "生产商", "哪个厂", "生产"],
            "query_acompany_disease": ["并发症", "伴随", "引起什么病", "还有什么病", "同时得"],
            "query_drug_side_effect": ["副作用", "不良反应", "危害", "禁忌"],
            "query_drug_effect_disease": ["治疗哪些病", "治什么病", "有什么疗效"], # 药物治病
            "query_check_diagnose_disease": ["诊断什么病", "查出什么病", "能查出什么", "确诊"], # 检查诊断病
            "query_department_subdepartment": ["下属科室", "有哪些科室", "分科"], # 科室下属科室
            "greeting": ["你好", "您好", "hello", "在吗", "hi", "喂"] # 通用问候
        }
        # 意图优先级 (如果一个问题匹配多个规则，优先匹配靠前的)
        # 越具体的意图，优先级越高。问候语最高。
        self.intent_priority = [
            "greeting", # 最高优先级
            "query_drug_producer", "query_drug_side_effect", "query_drug_effect_disease",
            "query_check_diagnose_disease", "query_symptom_disease",
            "query_recommand_drug", "query_common_drug", "query_no_eat", "query_do_eat", "query_recommand_eat",
            "query_need_check", "query_belongs_to_department", "query_acompany_disease",
            "query_cure_way", "query_cause", "query_prevent", "query_easy_get",
            "query_cure_lasttime", "query_cured_prob", "query_symptom", "query_desc",
            "query_department_subdepartment" # 最低优先级
        ]


    def classify_intent(self, text, extracted_entities):
        """
        根据文本和提取的实体进行意图分类。
        采用基于关键词的策略，并结合主要实体类型做辅助判断。
        """
        text_lower = text.lower() # 转小写方便匹配

        # 1. 优先处理通用问候，不依赖实体
        if "greeting" in self.intent_rules and any(k in text_lower for k in self.intent_rules["greeting"]):
            return "greeting"

        # 2. 如果没有识别到实体，并且不是问候语，则很难明确意图
        if not extracted_entities:
            return "unknown_intent"

        # 3. 尝试匹配意图规则
        # 获取主要实体类型（如果有的话，用于辅助判断）
        # 这里可以根据您的业务逻辑，选择一个最相关的实体作为“主要实体”
        # 例如，疾病通常是主要实体，其次是症状、药物等
        main_entity_type = None
        for e in extracted_entities:
            if e['type'] == 'Disease':
                main_entity_type = 'Disease'
                break
            elif e['type'] == 'Symptom' and main_entity_type is None:
                main_entity_type = 'Symptom'
            elif e['type'] == 'Drug' and main_entity_type is None:
                main_entity_type = 'Drug'
            elif e['type'] == 'Check' and main_entity_type is None:
                main_entity_type = 'Check'
            elif e['type'] == 'Department' and main_entity_type is None:
                main_entity_type = 'Department'
            # 可以添加更多优先级，例如 Food, Producer 如果它们能作为主要查询目标

        for intent_type in self.intent_priority:
            if intent_type == "greeting": # 问候语已在前面处理
                continue

            for keyword in self.intent_rules[intent_type]:
                if keyword in text_lower:
                    # 对于需要特定实体类型的意图，增加判断准确性
                    # 仅当文本中包含关键词，且主要实体类型符合预期时，才返回该意图
                    if intent_type in ["query_symptom", "query_cure_way", "query_cause", "query_easy_get",
                                        "query_cure_lasttime", "query_cured_prob", "query_desc", "query_prevent",
                                        "query_acompany_disease", "query_no_eat", "query_do_eat",
                                        "query_recommand_eat", "query_need_check", "query_belongs_to_department"]:
                        if main_entity_type == 'Disease': # 这些意图通常需要围绕疾病
                            return intent_type
                        # 某些也可以由症状引出对疾病的查询，例如“头疼怎么办” -> (Symptom) -> 治疗方式
                        # 这里可以更精细化判断，例如：如果只有症状实体且匹配到“治疗/怎么办”，也可归为query_cure_way
                        if main_entity_type == 'Symptom' and intent_type == 'query_cure_way':
                            return intent_type

                    elif intent_type in ["query_recommand_drug", "query_common_drug", "query_drug_side_effect", "query_drug_effect_disease"]:
                        if main_entity_type == 'Drug' or main_entity_type == 'Disease': # 药物意图可以由药物或疾病引出
                            return intent_type
                    
                    elif intent_type == "query_drug_producer":
                        if main_entity_type == 'Drug' or main_entity_type == 'Producer': # 生产商意图由药物或生产商引出
                            return intent_type

                    elif intent_type == "query_symptom_disease":
                        if main_entity_type == 'Symptom': # 症状查病，需要 Symptom 实体
                            return intent_type
                    
                    elif intent_type == "query_check_diagnose_disease":
                        if main_entity_type == 'Check': # 检查诊断病，需要 Check 实体
                            return intent_type
                    
                    elif intent_type == "query_department_subdepartment":
                        if main_entity_type == 'Department': # 科室下属科室，需要 Department 实体
                            return intent_type

                    # 如果没有特定实体类型要求，或者上述条件不满足，但关键词匹配，可以作为通用 fallback
                    # 例如，如果问“什么是高血压”，虽然是query_desc，但没有main_entity_type的严格限制，也可以返回
                    # 为了更精确，这里我们尽量保持严格的实体-意图匹配
                    # 如果需要更宽松的匹配，可以移除或修改上述 if 条件
                    # return intent_type # 这是一个更宽松的匹配，可能导致误判
        
        # 如果有实体，但没有匹配到特定意图规则
        if extracted_entities:
            return "unknown_intent"
            
        return "unknown_intent" # 如果没有匹配到任何意图，且没有识别到实体 (除了问候语)

# 仅用于本地测试或调试
if __name__ == '__main__':
    classifier = RuleBasedIntentClassifier()
    
    # 模拟提取到的实体 (这些实体应该由 entity_extractor 提供)
    def create_mock_entity(text, type):
        return {'text': text, 'type': type, 'start': 0, 'end': len(text)-1}

    print(f"Text: '高血压有什么症状？'")
    entities = [create_mock_entity('高血压', 'Disease')]
    print(f"  Extracted Entities: {entities} -> Intent: {classifier.classify_intent('高血压有什么症状？', entities)}")

    print(f"Text: '胃痛怎么办？'")
    entities = [create_mock_entity('胃痛', 'Symptom')]
    print(f"  Extracted Entities: {entities} -> Intent: {classifier.classify_intent('胃痛怎么办？', entities)}")

    print(f"Text: '阿莫西林可以治什么病？'")
    entities = [create_mock_entity('阿莫西林', 'Drug')]
    print(f"  Extracted Entities: {entities} -> Intent: {classifier.classify_intent('阿莫西林可以治什么病？', entities)}")

    print(f"Text: '你好'")
    entities = [] # 问候语通常不包含特定医疗实体
    print(f"  Extracted Entities: {entities} -> Intent: {classifier.classify_intent('你好', entities)}")

    print(f"Text: '我感觉不舒服'")
    entities = []
    print(f"  Extracted Entities: {entities} -> Intent: {classifier.classify_intent('我感觉不舒服', entities)}")

    print(f"Text: '肺炎会引起哪些并发症？'")
    entities = [create_mock_entity('肺炎', 'Disease')]
    print(f"  Extracted Entities: {entities} -> Intent: {classifier.classify_intent('肺炎会引起哪些并发症？', entities)}")

    print(f"Text: '华北制药生产什么药？'")
    entities = [create_mock_entity('华北制药', 'Producer')] # 这里的 Producer 实体暂时没有对应的意图分类逻辑，会返回 unknown_intent
    print(f"  Extracted Entities: {entities} -> Intent: {classifier.classify_intent('华北制药生产什么药？', entities)}")

    print(f"Text: '内科下面有哪些科室？'")
    entities = [create_mock_entity('内科', 'Department')]
    print(f"  Extracted Entities: {entities} -> Intent: {classifier.classify_intent('内科下面有哪些科室？', entities)}")