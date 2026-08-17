package org.likelionhsu.hackathon.home.service;

import java.util.List;

import org.likelionhsu.hackathon.common.exception.BusinessException;
import org.likelionhsu.hackathon.common.exception.ErrorCode;
import org.likelionhsu.hackathon.home.dto.HomeResponse;
import org.likelionhsu.hackathon.home.repository.HomeQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {

    private static final String ACTIVE = "ACTIVE";

    private final HomeQueryRepository homeQueryRepository;

    public HomeService(HomeQueryRepository homeQueryRepository) {
        this.homeQueryRepository = homeQueryRepository;
    }

    @Transactional(readOnly = true)
    public HomeResponse getHome(Long userId) {
        HomeQueryRepository.UserSummaryRow user =
                homeQueryRepository.findUserSummary(userId)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.ACCESS_TOKEN_INVALID
                        ));

        if (!ACTIVE.equals(user.status())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        HomeResponse.LatestStylePlan latestStylePlan =
                homeQueryRepository.findLatestStylePlan(userId)
                        .map(row -> new HomeResponse.LatestStylePlan(
                                String.valueOf(row.stylePlanId()),
                                row.title(),
                                row.thumbnailImageUrl()
                        ))
                        .orElse(null);

        List<HomeResponse.RecommendedProduct> recommendedProducts =
                homeQueryRepository.findLatestRecommendedProducts(userId)
                        .stream()
                        .map(row -> new HomeResponse.RecommendedProduct(
                                String.valueOf(row.productId()),
                                row.name(),
                                row.matchScore(),
                                row.primaryImageUrl()
                        ))
                        .toList();

        return new HomeResponse(
                new HomeResponse.UserSummary(
                        user.nickname(),
                        user.preferenceCompleted(),
                        user.myItemCount()
                ),
                latestStylePlan,
                recommendedProducts
        );
    }
}
