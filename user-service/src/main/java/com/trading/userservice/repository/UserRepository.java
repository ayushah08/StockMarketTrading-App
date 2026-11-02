package com.trading.userservice.repository;

import com.trading.userservice.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface  UserRepository  extends JpaRepository<User,String> {
    boolean existsByEmail(String email);

    Optional<User> findByEnmail(@NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email);
}
