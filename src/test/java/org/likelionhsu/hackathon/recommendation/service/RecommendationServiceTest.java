package org.likelionhsu.hackathon.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.preference.repository.PreferenceRepository;
import org.likelionhsu.hackathon.product.repository.ProductImageRepository;
import org.likelionhsu.hackathon.product.repository.ProductRepository;
import org.likelionhsu.hackathon.product.repository.ProductTagMappingRepository;
import org.likelionhsu.hackathon.recommendation.dto.request.RecommendationRequest;
import org.likelionhsu.hackathon.recommendation.repository.RecommendationProductRepository;
import org.likelionhsu.hackathon.recommendation.repository.RecommendationRepository;
import org.likelionhsu.hackathon.recommendation.validation.RecommendationRequestValidator;
import org.likelionhsu.hackathon.wishlist.repository.WishlistRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock UserRepository userRepository;
    @Mock PreferenceRepository preferenceRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductTagMappingRepository productTagMappingRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock WishlistRepository wishlistRepository;
    @Mock RecommendationRepository recommendationRepository;
    @Mock RecommendationProductRepository recommendationProductRepository;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(
                userRepository,
                preferenceRepository,
                productRepository,
                productTagMappingRepository,
                productImageRepository,
                wishlistRepository,
                recommendationRepository,
                recommendationProductRepository,
                new RecommendationRequestValidator(),
                new RecommendationScorer(),
                new RecommendationReasonBuilder(),
                Clock.fixed(
                        Instant.parse("2026-08-16T00:00:00Z"),
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    void missingPreferenceDoesNotCreateRecommendation() {
        User user = User.local(
                "recommendation@example.com",
                "추천사용자",
                Gender.NOT_SPECIFIED
        );
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(preferenceRepository.findByUser_Id(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> service.createRecommendation(
                        1L,
                        new RecommendationRequest(
                                "DATE",
                                "AUTUMN",
                                List.of("COMPACT"),
                                null,
                                null
                        )
                ),
                BusinessException.class
        );

        assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.PREFERENCE_REQUIRED);
        verifyNoInteractions(
                productRepository,
                recommendationRepository,
                recommendationProductRepository
        );
    }
}
