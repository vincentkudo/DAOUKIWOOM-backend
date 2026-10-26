package com.daoukiwoom.roomflow.Repository;

import com.daoukiwoom.roomflow.Entity.Role;
import com.daoukiwoom.roomflow.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    Page<User> findAllByActiveTrueOrderByFullNameAsc(Pageable pageable);

    long countByRole(Role role);
}
