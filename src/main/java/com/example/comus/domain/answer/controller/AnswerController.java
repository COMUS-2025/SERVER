package com.example.comus.domain.answer.controller;

import com.example.comus.domain.answer.dto.request.AnswerRequestDto;
import com.example.comus.domain.answer.dto.response.AnswerByQuestionResponseDto;
import com.example.comus.domain.answer.dto.response.AnswerHistoryListResponseDto;
import com.example.comus.domain.answer.service.AnswerService;
import com.example.comus.global.common.SuccessResponse;
import com.example.comus.global.config.auth.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/answer")
@RestController
public class AnswerController {
    private final AnswerService answerService;

    //답변하기
    //TODO: 객관식 답변은 주어진 답변 중 선택하도록 예외 처리 필요
    @PostMapping
    public ResponseEntity<?> createAnswer(@UserId Long userId, @RequestBody AnswerRequestDto answerRequest) {
        answerService.createAnswer(userId, answerRequest);
        return SuccessResponse.ok(null);
    }

    //월별 상세 페이지 조회
    @GetMapping
    public ResponseEntity<?> getAnswer(@UserId Long userId) {
        List<AnswerHistoryListResponseDto> answerHistory = answerService.getAnswerHistory(userId);
        return SuccessResponse.ok(answerHistory);
    }

    //이전 답변 보기 페이지 조회
    @GetMapping("/{questionId}")
    public ResponseEntity<?> getAnswerByQuestion(@UserId Long userId, @PathVariable Long questionId) {
        AnswerByQuestionResponseDto answerByQuestion = answerService.getAnswerByQuestion(userId, questionId);
        return SuccessResponse.ok(answerByQuestion);
    }

}
