package org.likelionhsu.hackathon.auth.domain;

import java.util.Locale;

import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "local_credentials")
public class LocalCredential extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(
            name = "login_id",
            nullable = false,
            unique = true,
            length = 20
    )
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    protected LocalCredential() {
    }

    public LocalCredential(
            User user,
            String loginId,
            String passwordHash
    ) {
        this.user = user;
        this.loginId = normalizeLoginId(loginId);
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    private static String normalizeLoginId(String loginId) {
        return loginId == null
                ? null
                : loginId.toLowerCase(Locale.ROOT);
    }
}
