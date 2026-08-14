package org.likelionhsu.hackathon.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.EmailVerification;
import org.likelionhsu.hackathon.auth.domain.EmailVerificationPurpose;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.LocalCredential;
import org.likelionhsu.hackathon.auth.domain.ReauthToken;
import org.likelionhsu.hackathon.auth.domain.ReauthTokenPurpose;
import org.likelionhsu.hackathon.auth.domain.RefreshToken;
import org.likelionhsu.hackathon.auth.domain.TermsAgreement;
import org.likelionhsu.hackathon.auth.domain.TermsType;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.domain.UserRole;
import org.likelionhsu.hackathon.auth.domain.UserStatus;
import org.likelionhsu.hackathon.common.config.ClockConfig;
import org.likelionhsu.hackathon.common.config.JpaAuditingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest(showSql = false)
@Import({
        ClockConfig.class,
        JpaAuditingConfig.class
})
class LocalAuthRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocalCredentialRepository localCredentialRepository;

    @Autowired
    private TermsAgreementRepository termsAgreementRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ReauthTokenRepository reauthTokenRepository;

    @Test
    void Local_사용자와_로그인_자격증명을_저장하고_함께_조회한다() {
        User user = userRepository.save(
                User.local(
                        " USER@Example.com ",
                        "오늘뭐입지",
                        Gender.NOT_SPECIFIED
                )
        );
        localCredentialRepository.save(
                new LocalCredential(
                        user,
                        "USER_1234",
                        "encoded-password"
                )
        );

        var credential = localCredentialRepository
                .findWithUserByLoginId("user_1234")
                .orElseThrow();

        assertThat(credential.getUser().getId())
                .isEqualTo(user.getId());
        assertThat(credential.getUser().getEmail())
                .isEqualTo("user@example.com");
        assertThat(credential.getUser().getRole())
                .isEqualTo(UserRole.USER);
        assertThat(credential.getUser().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
        assertThat(credential.getUser().getVersion())
                .isZero();
        assertThat(credential.getLoginId())
                .isEqualTo("user_1234");
    }

    @Test
    void 약관_동의는_사용자별로_조회한다() {
        User user = saveUser("terms@example.com", "약관사용자");
        Instant now = Instant.parse("2026-08-13T05:00:00Z");

        termsAgreementRepository.save(
                new TermsAgreement(
                        user,
                        TermsType.SERVICE_TERMS,
                        "2026-08-01",
                        true,
                        now
                )
        );
        termsAgreementRepository.save(
                new TermsAgreement(
                        user,
                        TermsType.EMAIL_MARKETING,
                        "2026-08-01",
                        false,
                        now
                )
        );

        var agreements = termsAgreementRepository
                .findAllByUserId(user.getId());

        assertThat(agreements)
                .extracting(TermsAgreement::getTermsType)
                .containsExactlyInAnyOrder(
                        TermsType.SERVICE_TERMS,
                        TermsType.EMAIL_MARKETING
                );
    }

    @Test
    void 이메일_인증은_최신_요청과_24시간_발송수를_조회한다() {
        Instant now = Instant.parse("2026-08-13T05:00:00Z");
        EmailVerification verification =
                EmailVerification.signup(
                        "verify@example.com",
                        "code-hash",
                        now.plusSeconds(300)
                );

        emailVerificationRepository.saveAndFlush(verification);

        var latest = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        "verify@example.com",
                        EmailVerificationPurpose.SIGNUP
                )
                .orElseThrow();
        long dailyCount = emailVerificationRepository
                .countByEmailAndPurposeAndCreatedAtGreaterThanEqual(
                        "verify@example.com",
                        EmailVerificationPurpose.SIGNUP,
                        now.minusSeconds(86_400)
                );

        assertThat(latest.getId()).isEqualTo(verification.getId());
        assertThat(dailyCount).isEqualTo(1);
    }

    @Test
    void Refresh_Token은_폐기_전과_만료_전에만_사용할_수_있다() {
        User user = saveUser("refresh@example.com", "토큰사용자");
        Instant now = Instant.parse("2026-08-13T05:00:00Z");
        RefreshToken refreshToken = refreshTokenRepository.save(
                new RefreshToken(
                        user,
                        "a".repeat(64),
                        "00000000-0000-0000-0000-000000000001",
                        now.plusSeconds(3600)
                )
        );

        assertThat(refreshToken.isUsableAt(now)).isTrue();

        refreshToken.revoke(now.plusSeconds(10));

        assertThat(refreshToken.isUsableAt(now.plusSeconds(20)))
                .isFalse();
    }

    @Test
    void 재인증_Token은_만료_전_한_번만_사용할_수_있다() {
        User user = saveUser("reauth@example.com", "재인증사용자");
        Instant now = Instant.parse("2026-08-13T05:00:00Z");
        ReauthToken reauthToken = reauthTokenRepository.saveAndFlush(
                new ReauthToken(
                        user,
                        ReauthTokenPurpose.ACCOUNT_DELETE,
                        "b".repeat(64),
                        now.plusSeconds(300)
                )
        );

        ReauthToken found = reauthTokenRepository
                .findByTokenHashForUpdate("b".repeat(64))
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(reauthToken.getId());
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getPurpose())
                .isEqualTo(ReauthTokenPurpose.ACCOUNT_DELETE);
        assertThat(found.isUsableAt(now)).isTrue();

        found.consume(now.plusSeconds(30));

        assertThat(found.isUsableAt(now.plusSeconds(31))).isFalse();
    }

    private User saveUser(String email, String nickname) {
        return userRepository.save(
                User.local(
                        email,
                        nickname,
                        Gender.NOT_SPECIFIED
                )
        );
    }
}
