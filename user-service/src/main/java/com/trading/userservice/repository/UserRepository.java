package com.trading.userservice.repository;

import com.trading.userservice.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface  UserRepository  extends JpaRepository<User,String> {
    boolean existsByEmail(String email);
}
