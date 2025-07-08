package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.request.QuestionRequest; // Ensure QuestionRequest is imported
import com.medical.qna.medical_qna_system.dto.response.AnswerResponse; // Ensure AnswerResponse is imported
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import java.util.List;
import java.util.function.Consumer; // Used for streaming response callbacks

public interface QuestionService {

    /**
     * Processes a user question, performs semantic understanding, and retrieves an answer and recommended questions.
     * This method is primarily used in scenarios where the controller receives a QuestionRequest and expects an AnswerResponse.
     * @param request The request DTO containing the user's question.
     * @return The response DTO containing the answer and recommended questions.
     */
    AnswerResponse processQuestion(QuestionRequest request);

    /**
     * Processes a question and streams the answer back.
     * This method is used for scenarios requiring chunked answer reception, such as Server-Sent Events (SSE).
     * @param question The user's question.
     * @param chunkConsumer The callback function to receive answer chunks.
     */
    void processQuestionWithStream(String question, Consumer<String> chunkConsumer);

    /**
     * Saves a question-answer record.
     * @param userId The ID of the user.
     * @param question The user's question.
     * @param answer The answer provided.
     */
    void saveQuestionAnswer(Long userId, String question, String answer);

    /**
     * Retrieves a user's question-answer history.
     * @param userId The ID of the user.
     * @return A list of question-answer DTOs representing the user's history.
     */
    List<QuestionAnswerDto> getUserHistory(Long userId);

    /**
     * Deletes a specific question-answer record.
     * @param id The ID of the record to delete.
     * @param userId The ID of the user who owns the record.
     * @return True if the record was successfully deleted, false otherwise.
     */
    boolean deleteQuestionAnswer(Long id, Long userId);

    /**
     * Clears all question-answer history for a specific user.
     * @param userId The ID of the user whose history should be cleared.
     */
    void clearUserHistory(Long userId);

    /**
     * Searches a user's question-answer history by keyword.
     * @param userId The ID of the user.
     * @param keyword The keyword to search for within questions.
     * @return A list of matching question-answer DTOs.
     */
    List<QuestionAnswerDto> searchUserHistory(Long userId, String keyword);

    /**
     * Gets the total count of question-answer records for a user.
     * @param userId The ID of the user.
     * @return The total count of records.
     */
    long getUserQuestionCount(Long userId);

    /**
     * Retrieves a paginated list of a user's most recent question-answer records.
     * @param userId The ID of the user.
     * @param page The page number (0-indexed).
     * @param size The number of records per page.
     * @return A paginated list of recent question-answer DTOs.
     */
    List<QuestionAnswerDto> getUserRecentHistory(Long userId, int page, int size);

    /**
     * Deletes multiple question-answer records for a user.
     * @param ids A list of record IDs to delete.
     * @param userId The ID of the user who owns the records.
     * @return The number of records successfully deleted.
     */
    int batchDeleteQuestionAnswers(List<Long> ids, Long userId);

    /**
     * Checks if a user has any question-answer records.
     * @param userId The ID of the user.
     * @return True if the user has records, false otherwise.
     */
    boolean hasUserQuestions(Long userId);
}
