package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.AdminDashboardDto;
import com.medical.qna.medical_qna_system.dto.UserQuestionHistoryDto;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.enums.UserRole;
import com.medical.qna.medical_qna_system.repository.mysql.UserRepository;
import com.medical.qna.medical_qna_system.repository.mysql.QuestionAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private QuestionAnswerRepository qaRepository;
    
    public AdminDashboardDto getDashboardData() {
        AdminDashboardDto dashboard = new AdminDashboardDto();
        
        dashboard.setTotalUsers(userRepository.countByRole(UserRole.USER));
        dashboard.setTotalQuestions(qaRepository.count());
        dashboard.setQuestionsToday(qaRepository.countTodayQuestions());
        
        List<Object[]> userStats = qaRepository.findUserQuestionStats(PageRequest.of(0, 10));
        List<AdminDashboardDto.UserSummaryDto> activeUsers = userStats.stream()
            .map(stat -> {
                User user = (User) stat[0];
                Long questionCount = (Long) stat[1];
                LocalDateTime lastQuestionTime = (LocalDateTime) stat[2];
                
                AdminDashboardDto.UserSummaryDto summary = new AdminDashboardDto.UserSummaryDto();
                summary.setUserId(user.getId());
                summary.setUsername(user.getUsername());
                summary.setQuestionCount(questionCount);
                summary.setLastQuestionTime(lastQuestionTime);
                return summary;
            })
            .collect(Collectors.toList());
        
        dashboard.setRecentActiveUsers(activeUsers);
        return dashboard;
    }
    
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAllUsersOrderByCreatedAtDesc(pageable);
    }
    
    public UserQuestionHistoryDto getUserQuestionHistory(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<QuestionAnswer> questionAnswers = qaRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        UserQuestionHistoryDto dto = new UserQuestionHistoryDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setUserCreatedAt(user.getCreatedAt());
        
        List<UserQuestionHistoryDto.QuestionAnswerDto> qaDtos = questionAnswers.stream()
            .map(qa -> {
                UserQuestionHistoryDto.QuestionAnswerDto qaDto = new UserQuestionHistoryDto.QuestionAnswerDto();
                qaDto.setId(qa.getId());
                qaDto.setQuestion(qa.getQuestion());
                qaDto.setAnswer(qa.getAnswer());
                qaDto.setCreatedAt(qa.getCreatedAt());
                return qaDto;
            })
            .collect(Collectors.toList());
        
        dto.setQuestionAnswers(qaDtos);
        return dto;
    }
    
    public Page<QuestionAnswer> getAllQuestionAnswers(Pageable pageable) {
        return qaRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Page<QuestionAnswer> getUserQuestionAnswers(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return qaRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
}