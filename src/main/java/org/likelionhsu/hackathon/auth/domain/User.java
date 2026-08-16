package org.likelionhsu.hackathon.auth.domain;

import java.time.Instant;

import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "notification_email", length = 320)
    private String notificationEmail;

    @Column(
            name = "notification_email_verified",
            nullable = false
    )
    private boolean notificationEmailVerified;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected User() {
    }

    public static User local(
            String email,
            String nickname,
            Gender gender
    ) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.nickname = nickname;
        user.gender = gender;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        user.notificationEmailVerified = false;
        return user;
    }

    public static User social(
            String nickname,
            Gender gender,
            String notificationEmail
    ) {
        User user = new User();
        user.email = null;
        user.nickname = nickname;
        user.gender = gender;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        user.notificationEmail = normalizeEmail(notificationEmail);
        user.notificationEmailVerified = false;
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public Gender getGender() {
        return gender;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void updateProfile(
            String nickname,
            Gender gender
    ) {
        if (nickname != null) {
            this.nickname = nickname;
        }

        if (gender != null) {
            this.gender = gender;
        }
    }

    public Long getVersion() {
        return version;
    }

    private static String normalizeEmail(String email) {
        return email == null
                ? null
                : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
