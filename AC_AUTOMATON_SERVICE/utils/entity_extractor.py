# 文件路径: AC_AUTOMATON_SERVICE/utils/entity_extractor.py

import ahocorasick
import json
import os

class EntityExtractor:
    def __init__(self, entity_data_path=None):
        """
        初始化实体提取器，从JSONL文件加载实体并构建AC自动机。
        Args:
            entity_data_path (str): 包含所有实体数据的JSONL文件路径。
                                   每行应是一个JSON对象，包含 'name' 和 'type' 字段。
        """
        self.A = ahocorasick.Automaton()
        self.entity_map = {} # 用于存储实体名 -> 实体类型 的映射，方便快速查找

        if entity_data_path:
            # 检查文件是否存在
            if not os.path.exists(entity_data_path):
                raise FileNotFoundError(f"Entity data file not found: {entity_data_path}. "
                                        "Please run 'scripts/extract_java_entities.py' first.")
            self._load_entities_from_jsonl(entity_data_path)
        else:
            raise ValueError("Must provide entity_data_path to initialize EntityExtractor.")

        # 检查是否成功加载了模式
        if len(self.A) == 0:
            raise ValueError("No entities loaded into AC Automaton. "
                             "Please check your entity data file and extraction script.")
            
        self.A.make_automaton() # 构建AC自动机
        print(f"AC Automaton built with {len(self.A)} unique entities/patterns.")

    def _load_entities_from_jsonl(self, data_path):
        """从JSONL文件中加载所有实体，并添加到AC自动机。"""
        with open(data_path, 'r', encoding='utf-8') as f:
            for line_num, line in enumerate(f, 1):
                try:
                    entity_info = json.loads(line.strip())
                    entity_name = entity_info.get('name')
                    entity_type = entity_info.get('type')
                    if entity_name and entity_type:
                        # ahocorasick的add_word方法会自动处理重复的词
                        # 这里的value是(entity_name, entity_type)，用于匹配时返回
                        self.A.add_word(entity_name, (entity_name, entity_type))
                        self.entity_map[entity_name] = entity_type # 维护一个map，方便调试
                    else:
                        print(f"Warning: Skipping malformed entity in line {line_num}: {line.strip()} (Missing 'name' or 'type').")
                except json.JSONDecodeError:
                    print(f"Warning: Skipping malformed JSON line {line_num} in entity data: {line.strip()}")
                except Exception as e:
                    print(f"Error processing line {line_num}: {line.strip()} - {e}")


    def extract_entities(self, text):
        """
        从文本中提取所有匹配的实体及其类型。
        返回一个列表，每个元素是字典 { 'text': '匹配到的实体文本', 'type': '实体类型', 'start': int, 'end': int }
        """
        found_entities = []
        # iter() 方法返回 (end_index, value)
        for end_index, (original_entity_name, entity_type) in self.A.iter(text):
            start_index = end_index - len(original_entity_name) + 1
            found_entities.append({
                'text': original_entity_name, # 实际匹配到的文本
                'type': entity_type,
                'start': start_index,
                'end': end_index
            })

        # 处理重叠实体：优先选择更长的匹配。
        # 例如，如果匹配到“慢性肺炎”和“肺炎”，只保留“慢性肺炎”。
        # 策略：按长度降序排序，然后检查是否被已选择的、更长的实体完全包含。
        unique_entities = []
        # 先按长度降序排序，以便处理重叠时优先考虑长词
        found_entities_sorted_by_len = sorted(found_entities, key=lambda x: len(x['text']), reverse=True)
        
        for entity in found_entities_sorted_by_len:
            is_overlapped_and_shorter = False
            for existing_entity in unique_entities:
                # 检查当前实体是否被已选择的、更长的实体完全包含
                if (existing_entity['start'] <= entity['start'] and existing_entity['end'] >= entity['end']):
                    is_overlapped_and_shorter = True
                    break
            if not is_overlapped_and_shorter:
                unique_entities.append(entity)
        
        # 最终按start_index排序，保持在文本中的原始顺序，方便理解和后续处理
        return sorted(unique_entities, key=lambda x: x['start'])

# 仅用于本地测试或调试
if __name__ == '__main__':
    # 模拟数据文件路径 (用于本地测试，确保先运行 scripts/extract_java_entities.py)
    current_script_dir = os.path.dirname(os.path.abspath(__file__))
    ENTITY_DATA_PATH = os.path.join(current_script_dir, '..', 'data', 'all_graph_entities.jsonl')
    
    # 为了演示，如果文件不存在，则使用一个硬编码的mock数据
    mock_entities_for_test_if_no_file = {
        "Disease": ["高血压", "糖尿病", "感冒", "肺结核", "偏头痛", "胃炎", "肝炎", "心脏病", "白血病", "甲状腺炎", "支气管炎", "肿瘤", "流感", "肺炎", "慢性肺炎"],
        "Symptom": ["头晕", "咳嗽", "发烧", "胃痛", "恶心", "乏力", "食欲不振", "剧烈头痛", "流鼻涕"],
        "Drug": ["阿莫西林", "布洛芬", "奥美拉唑", "降压药", "抗生素", "头孢", "阿司匹林", "止痛药", "感冒药"],
        "Check": ["胃镜检查", "心电图", "血常规", "CT", "核磁共振", "X光"],
        "Department": ["内科", "外科", "儿科", "呼吸科", "心内科", "消化科", "神经内科", "肿瘤科", "中医科", "心血管内科"],
        "Food": ["辣椒", "肥肉", "米饭", "水果", "蔬菜", "牛奶", "鸡蛋"],
        "Producer": ["华北制药", "哈药集团", "拜耳"]
    }

    try:
        extractor = EntityExtractor(entity_data_path=ENTITY_DATA_PATH)
    except FileNotFoundError:
        print("Warning: 'all_graph_entities.jsonl' not found. Using hardcoded mock entities for testing.")
        # 如果文件不存在，则通过字典直接构建自动机 (这种方式需要修改 EntityExtractor 的 __init__ )
        # 这里为了简化，我们直接从mock字典构建，但实际应用应始终从文件加载
        extractor = EntityExtractor(entity_data_path=None) # 临时设置None以规避文件检查
        # 模拟 EntityExtractor 内部构建逻辑
        for entity_type, names in mock_entities_for_test_if_no_file.items():
            for name in names:
                extractor.A.add_word(name, (name, entity_type))
                extractor.entity_map[name] = entity_type
        extractor.A.make_automaton()
        print(f"AC Automaton built with {len(extractor.A)} mock entities.")
    except ValueError as e:
        print(f"Error during EntityExtractor initialization: {e}")
        sys.exit(1)


    test_texts = [
        "高血压患者不能吃什么？",
        "我最近有点胃痛，需要做胃镜检查吗？",
        "请问阿莫西林的生产厂家是哪个？",
        "咳嗽伴有发烧可能是得了什么病？",
        "关于糖尿病的治疗方法有什么？",
        "我有慢性肺炎，咳嗽的很厉害。",
        "你好，我感冒了，吃什么药好？",
        "去心血管内科看什么病？"
    ]

    for text in test_texts:
        entities = extractor.extract_entities(text)
        print(f"Text: '{text}'")
        print(f"  Extracted Entities: {entities}\n")