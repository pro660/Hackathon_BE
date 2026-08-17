package org.likelionhsu.hackathon.useritem.repository;

import java.util.List;
import java.util.Optional;

import org.likelionhsu.hackathon.useritem.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserItemRepository
        extends JpaRepository<UserItem, Long>,
        JpaSpecificationExecutor<UserItem> {

    Optional<UserItem> findByIdAndUser_IdAndDeletedAtIsNull(
            Long id,
            Long userId
    );

    Optional<UserItem> findByIdAndUser_Id(
            Long id,
            Long userId
    );

    List<UserItem> findAllByUser_IdAndDeletedAtIsNullOrderByIdAsc(
            Long userId
    );
}
