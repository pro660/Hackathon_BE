package org.likelionhsu.hackathon.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActiveUserFilterTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityErrorWriter errorWriter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeUserRequestContinues() throws Exception {
        User user = user(UserStatus.ACTIVE);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        authenticate(1L);
        MockFilterChain chain = new MockFilterChain();

        new ActiveUserFilter(userRepository, errorWriter)
                .doFilter(
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        chain
                );

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(errorWriter);
    }

    @Test
    void deletedUserAccessTokenIsRejected() throws Exception {
        User user = user(UserStatus.DELETED);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        authenticate(1L);
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        new ActiveUserFilter(userRepository, errorWriter)
                .doFilter(
                        new MockHttpServletRequest(),
                        response,
                        new MockFilterChain()
                );

        verify(errorWriter).write(
                response,
                ErrorCode.ACCOUNT_NOT_ACTIVE
        );
        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isNull();
    }

    private User user(UserStatus status) {
        User user = User.local(
                "user@example.com",
                "사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "status", status);
        return user;
    }

    private void authenticate(Long userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(String.valueOf(userId))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt)
        );
    }
}
