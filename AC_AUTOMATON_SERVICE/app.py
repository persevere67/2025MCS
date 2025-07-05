from flask import Flask, request, jsonify
import os
import sys
from neo4j import GraphDatabase # 导入 Neo4j 驱动

# ====================================================================
# *** 关键路径调整：将 `AC_AUTOMATON_SERVICE` 目录本身添加到 Python 模块搜索路径。***
# 这使得可以直接导入 `utils.entity_extractor` 和 `utils.intent_classifier`。
# ====================================================================
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(current_dir)

# 从 utils 文件夹导入模块
from utils.entity_extractor import EntityExtractor
from utils.intent_classifier import RuleBasedIntentClassifier

app = Flask(__name__)

# ====================================================================
# *** Neo4j 数据库配置 ***
# 根据您提供的 spring.neo4j.uri, username, password
# ====================================================================
NEO4J_URI = "bolt://10.242.17.41:7687"
NEO4J_USERNAME = "neo4j"
NEO4J_PASSWORD = "12345678"

# 初始化 Neo4j 驱动
try:
    neo4j_driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USERNAME, NEO4J_PASSWORD))
    neo4j_driver.verify_connectivity() # 验证连接
    print("Successfully connected to Neo4j database.")
except Exception as e:
    print(f"ERROR: Could not connect to Neo4j database: {e}")
    sys.exit(1) # 如果无法连接数据库，则退出

# ====================================================================
# *** 实体数据文件路径 ***
# ====================================================================
ENTITY_DATA_PATH = os.path.join(current_dir, 'data', 'all_graph_entities.jsonl')

# 初始化实体提取器和意图分类器
try:
    # 尝试初始化 EntityExtractor。如果 all_graph_entities.jsonl 不存在，会抛出 FileNotFoundError
    entity_extractor = EntityExtractor(entity_data_path=ENTITY_DATA_PATH)
except FileNotFoundError as e:
    print(f"ERROR: Entity data file not found: {e}")
    print("Please run 'python scripts/extract_entities_from_neo4j.py' to generate 'all_graph_entities.jsonl'.")
    sys.exit(1) # 如果实体数据文件不存在，则直接退出
except ValueError as e:
    print(f"ERROR during EntityExtractor initialization: {e}")
    sys.exit(1)

intent_classifier = RuleBasedIntentClassifier()

