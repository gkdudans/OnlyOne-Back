package com.example.onlyone.domain.chat.controller;

import com.example.onlyone.domain.chat.dto.ChatMessageRequest;
import com.example.onlyone.domain.chat.service.AsyncMessageService;
import com.example.onlyone.domain.chat.service.ChatPublisher;
import com.example.onlyone.domain.user.entity.Status;
import com.example.onlyone.domain.user.entity.User;
import com.example.onlyone.domain.user.repository.UserRepository;
import com.example.onlyone.global.exception.CustomException;
import com.example.onlyone.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChatWebSocketControllerTest {

    UserRepository userRepository;
    ChatPublisher chatPublisher;
    AsyncMessageService asyncMessageService;
    ObjectMapper objectMapper;
    ChatWebSocketController controller;

    @BeforeEach
    void setUp() throws Exception {
        userRepository = Mockito.mock(UserRepository.class);
        chatPublisher = Mockito.mock(ChatPublisher.class);
        asyncMessageService = Mockito.mock(AsyncMessageService.class);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        controller = new ChatWebSocketController(userRepository, asyncMessageService, chatPublisher, objectMapper);
    }

    private User mockUser(Long kakaoId, String nickname) {
        return User.builder()
                .userId(1L)
                .kakaoId(kakaoId)
                .nickname(nickname)
                .status(Status.ACTIVE)
                .profileImage("test.png")
                .build();
    }

    @Test
    @DisplayName("메시지 전송 성공 시, chatPublisher.publish가 호출되고 저장은 비동기로 위임된다")
    void sendMessageSuccess() {
        Long roomId = 77L;
        Long userId = 1001L;
        String text = "안녕";
        var req = ChatMessageRequest.fromText(userId, text);

        given(userRepository.findByKakaoId(userId)).willReturn(Optional.of(mockUser(userId, "닉네임")));

        controller.sendMessage(roomId, req);

        then(chatPublisher).should().publish(eq(roomId), anyString());
        then(asyncMessageService).should().saveMessageAsync(roomId, req);
    }

    @Test
    @DisplayName("존재하지 않는 유저일 경우 CustomException(USER_NOT_FOUND)을 던진다")
    void sendMessageUserNotFound() {
        Long roomId = 77L;
        Long userId = 9999L;
        String text = "안녕";
        var req = ChatMessageRequest.fromText(userId, text);

        given(userRepository.findByKakaoId(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> controller.sendMessage(roomId, req))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());

        verifyNoInteractions(chatPublisher);
        verifyNoInteractions(asyncMessageService);
    }

    @Test
    @DisplayName("전송 중 알 수 없는 예외 발생 시 MESSAGE_SERVER_ERROR로 래핑된다")
    void sendMessageUnknownExceptionWrapped() {
        Long roomId = 77L;
        Long userId = 1001L;
        String text = "안녕";
        var req = ChatMessageRequest.fromText(userId, text);

        given(userRepository.findByKakaoId(userId)).willThrow(new RuntimeException("DB down"));

        CustomException ex = catchThrowableOfType(
                () -> controller.sendMessage(roomId, req),
                CustomException.class
        );
        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MESSAGE_SERVER_ERROR);

        verifyNoInteractions(chatPublisher);
        verifyNoInteractions(asyncMessageService);
    }
}
