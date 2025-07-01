package com.medical.qna.medical_qna_system.service.impl;

import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.repository.mysql.QuestionAnswerRepository;
import com.medical.qna.medical_qna_system.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {
    
    private final QuestionAnswerRepository questionAnswerRepository;
    
    // 简单的问答知识库（后续可以接入AI服务）
    private static final Map<String, String> SIMPLE_QA_MAP = new HashMap<>();
    
    static {
        // 常见医疗问题的简单回答
        SIMPLE_QA_MAP.put("头痛", "头痛可能由多种原因引起，如紧张、睡眠不足、压力等。建议充分休息，放松心情。如果头痛持续或加重，请及时就医。");
        SIMPLE_QA_MAP.put("发烧", "发烧通常是身体对感染的反应。轻度发烧可以多喝水、休息。体温超过38.5°C或持续不退，应及时就医。");
        SIMPLE_QA_MAP.put("咳嗽", "咳嗽可能是感冒、过敏或其他呼吸道疾病的症状。保持室内空气湿润，多喝温水。持续咳嗽或伴有其他症状请就医。");
        SIMPLE_QA_MAP.put("腹痛", "腹痛原因多样，可能是消化不良、胃炎等。注意饮食清淡，避免刺激性食物。剧烈腹痛或持续不缓解应立即就医。");
        SIMPLE_QA_MAP.put("失眠", "失眠可能与压力、环境、生活习惯有关。建议规律作息，睡前避免刺激性活动，营造良好的睡眠环境。");
        SIMPLE_QA_MAP.put("感冒", "感冒通常由病毒引起，症状包括鼻塞、流涕、咳嗽等。多休息、多喝水，保持室内通风。症状严重或持续请就医。");
    }
    
    @Override
    @Transactional
    public QuestionAnswerDto processQuestion(String question, User user) {
        log.info("用户 {} 提问: {}", user.getUsername(), question);
        
        // 生成答案
        String answer = generateAnswer(question);
        
        // 保存问答记录到数据库
        QuestionAnswer qa = QuestionAnswer.builder()
                .user(user)
                .question(question)
                .answer(answer)
                .createAt(LocalDateTime.now())
                .build();
        
        questionAnswerRepository.save(qa);
        
        log.info("为用户 {} 生成答案: {}", user.getUsername(), answer);
        
        return QuestionAnswerDto.create(question, answer);
    }
    
    /**
     * 简单的答案生成逻辑
     * 后续可以替换为调用AI服务（如DeepSeek）
     */
    private String generateAnswer(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "抱歉，我没有收到您的问题，请重新输入。";
        }
        
        // 简单的关键词匹配
        String normalizedQuestion = question.toLowerCase().trim();
        
        for (Map.Entry<String, String> entry : SIMPLE_QA_MAP.entrySet()) {
            if (normalizedQuestion.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 默认回答
        return String.format("感谢您的提问：\"%s\"。这是一个很好的健康问题。建议您：\n" +
                "1. 保持良好的生活习惯\n" +
                "2. 注意饮食均衡\n" +
                "3. 适当运动\n" +
                "4. 如有不适症状，请及时咨询专业医生\n\n" +
                "如果您有更具体的症状描述，我可以为您提供更针对性的建议。", question);
    }
}
