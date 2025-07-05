# generate_from_new_data_final_fix.py (The ultimate parser version)

import json
import random
from tqdm import tqdm
import os

# --- 配置区 ---
CONFIG = {
    "input_json_path": "medical.json",
    "output_json_path": "training_qa_pairs_final.json"
}


# --- 全新的、可以自动修复格式的JSON加载函数 ---
def load_and_fix_json_stream(filepath):
    """
    一个终极健壮的函数，用于加载由逗号分隔的、非标准JSON对象序列文件。
    它会通过在内存中添加[]来自动修复格式。
    """
    print("使用终极解析器加载并自动修复JSON文件...")
    try:
        with open(filepath, 'r', encoding='utf-8-sig') as f:
            # 读取整个文件内容到一个字符串缓冲区
            buffer = f.read()

        # 移除首尾可能存在的空白字符和末尾可能存在的逗号
        buffer = buffer.strip()
        if buffer.endswith(','):
            buffer = buffer[:-1]

        # 【核心修复】自动在首尾添加方括号，将其变成一个合法的JSON数组字符串
        fixed_buffer = f"[{buffer}]"

        # 解析这个修复后的、完整的JSON数组字符串
        data = json.loads(fixed_buffer)

        print(f"自动修复并解析成功！共加载 {len(data)} 个JSON对象。")
        return data

    except FileNotFoundError:
        print(f"错误: 找不到文件 {filepath}。")
        return []
    except Exception as e:
        print(f"加载或修复文件时发生严重错误: {e}")
        return []


def process_medical_data(all_data):
    """
    将加载好的数据对象列表，转换为问答对。
    """
    qa_pairs = []
    field_to_question_templates = {
        "desc": ["{}是什么？", "介绍一下{}。"],
        "prevent": ["如何预防{}？", "{}的预防措施有哪些？"],
        "cause": ["{}的病因是什么？", "什么原因会导致{}？"],
        "symptom": ["{}有什么症状？", "{}的临床表现是什么？"],
        "cure_department": ["得了{}应该去哪个科室？"],
        "cure_way": ["{}有哪些治疗方法？"],
        "cure_lasttime": ["治疗{}大概需要多久？"],
        "cured_prob": ["{}的治愈率高吗？"],
        "common_drug": ["治疗{}常用哪些药？"],
        "do_eat": ["得了{}吃什么对身体好？"],
        "not_eat": ["得了{}不应该吃什么？"],
        "recommand_eat": ["有没有适合{}患者的推荐食谱？"]
    }

    print("开始从JSON对象生成问答对...")
    for data in tqdm(all_data, desc="处理医疗条目"):
        if not isinstance(data, dict): continue
        entity_name = data.get("name")
        if not entity_name: continue

        for field, templates in field_to_question_templates.items():
            content = data.get(field)
            if not content: continue

            answer_text = ""
            if isinstance(content, list) and len(content) > 0:
                str_content = [str(item) for item in content]
                answer_text = f"{entity_name}的{field}包括{'、'.join(str_content)}。"
            elif isinstance(content, str) and len(content) > 2:
                answer_text = content
            else:
                continue

            for question in templates:
                qa_pairs.append([question.format(entity_name), answer_text])

    print(f"从数据集中共生成了 {len(qa_pairs)} 个问答对。")
    return qa_pairs


if __name__ == "__main__":
    print("开始从 medical.json 生成训练数据...")

    # 使用全新的、带自动修复功能的加载函数
    medical_objects = load_and_fix_json_stream(CONFIG["input_json_path"])

    if medical_objects:
        qa_pairs = process_medical_data(medical_objects)

        if qa_pairs:
            random.shuffle(qa_pairs)
            print(f"\n总共生成 {len(qa_pairs)} 个问答对。")

            try:
                with open(CONFIG["output_json_path"], 'w', encoding='utf-8') as f:
                    json.dump(qa_pairs, f, ensure_ascii=False, indent=4)
                print(f"成功！新的训练数据集已保存到: {CONFIG['output_json_path']}")
            except Exception as e:
                print(f"保存文件时出错: {e}")
        else:
            print("未能从加载的数据中生成任何问答对。")
    else:
        print("未能从文件中加载任何JSON对象，请检查文件内容。")