package org.likelionhsu.hackathon.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
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
class BaseTimeEntityTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void Entity를_저장하면_생성시각과_수정시각이_자동_기록된다() {

        TestAuditEntity entity =
                new TestAuditEntity(
                        "test"
                );

        entityManager.persist(entity);
        entityManager.flush();

        assertThat(
                entity.getCreatedAt()
        ).isNotNull();

        assertThat(
                entity.getUpdatedAt()
        ).isNotNull();

        assertThat(
                entity.getCreatedAt()
        ).isBeforeOrEqualTo(
                entity.getUpdatedAt()
        );
    }

    @Test
    void Auditing_시각은_UTC_Instant로_기록된다() {

        TestAuditEntity entity =
                new TestAuditEntity(
                        "test"
                );

        entityManager.persist(entity);
        entityManager.flush();

        Instant createdAt =
                entity.getCreatedAt();

        assertThat(createdAt)
                .isNotNull();

        assertThat(
                createdAt.toString()
        ).endsWith("Z");
    }

    @Test
    void Entity를_수정하면_수정시각이_갱신된다()
            throws InterruptedException {

        TestAuditEntity entity =
                new TestAuditEntity(
                        "before"
                );

        entityManager.persist(entity);
        entityManager.flush();

        Instant beforeUpdatedAt =
                entity.getUpdatedAt();

        Thread.sleep(10);

        entity.changeName(
                "after"
        );

        entityManager.flush();

        Instant afterUpdatedAt =
                entity.getUpdatedAt();

        assertThat(afterUpdatedAt)
                .isAfter(
                        beforeUpdatedAt
                );
    }
}