package com.example.comus.domain.question.service;

import com.example.comus.domain.answer.entity.Answer;
import com.example.comus.domain.answer.repository.AnswerRepository;
import com.example.comus.domain.question.dto.response.QuestionCountResponseDto;
import com.example.comus.domain.question.dto.response.QuestionListResponseDto;
import com.example.comus.domain.question.dto.response.QuestionResponseDto;
import com.example.comus.domain.question.dto.response.RandomQuestionResponseDto;
import com.example.comus.domain.question.entity.*;
import com.example.comus.domain.question.repository.QuestionLikeRepository;
import com.example.comus.domain.question.repository.QuestionRepository;
import com.example.comus.domain.user.entity.User;
import com.example.comus.domain.user.repository.UserRespository;
import com.example.comus.global.error.exception.EntityNotFoundException;
import com.example.comus.global.error.exception.InvalidValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.comus.global.error.ErrorCode.*;

@RequiredArgsConstructor
@Service
@Transactional
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final UserRespository userRepository;
    private final AnswerRepository answerRepository;
    private final QuestionLikeRepository questionLikeRepository;

    // 랜덤 질문 아이디
    public Long getRandomQuestionId() {
        List<Question> questions = questionRepository.findAll();
        int randomIndex = (int) (Math.random() * questions.size());
        return questions.get(randomIndex).getId();
    }

    // 카테고리별 질문리스트 조회
    public List<QuestionListResponseDto> getQuestionList(Long userId, QuestionCategory category,  SortType sort) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        List<Question> questions = (category == null)
                ? questionRepository.findAll()
                : questionRepository.findByCategory(category);

        questions = sortQuestions(user, questions, sort);

        return questions.stream()
                .map(question -> toQuestionListResponseDto(question, user))
                .collect(Collectors.toList());
    }

    private List<Question> sortQuestions(User user, List<Question> questions, SortType sortType) {
        List<Pair<Question, Integer>> questionAnswerCounts = questions.stream()
                .map(question -> Pair.of(question, answerRepository.countByUserAndQuestion(user, question)))
                .collect(Collectors.toList());

        questionAnswerCounts.sort((pair1, pair2) -> {
            int count1 = pair1.getSecond();
            int count2 = pair2.getSecond();
            return switch (sortType) {
                case MOST_CHAT -> Integer.compare(count2, count1);
                case LEAST_CHAT -> Integer.compare(count1, count2);
                default -> pair1.getFirst().getId().compareTo(pair2.getFirst().getId());
            };
        });

        return questionAnswerCounts.stream()
                .map(Pair::getFirst)
                .collect(Collectors.toList());
    }

    private QuestionListResponseDto toQuestionListResponseDto(Question question, User user) {
        int answerCount = answerRepository.countByUserAndQuestion(user, question);
        boolean isLiked = questionLikeRepository.existsByUserAndQuestion(user, question);

        return QuestionListResponseDto.from(question, answerCount, isLiked);
    }

    public QuestionResponseDto getQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND));
        return QuestionResponseDto.from(question);
    }

    public List<String> getQuestionDatail(Long questionId) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND));
        String multipleChoices = question.getMultipleChoices();
        if (multipleChoices == null) {
            return null;
        }
        return List.of(multipleChoices.split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());

    }

    public void likeQuestion(Long userId, Long questionId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND));

        if (questionLikeRepository.existsByUserAndQuestion(user, question)) {
            throw new InvalidValueException(QUESTION_ALREADY_LIKED);
        }

        QuestionLike questionLike = QuestionLike.builder()
                .user(user)
                .question(question)
                .build();
        questionLikeRepository.save(questionLike);
    }

    public void unlikeQuestion(Long userId, Long questionId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND));

        QuestionLike questionLike = questionLikeRepository.findByUserAndQuestion(user, question)
                .orElseThrow(() -> new EntityNotFoundException(QUESTION_LIKE_NOT_FOUND));

        questionLikeRepository.delete(questionLike);
    }

    public RandomQuestionResponseDto getRandomQuestion() {
        Long questionId = getRandomQuestionId();
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new EntityNotFoundException(QUESTION_NOT_FOUND));
        return RandomQuestionResponseDto.from(question);
    }

    // 카테고리별 질문 통계 조회
    public List<QuestionCountResponseDto> getQuestionCountByCategory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다."));

        return List.of(QuestionCategory.values()).stream()
                .map(category -> {
                    List<Question> questions = questionRepository.findByCategory(category);
                    int totalCount = questions.size();
                    int answeredCount = 0;

                    for (Question question : questions) {
                        List<Answer> answers = answerRepository.findByQuestionAndUser(question, user);
                        if (!answers.isEmpty()) {
                            answeredCount++;
                        }
                    }

                    int percentage = totalCount == 0 ? 0 : (answeredCount * 100) / totalCount;
                    String count = answeredCount + "/" + totalCount;

                    return new QuestionCountResponseDto(category, totalCount, answeredCount, count, percentage + "%");
                })
                .collect(Collectors.toList());
    }
}
