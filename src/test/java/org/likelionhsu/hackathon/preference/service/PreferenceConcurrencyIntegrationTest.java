package org.likelionhsu.hackathon.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.exception.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.likelionhsu.hackathon.auth.domain.Gender;
import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.auth.repository.UserRepository;
import org.likelionhsu.hackathon.common.enums.ColorGroup;
import org.likelionhsu.hackathon.common.enums.ItemCategory;
import org.likelionhsu.hackathon.preference.dto.request.PreferenceRequest;
import org.likelionhsu.hackathon.preference.entity.PreferenceProfile;
import org.likelionhsu.hackathon.preference.entity.PreferenceStyleTag;
import org.likelionhsu.hackathon.product.entity.Product;
import org.likelionhsu.hackathon.wishlist.entity.Wishlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

@Testcontainers
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
@EntityScan(basePackageClasses = {
        PreferenceProfile.class,
        User.class,
        Product.class,
        Wishlist.class
})
class PreferenceConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("hackathon_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                mysql::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                mysql::getUsername
        );

        registry.add(
                "spring.datasource.password",
                mysql::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    private PreferenceService preferenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "DELETE FROM preference_profiles"
        );

        jdbcTemplate.update(
                "DELETE FROM users"
        );
    }

    @Test
    void stalePreferenceUpdateCausesOptimisticLockConflict() {
        User user =
                saveUser(
                        "preference-optimistic@example.com"
                );

        preferenceService.updatePreference(
                user.getId(),
                new PreferenceRequest(
                        List.of("BLACK"),
                        List.of("BAG"),
                        List.of("CASUAL")
                )
        );

        Long profileId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM preference_profiles
                        WHERE user_id = ?
                        """,
                        Long.class,
                        user.getId()
                );

        EntityManager firstEntityManager =
                entityManagerFactory
                        .createEntityManager();

        EntityManager secondEntityManager =
                entityManagerFactory
                        .createEntityManager();

        try {
            firstEntityManager
                    .getTransaction()
                    .begin();

            secondEntityManager
                    .getTransaction()
                    .begin();

            PreferenceProfile firstProfile =
                    firstEntityManager.find(
                            PreferenceProfile.class,
                            profileId
                    );

            PreferenceProfile secondProfile =
                    secondEntityManager.find(
                            PreferenceProfile.class,
                            profileId
                    );

            Long initialVersion =
                    firstProfile.getVersion();

            assertThat(
                    secondProfile.getVersion()
            ).isEqualTo(initialVersion);

            firstProfile.applyManualPreferences(
                    List.of(
                            ColorGroup.BLUE
                    ),
                    List.of(
                            ItemCategory.BAG
                    ),
                    List.of(
                            PreferenceStyleTag.FORMAL
                    )
            );

            firstEntityManager
                    .getTransaction()
                    .commit();

            secondProfile.applyManualPreferences(
                    List.of(
                            ColorGroup.BROWN
                    ),
                    List.of(
                            ItemCategory.SHOES
                    ),
                    List.of(
                            PreferenceStyleTag.GLAMOROUS
                    )
            );

            Throwable secondCommitFailure =
                    catchThrowable(() ->
                            secondEntityManager
                                    .getTransaction()
                                    .commit()
                    );

            assertThat(secondCommitFailure)
                    .isNotNull();

            assertThat(
                    hasCause(
                            secondCommitFailure,
                            OptimisticLockException.class
                    )
            ).isTrue();

            Long finalVersion =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT version
                            FROM preference_profiles
                            WHERE id = ?
                            """,
                            Long.class,
                            profileId
                    );

            assertThat(finalVersion)
                    .isEqualTo(
                            initialVersion + 1
                    );
        } finally {
            rollbackIfActive(
                    firstEntityManager
            );

            rollbackIfActive(
                    secondEntityManager
            );

            firstEntityManager.close();
            secondEntityManager.close();
        }
    }

    @Test
    void simultaneousFirstCreateCausesExactUniqueConstraintViolation()
            throws Exception {

        User user =
                saveUser(
                        "preference-unique@example.com"
                );

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> firstFuture =
                    executor.submit(() ->
                            createPreferenceConcurrently(
                                    user.getId(),
                                    ColorGroup.BLACK,
                                    ready,
                                    start
                            )
                    );

            Future<Throwable> secondFuture =
                    executor.submit(() ->
                            createPreferenceConcurrently(
                                    user.getId(),
                                    ColorGroup.BROWN,
                                    ready,
                                    start
                            )
                    );

            assertThat(
                    ready.await(
                            5,
                            TimeUnit.SECONDS
                    )
            ).isTrue();

            start.countDown();

            Throwable firstFailure =
                    firstFuture.get(
                            10,
                            TimeUnit.SECONDS
                    );

            Throwable secondFailure =
                    secondFuture.get(
                            10,
                            TimeUnit.SECONDS
                    );

            int failureCount =
                    (firstFailure == null ? 0 : 1)
                            + (secondFailure == null ? 0 : 1);

            assertThat(failureCount)
                    .isEqualTo(1);

            Throwable failure =
                    firstFailure != null
                            ? firstFailure
                            : secondFailure;

            ConstraintViolationException
                    constraintViolation =
                    findConstraintViolation(
                            failure
                    );

            assertThat(constraintViolation)
                    .isNotNull();

            assertThat(
                    constraintViolation
                            .getConstraintName()
            ).isEqualTo(
                    "preference_profiles."
                            + "uk_preference_profiles_user_id"
            );

            Integer rowCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM preference_profiles
                            WHERE user_id = ?
                            """,
                            Integer.class,
                            user.getId()
                    );

            assertThat(rowCount)
                    .isEqualTo(1);
        } finally {
            start.countDown();

            executor.shutdownNow();

            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }
    }

    private Throwable createPreferenceConcurrently(
            Long userId,
            ColorGroup color,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        EntityManager entityManager =
                entityManagerFactory
                        .createEntityManager();

        try {
            entityManager
                    .getTransaction()
                    .begin();

            User user =
                    entityManager.find(
                            User.class,
                            userId
                    );

            ready.countDown();

            boolean started =
                    start.await(
                            5,
                            TimeUnit.SECONDS
                    );

            if (!started) {
                throw new IllegalStateException(
                        "동시 실행 시작을 기다리는 시간이 초과되었습니다."
                );
            }

            PreferenceProfile profile =
                    PreferenceProfile.createManual(
                            user,
                            List.of(color),
                            List.of(
                                    ItemCategory.BAG
                            ),
                            List.of(
                                    PreferenceStyleTag.CASUAL
                            )
                    );

            entityManager.persist(profile);

            entityManager.flush();

            entityManager
                    .getTransaction()
                    .commit();

            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            rollbackIfActive(
                    entityManager
            );

            return exception;
        } catch (Throwable exception) {
            rollbackIfActive(
                    entityManager
            );

            return exception;
        } finally {
            entityManager.close();
        }
    }

    private ConstraintViolationException
    findConstraintViolation(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (current
                    instanceof ConstraintViolationException
                    constraintViolationException) {

                return constraintViolationException;
            }

            if (current.getCause() == current) {
                break;
            }

            current = current.getCause();
        }

        return null;
    }

    private boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> type
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }

            if (current.getCause() == current) {
                break;
            }

            current = current.getCause();
        }

        return false;
    }

    private void rollbackIfActive(
            EntityManager entityManager
    ) {
        if (entityManager
                .getTransaction()
                .isActive()) {

            entityManager
                    .getTransaction()
                    .rollback();
        }
    }

    private User saveUser(
            String email
    ) {
        return userRepository.saveAndFlush(
                User.local(
                        email,
                        "preference-user",
                        Gender.NOT_SPECIFIED
                )
        );
    }
}