# ====================================================================
# *** 核心逻辑：根据知识图谱生成推荐问题 ***
# ====================================================================
def generate_recommended_questions(query_text, extracted_entities, predicted_intent):
    """
    根据用户查询、提取的实体和预测的意图，从Neo4j知识图谱中生成推荐问题。
    返回一个包含 SuggestedMedicalQuestionDto 结构字典的列表。
    """
    recommended_questions = []
    
    # 优先从 extracted_entities 中找到一个主要实体用于图谱查询
    main_entity_name = None
    main_entity_type = None
    
    # 按照优先级查找主要实体
    # 疾病 > 症状 > 药品 > 检查 > 科室 > 食物 > 生产商
    priority_order = ['Disease', 'Symptom', 'Drug', 'Check', 'Department', 'Food', 'Producer']
    for p_type in priority_order:
        for entity in extracted_entities:
            if entity['type'] == p_type:
                main_entity_name = entity['text']
                main_entity_type = entity['type']
                break
        if main_entity_name:
            break

    # 如果没有识别到主要实体，或者意图是问候语，则返回通用或空推荐
    if not main_entity_name or predicted_intent == "greeting":
        if predicted_intent == "unknown_intent":
             recommended_questions.append({
                "questionText": "您可以问我关于哪些疾病的信息？",
                "questionId": "general_q_1",
                "relevantEntities": [],
                "relevanceScore": 0.7
             })
             recommended_questions.append({
                "questionText": "我能查询哪些症状的信息？",
                "questionId": "general_q_2",
                "relevantEntities": [],
                "relevanceScore": 0.65
             })
             recommended_questions.append({
                "questionText": "有哪些常见的药物？",
                "questionId": "general_q_3",
                "relevantEntities": [],
                "relevanceScore": 0.6
             })
        return recommended_questions


    # 动态构建相关实体（用于 RecommendedQuestionDto.relevantEntities）
    # 确保返回的实体结构符合 MedicalKnowledgeGraphEntityDto
    # 这里只填充 id, name, type，如果需要 diseaseDetails，需要在后续查询中获取
    relevant_entities_for_recommendation = [
        {"id": f"kg_id_{main_entity_type}_{main_entity_name}", "name": main_entity_name, "type": main_entity_type}
    ]

    # Neo4j 查询会话
    with neo4j_driver.session() as session:
        # 基于意图和主要实体来生成推荐问题
        # 以下是几个根据您知识图谱关系定义的推荐逻辑示例
        
        # 1. 查询疾病相关问题 (最常见)
        if main_entity_type == 'Disease':
            # 症状
            if predicted_intent != 'query_symptom': # 如果用户还没问症状，推荐问症状
                query = f"MATCH (d:Disease)-[:has_symptom]->(s:Symptom) WHERE d.name = '{main_entity_name}' RETURN s.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}的常见症状有哪些？",
                        "questionId": f"{main_entity_name}_symptoms",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.9
                    })
            
            # 治疗方法
            if predicted_intent != 'query_cure_way':
                query = f"MATCH (d:Disease) WHERE d.name = '{main_entity_name}' RETURN d.cure_way LIMIT 1"
                result = session.run(query).single()
                # *** 关键修改：使用 .get() 方法安全地访问属性 ***
                cure_way_data = result.get('cure_way') if result else None 
                if cure_way_data: # result.get('cure_way') 返回 None 或实际值
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}有哪些治疗方法？",
                        "questionId": f"{main_entity_name}_cure_way",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.88
                    })
            
            # 推荐药品
            if predicted_intent not in ['query_recommand_drug', 'query_common_drug']:
                query = f"MATCH (d:Disease)-[:recommand_drug|common_drug]->(dr:Drug) WHERE d.name = '{main_entity_name}' RETURN dr.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}推荐/常用什么药？",
                        "questionId": f"{main_entity_name}_drugs",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.85
                    })

            # 并发症
            if predicted_intent != 'query_acompany_disease':
                query = f"MATCH (d:Disease)-[:acompany_with]->(a:Disease) WHERE d.name = '{main_entity_name}' RETURN a.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}会引起哪些并发症？",
                        "questionId": f"{main_entity_name}_complications",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.82
                    })
            
            # 所属科室
            if predicted_intent != 'query_belongs_to_department':
                query = f"MATCH (d:Disease)-[:belongs_to]->(dep:Department) WHERE d.name = '{main_entity_name}' RETURN dep.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}应该挂什么科？",
                        "questionId": f"{main_entity_name}_department",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.8
                    })
            
            # 宜忌食物 (do_eat / no_eat)
            if predicted_intent not in ['query_do_eat', 'query_no_eat', 'query_recommand_eat']:
                query = f"MATCH (d:Disease)-[:do_eat|no_eat|recommand_eat]->(f:Food) WHERE d.name = '{main_entity_name}' RETURN f.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}患者饮食上有什么禁忌或推荐？",
                        "questionId": f"{main_entity_name}_food",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.78
                    })

            # 检查
            if predicted_intent != 'query_need_check':
                query = f"MATCH (d:Disease)-[:need_check]->(c:Check) WHERE d.name = '{main_entity_name}' RETURN c.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"诊断{main_entity_name}需要做哪些检查？",
                        "questionId": f"{main_entity_name}_checks",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.75
                    })

            # 预防方法
            if predicted_intent != 'query_prevent':
                query = f"MATCH (d:Disease) WHERE d.name = '{main_entity_name}' RETURN d.prevent LIMIT 1"
                result = session.run(query).single()
                # *** 关键修改：使用 .get() 方法安全地访问属性 ***
                prevent_data = result.get('prevent') if result else None
                if prevent_data:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}如何预防？",
                        "questionId": f"{main_entity_name}_prevent",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.72
                    })

            # 病因
            if predicted_intent != 'query_cause':
                query = f"MATCH (d:Disease) WHERE d.name = '{main_entity_name}' RETURN d.cause LIMIT 1"
                result = session.run(query).single()
                # *** 关键修改：使用 .get() 方法安全地访问属性 ***
                cause_data = result.get('cause') if result else None
                if cause_data:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}的病因是什么？",
                        "questionId": f"{main_entity_name}_cause",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.7
                    })

        # 2. 查询症状相关问题 (症状->疾病)
        elif main_entity_type == 'Symptom':
            if predicted_intent != 'query_symptom_disease':
                query = f"MATCH (s:Symptom)<-[:has_symptom]-(d:Disease) WHERE s.name = '{main_entity_name}' RETURN d.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}可能是哪些疾病的症状？",
                        "questionId": f"{main_entity_name}_related_diseases",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.9
                    })
            # 症状通常也和治疗方法相关
            if predicted_intent != 'query_cure_way':
                 recommended_questions.append({
                    "questionText": f"{main_entity_name}应该如何缓解或治疗？",
                    "questionId": f"{main_entity_name}_alleviate",
                    "relevantEntities": relevant_entities_for_recommendation,
                    "relevanceScore": 0.85
                })

        # 3. 查询药品相关问题 (药品->疾病, 药品->生产商)
        elif main_entity_type == 'Drug':
            if predicted_intent not in ['query_drug_effect_disease', 'query_recommand_drug', 'query_common_drug']:
                query = f"MATCH (d:Disease)-[:recommand_drug|common_drug]->(dr:Drug) WHERE dr.name = '{main_entity_name}' RETURN d.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}主要治疗哪些疾病？",
                        "questionId": f"{main_entity_name}_treat_diseases",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.9
                    })
            if predicted_intent != 'query_drug_producer':
                query = f"MATCH (dr:Drug)<-[:produces]-(p:Producer) WHERE dr.name = '{main_entity_name}' RETURN p.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}是哪个药厂生产的？",
                        "questionId": f"{main_entity_name}_producer",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.8
                    })
            
        # 4. 查询检查相关问题 (检查->疾病)
        elif main_entity_type == 'Check':
            if predicted_intent != 'query_check_diagnose_disease':
                query = f"MATCH (c:Check)<-[:need_check]-(d:Disease) WHERE c.name = '{main_entity_name}' RETURN d.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}可以诊断哪些疾病？",
                        "questionId": f"{main_entity_name}_diagnose_diseases",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.85
                    })

        # 5. 查询科室相关问题 (科室->疾病, 科室->子科室)
        elif main_entity_type == 'Department':
            if predicted_intent != 'query_department_subdepartment':
                query = f"MATCH (d:Department)-[:belongs_to]->(sub:Department) WHERE d.name = '{main_entity_name}' RETURN sub.name LIMIT 1"
                result = session.run(query).single()
                if result:
                    recommended_questions.append({
                        "questionText": f"{main_entity_name}下面包含哪些细分科室？",
                        "questionId": f"{main_entity_name}_subdepartments",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.8
                    })
            if predicted_intent != 'query_belongs_to_department':
                 query = f"MATCH (d:Department)<-[:belongs_to]-(dis:Disease) WHERE d.name = '{main_entity_name}' RETURN dis.name LIMIT 1"
                 result = session.run(query).single()
                 if result:
                     recommended_questions.append({
                         "questionText": f"哪些疾病属于{main_entity_name}？",
                         "questionId": f"{main_entity_name}_diseases_in_dept",
                         "relevantEntities": relevant_entities_for_recommendation,
                         "relevanceScore": 0.75
                     })


        # 确保至少返回三个，不足时补充通用问题
        while len(recommended_questions) < 3:
            # 补充基于主要实体的通用问题
            if main_entity_type == 'Disease':
                if len(recommended_questions) == 0: # 如果前面一个都没加进去，至少保证有一个
                     recommended_questions.append({
                        "questionText": f"关于{main_entity_name}，您还想了解什么？",
                        "questionId": "general_disease_q",
                        "relevantEntities": relevant_entities_for_recommendation,
                        "relevanceScore": 0.6
                     })
                else: # 否则就加一些通用的
                     recommended_questions.append({
                        "questionText": "还有其他关于医药方面的问题吗？",
                        "questionId": "general_q_more",
                        "relevantEntities": [],
                        "relevanceScore": 0.5
                     })
            elif len(recommended_questions) == 0 and main_entity_name: # 其他实体类型也至少有一个
                 recommended_questions.append({
                    "questionText": f"关于{main_entity_name}，您想了解哪些信息？",
                    "questionId": "general_entity_q",
                    "relevantEntities": relevant_entities_for_recommendation,
                    "relevanceScore": 0.6
                 })
            else: # 如果实在没有实体，也没有别的推荐，就加最通用的
                recommended_questions.append({
                    "questionText": "我能为您提供哪些帮助？",
                    "questionId": "final_general_q",
                    "relevantEntities": [],
                    "relevanceScore": 0.4
                })
            # 避免无限循环，如果问题已经很多了，或者根本加不进去了
            if len(recommended_questions) > 5: # 设定一个上限
                break

    # 限制返回最多3个推荐问题，并可以根据relevanceScore排序（如果需要）
    recommended_questions = sorted(recommended_questions, key=lambda x: x.get('relevanceScore', 0), reverse=True)[:3]
    
    return recommended_questions


