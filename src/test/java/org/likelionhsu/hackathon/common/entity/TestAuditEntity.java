package org.likelionhsu.hackathon.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_audit_entity")
class TestAuditEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(nullable = false)
    private String name;

    protected TestAuditEntity() {
    }

    TestAuditEntity(String name) {
        this.name = name;
    }

    void changeName(String name) {
        this.name = name;
    }
}