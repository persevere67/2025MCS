package com.medical.qna.medical_qna_system.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "answer")
public class Answer implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 问题ID
    @Column(nullable = false)
    private Long questionId;

    // 回答内容
    @Column(nullable = false, length = 1000)
    private String answerContent;

    // 回答时间
    @Column(nullable = false)
    private LocalDateTime answerTime;

    // 可扩展：回答来源、置信度等

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getAnswerContent() {
        return answerContent;
    }

    public void setAnswerContent(String answerContent) {
        this.answerContent = answerContent;
    }

    public LocalDateTime getAnswerTime() {
        return answerTime;
    }

    public void setAnswerTime(LocalDateTime answerTime) {
        this.answerTime = answerTime;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    @Override
    public String toString() {
        return "Answer{" +
                "id=" + id +
                ", questionId=" + questionId +
                ", answerContent='" + answerContent + '\'' +
                ", answerTime=" + answerTime +
                '}';
    }
}