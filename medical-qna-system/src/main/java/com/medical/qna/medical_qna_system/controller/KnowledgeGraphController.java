package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.service.MedicalQnAService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge") // 新的API基础路径，与/api/question区分开
public class KnowledgeGraphController {

    private final MedicalQnAService qnaService;

    public KnowledgeGraphController(MedicalQnAService qnaService) {
        this.qnaService = qnaService;
    }

    /**
     * 知识图谱关键词查询接口
     * 例如：GET /api/knowledge/query/高血压
     * 用户输入高血压，返回其症状、忌食、所需要的药品等信息
     */
    @GetMapping("/query/{keyword}")
    public ResponseEntity<Map<String, Object>> queryKnowledgeGraphKeyword(@PathVariable String keyword) {
        Map<String, Object> result = qnaService.queryKnowledgeGraphByKeyword(keyword);
        if (result.containsKey("status") && "未找到相关信息".equals(result.get("status"))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
}