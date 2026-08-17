package org.likelionhsu.hackathon.careguide.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.careguide.domain.CareIntervalUnit;
import org.likelionhsu.hackathon.careguide.domain.CareRoutineType;
import org.likelionhsu.hackathon.careguide.domain.CareUnavailableReason;
import org.likelionhsu.hackathon.careguide.dto.CareGuideResponse;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicy;
import org.likelionhsu.hackathon.careguide.policy.MaterialCarePolicyRegistry;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.common.enums.MaterialGroup;
import org.likelionhsu.hackathon.common.enums.MaterialSource;
import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.likelionhsu.hackathon.useritem.repository.UserItemRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CareGuideServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ITEM_ID = 10L;
    private static final Instant NOW =
            Instant.parse("2026-08-18T00:00:00Z");

    @Mock
    UserItemRepository userItemRepository;

    @Mock
    MaterialCarePolicyRegistry policyRegistry;

    CareGuideService service;

    @BeforeEach
    void setUp() {
        service = new CareGuideService(
                userItemRepository,
                policyRegistry,
                new CareScheduleCalculator(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void leatherGuideUsesUserItemMaterialAndPurchaseDate() {
        UserItem item = item(
                MaterialGroup.LEATHER,
                LocalDate.of(2026, 8, 10)
        );

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        ITEM_ID,
                        USER_ID
                ))
                .thenReturn(Optional.of(item));
        when(policyRegistry.get(MaterialGroup.LEATHER))
                .thenReturn(leatherPolicy());

        CareGuideResponse response =
                service.getCareGuide(USER_ID, ITEM_ID);

        assertThat(response.material())
                .isEqualTo(MaterialGroup.LEATHER);
        assertThat(response.availability().calendarAvailable())
                .isTrue();
        assertThat(response.availability().reminderAvailable())
                .isTrue();
        assertThat(response.routines()).hasSize(2);
        assertThat(response.nextRecommendedCare().date())
                .isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void missingPurchaseDateKeepsGuideButDisablesSchedule() {
        UserItem item = item(MaterialGroup.LEATHER, null);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        ITEM_ID,
                        USER_ID
                ))
                .thenReturn(Optional.of(item));
        when(policyRegistry.get(MaterialGroup.LEATHER))
                .thenReturn(leatherPolicy());

        CareGuideResponse response =
                service.getCareGuide(USER_ID, ITEM_ID);

        assertThat(response.availability().careGuideAvailable())
                .isTrue();
        assertThat(response.availability().storageGuideAvailable())
                .isTrue();
        assertThat(response.availability().calendarAvailable())
                .isFalse();
        assertThat(response.availability().unavailableReason())
                .isEqualTo(
                        CareUnavailableReason.PURCHASE_DATE_REQUIRED
                );
        assertThat(response.nextRecommendedCare()).isNull();
    }

    @Test
    void missingMaterialDisablesMaterialBasedFeatures() {
        UserItem item = item(null, null);

        when(userItemRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(
                        ITEM_ID,
                        USER_ID
                ))
                .thenReturn(Optional.of(item));

        CareGuideResponse response =
                service.getCareGuide(USER_ID, ITEM_ID);

        assertThat(response.material()).isNull();
        assertThat(response.availability().careGuideAvailable())
                .isFalse();
        assertThat(response.availability().unavailableReason())
                .isEqualTo(CareUnavailableReason.MATERIAL_REQUIRED);
    }

    private UserItem item(
            MaterialGroup material,
            LocalDate purchaseDate
    ) {
        User user = User.local(
                "care@example.com",
                "관리사용자",
                Gender.NOT_SPECIFIED
        );
        ReflectionTestUtils.setField(user, "id", USER_ID);

        UserItem item = UserItem.create(
                user,
                null,
                "MCM",
                "내 가방",
                ItemCategory.BAG,
                ColorGroup.BLACK,
                material,
                material == null
                        ? null
                        : MaterialSource.USER_CONFIRMED,
                purchaseDate,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(item, "id", ITEM_ID);
        return item;
    }

    private MaterialCarePolicy leatherPolicy() {
        return new MaterialCarePolicy(
                "가죽",
                "가죽 관리",
                "설명",
                List.of(
                        new MaterialCarePolicy.RoutinePolicy(
                                CareRoutineType.CLEANING,
                                3,
                                CareIntervalUnit.MONTH,
                                "가죽 클리닝",
                                "설명"
                        ),
                        new MaterialCarePolicy.RoutinePolicy(
                                CareRoutineType.CONDITIONING,
                                6,
                                CareIntervalUnit.MONTH,
                                "가죽 컨디셔닝",
                                "설명"
                        )
                ),
                new MaterialCarePolicy.StoragePolicy(
                        List.of("피해야 할 환경"),
                        List.of("습기 관리"),
                        List.of("보관법")
                )
        );
    }
}
