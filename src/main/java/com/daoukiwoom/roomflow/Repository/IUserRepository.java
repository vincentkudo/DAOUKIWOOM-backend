package com.daoukiwoom.roomflow.Repository;

import com.daoukiwoom.roomflow.Entity.Role;
import com.daoukiwoom.roomflow.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findAllByActiveTrueOrderByFullNameAsc();

    long countByRole(Role role);
}
