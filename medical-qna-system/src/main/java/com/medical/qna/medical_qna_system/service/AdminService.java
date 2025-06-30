package com.medical.qna.medical_qna_system.service;


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

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

}