package org.likelionhsu.hackathon.wishlist.entity;

import org.likelionhsu.hackathon.auth.domain.User;
import org.likelionhsu.hackathon.common.entity.BaseTimeEntity;
import org.likelionhsu.hackathon.product.entity.Product;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "wishlists",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wishlists_user_product",
                        columnNames = {
                                "user_id",
                                "product_id"
                        }
                )
        }
)
public class Wishlist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    protected Wishlist() {
    }

    private Wishlist(
            User user,
            Product product
    ) {
        this.user = user;
        this.product = product;
    }

    public static Wishlist create(
            User user,
            Product product
    ) {
        return new Wishlist(
                user,
                product
        );
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Product getProduct() {
        return product;
    }
}