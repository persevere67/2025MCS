package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.entity.neo4j.KnowledgeNode;
import com.medical.qna.medical_qna_system.repository.neo4j.KnowledgeGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Neo4jQueryService {
    @Autowired
    private KnowledgeGraphRepository knowledgeRepository;
    
    public String queryKnowledgeGraph(String question) {
        String[] keywords = extractKeywords(question);
        StringBuilder answer = new StringBuilder();
        
        for (String keyword : keywords) {
            List<KnowledgeNode> nodes = knowledgeRepository.findByKeyword(keyword);
            if (!nodes.isEmpty()) {
                answer.append("关于").append(keyword).append("的信息：");
                nodes.forEach(node -> {
                    answer.append(node.getName()).append("；");
                });
            }
        }
        
        return answer.length() > 0 ? answer.toString() : "抱歉，没有找到相关信息。";
    }
    
    private String[] extractKeywords(String question) {
        return question.replaceAll("[？?！!。.]", "").split("\\s+");
    }
}