@app.route('/process_query', methods=['POST'])
def process_query():
    data = request.get_json()
    query_text = data.get('queryText') # 与 Java DTO 的 queryText 字段匹配
    # sessionId = data.get('sessionId') # 如果需要，可以在这里获取
    # userId = data.get('userId') # 如果需要，可以在这里获取

    if not query_text:
        return jsonify({"apiResponse": {"code": 400, "message": "Missing 'queryText' in request body"}}), 400

    print(f"Received query: '{query_text}'")

    # 调用实体提取器
    extracted_entities_raw = entity_extractor.extract_entities(query_text)
    
    # 转换为 identifiedKeywords (List<String>)
    # extracted_entities_raw 是 [{'text': '高血压', 'type': 'Disease', ...}, ...]
    # identifiedKeywords 只是 ['高血压', ...]
    identified_keywords = [e['text'] for e in extracted_entities_raw]

    # 调用意图分类器
    predicted_intent = intent_classifier.classify_intent(query_text, extracted_entities_raw)
    print(f"  Extracted Entities: {extracted_entities_raw}") # 打印详细信息方便调试
    print(f"  Predicted Intent: {predicted_intent}")

    # 调用生成推荐问题的逻辑
    recommended_questions = generate_recommended_questions(query_text, extracted_entities_raw, predicted_intent)
    print(f"  Recommended Questions: {recommended_questions}")

    # 构建最终响应，匹配 MedicalRecommendationResponse Java DTO
    response_data = {
        "originalQuery": query_text,
        "identifiedKeywords": identified_keywords,
        "identifiedIntent": predicted_intent,
        "recommendedQuestions": recommended_questions,
        "apiResponse": { # 模拟 ApiResponse
            "code": 200,
            "message": "Success"
        }
    }
    return jsonify(response_data)

if __name__ == '__main__':
    # 运行Flask应用
    # host='0.0.0.0' 允许从任何网络接口访问 (例如，如果Java在另一台机器上)
    # port=5000 是服务监听的端口
    # debug=True 仅用于开发，生产环境请设置为False或使用Gunicorn/uWSGI
    print("Starting Flask AC_AUTOMATON_SERVICE...")
    app.run(host='0.0.0.0', port=5000, debug=True)