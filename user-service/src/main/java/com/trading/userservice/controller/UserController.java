package com.trading.userservice.controller;

import com.trading.userservice.dto.AuthResponse;
import com.trading.userservice.dto.LoginRequest;
import com.trading.userservice.dto.RegisterRequest;
import com.trading.userservice.dto.UserResponse;
import com.trading.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> profile(@RequestHeader("X-User-Id") String userId){

        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId){

        return ResponseEntity.ok(userService.getUserById(userId));

    }

    @PostMapping("/{userId}/funds/add")
    public ResponseEntity<UserResponse> addFunds(@PathVariable String userId ,
                                                 @RequestParam BigDecimal amount){

        return ResponseEntity.ok(userService.addFunds(userId,amount);
    }

    @PostMapping("/{userId}/funds/deduct")
    public ResponseEntity<UserResponse> deductFunds(@PathVariable String userId ,
                                                 @RequestParam BigDecimal amount){

        return ResponseEntity.ok(userService.deductFunds(userId,amount);
    }


    @PostMapping("/{userId}/funds/credit")
    public ResponseEntity<UserResponse> creditFunds(@PathVariable String userId ,
                                                    @RequestParam BigDecimal amount){

        return ResponseEntity.ok(userService.creditFunds(userId,amount);
    }



}
