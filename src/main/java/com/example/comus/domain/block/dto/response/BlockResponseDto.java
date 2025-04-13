package com.example.comus.domain.block.dto.response;

import com.example.comus.domain.answer.entity.Answer;
import com.example.comus.domain.question.entity.AnswerType;
import com.example.comus.domain.question.entity.QuestionCategory;

import java.util.List;

public record BlockResponseDto(
        Long questionId,
        QuestionCategory category,
        String questionContent,
        Long answerId,
        AnswerType answerType,
        String answerContent,
        List<BlockPlaceResponseDto> blockPlace
) {
    public static BlockResponseDto of(Answer answer, List<BlockPlaceResponseDto> blockPlace) {
        return new BlockResponseDto(
                answer.getQuestion().getId(),
                answer.getQuestion().getCategory(),
                answer.getQuestion().getQuestionContent(),
                answer.getId(),
                answer.getAnswerType(),
                answer.getAnswerContent(),
                blockPlace
        );
    }
}
