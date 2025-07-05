import json
import os
from neo4j import GraphDatabase
import sys

# ====================================================================
# *** Neo4j 数据库配置 ***
# ====================================================================
NEO4J_URI = "bolt://10.242.17.41:7687"
NEO4J_USERNAME = "neo4j"
NEO4J_PASSWORD = "12345678"

# ====================================================================
# *** 输出文件路径 ***
# ====================================================================
OUTPUT_JSONL = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                            '..', 'data', 'all_graph_entities.jsonl')

def extract_entities_from_neo4j():
    """
    连接Neo4j数据库，查询所有节点名称和类型，并保存到JSONL文件。
    """
    all_entities = []
    
    try:
        driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USERNAME, NEO4J_PASSWORD))
        driver.verify_connectivity()
        print("Successfully connected to Neo4j database.")

        with driver.session() as session:
            print("Querying all entity names and types from Neo4j...")
            
            labels_result = session.run("CALL db.labels()").data()
            all_labels = [record['label'] for record in labels_result]

            for label in all_labels:
                # 排除系统标签或非实体标签，例如Neo4j 5.x可能有一些_Label等内部标签
                if label.startswith('__') or label.startswith('Constraint') or label in ['GraphSchema']: 
                    continue # 根据实际情况调整需要排除的标签

                print(f"  Fetching entities for type: {label}...")
                # *** 关键修改：将 EXISTS(n.name) 改为 n.name IS NOT NULL ***
                query = f"MATCH (n:`{label}`) WHERE n.name IS NOT NULL RETURN n.name AS name, '{label}' AS type"
                result = session.run(query)
                for record in result:
                    entity_name = record['name']
                    entity_type = record['type']
                    if entity_name and entity_type:
                        all_entities.append({"name": entity_name, "type": entity_type})
        
        driver.close()
        print("Neo4j connection closed.")

    except Exception as e:
        print(f"ERROR: Failed to extract entities from Neo4j: {e}")
        print("Please ensure Neo4j database is running and connection details are correct. And check Cypher query syntax for your Neo4j version.")
        sys.exit(1)
    
    output_dir = os.path.dirname(OUTPUT_JSONL)
    os.makedirs(output_dir, exist_ok=True)

    with open(OUTPUT_JSONL, 'w', encoding='utf-8') as f:
        seen_names = set()
        for entity in all_entities:
            if entity['name'] not in seen_names:
                f.write(json.dumps(entity, ensure_ascii=False) + '\n')
                seen_names.add(entity['name'])
            # else: # 如果你不想看到大量的重复警告，可以注释掉这行
            #     print(f"  Skipping duplicate entity name (from different types or multiple entries): {entity['name']}")

    print(f"Successfully extracted {len(seen_names)} unique entities to {OUTPUT_JSONL}")
    if not all_entities:
        print("WARNING: No entities were extracted from Neo4j. Please check your database data or label exclusion list.")

if __name__ == '__main__':
    extract_entities_from_neo4j()