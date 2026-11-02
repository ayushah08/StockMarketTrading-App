package com.trading.userservice.service;

import com.trading.userservice.dto.AuthResponse;
import com.trading.userservice.dto.LoginRequest;
import com.trading.userservice.dto.RegisterRequest;
import com.trading.userservice.dto.UserResponse;
import com.trading.userservice.entity.User;
import com.trading.userservice.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-Expiration}")
    private long refreshExpiration;

    private static final String USER_REGISTERED_TOPIC = "user.registered";

    public AuthResponse register(RegisterRequest request) {
        log.info("registering user : {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered " + request.getEmail());
        }

        User user = User.builder().email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).firstName(request.getFirstName()).lastName(request.getLastName()).walletBalance(request.getWalletBalance() != null ? request.getWalletBalance() : BigDecimal.valueOf(10000)).build();

        User savedUser = userRepository.save(user);
        log.info("User Registered  : {}", savedUser);

        //publish user.registered Event

        Map<String, Object> userRegisteredEvent = new HashMap<>();
        userRegisteredEvent.put("userId", savedUser.getId());
        userRegisteredEvent.put("email", savedUser.getEmail());
        userRegisteredEvent.put("firstName", savedUser.getFirstName());
        userRegisteredEvent.put("lastName", savedUser.getLastName());
        userRegisteredEvent.put("walletBalance", savedUser.getWalletBalance());

        kafkaTemplate.send(USER_REGISTERED_TOPIC, savedUser.getId(), userRegisteredEvent);

        String token = generateAccessToken(savedUser.getId(), savedUser.getEmail());

        String refreshToken = generateRefereshToken(savedUser.getId());

        return buildAuthResponse(savedUser, token, refreshToken);
    }

    /* Generate Referesh Token
    Used To get a new Access token when it expires
    Server validates and returns new access Token
    Client Sends referesh token to /auth/referesh endpoint
    * */
    private String generateRefereshToken(String id) {
        return Jwts.builder().claim("Userid", id).setSubject(id).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)).signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();
    }


    //Generates AccessTokern
    private String generateAccessToken(String id, String email) {

        return Jwts.builder().claim("Userid", id).setSubject(email).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)).signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();

    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(token);
        authResponse.setRefreshToken(refreshToken);
        authResponse.setUserId(user.getId());
        authResponse.setEmail(user.getEmail());
        authResponse.setFirstName(user.getFirstName());
        authResponse.setLastName(user.getLastName());
        authResponse.setWalletBalance(user.getWalletBalance());
        return authResponse;
    }


    public AuthResponse login(@Valid LoginRequest request) {

        log.info("logging attempt : {}", request.getEmail());

        User user = userRepository.findByEnmail(request.getEmail()).orElseThrow(() -> new RuntimeException("User not found with email : " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid Credentials");
        }
        log.info("login Successful : {}", user.getId());

        String token = generateAccessToken(user.getId(), user.getEmail());

        String refreshToken = generateRefereshToken(user.getId());

        return buildAuthResponse(user, token, refreshToken);


    }


    //To Map User Entity to UserResponse Dto
    public UserResponse mapToUserResponse(User user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setWalletBalance(user.getWalletBalance());
        return userResponse;
    }

    //To Get USer By UID
    /*
    Called By other services by feign client
    @param User Id
    @return UserResponse
     */
    public UserResponse getUserById(String userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found with id : " + userId));
        return mapToUserResponse(user);
    }


    public UserResponse addFunds(String userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found with id : " + userId));

        user.setWalletBalance(user.getWalletBalance().add(amount));
        User savedUser= userRepository.save(user);
        log.info(" funds added :{} successfully : {}",savedUser.getId(), savedUser.getWalletBalance() );
        return mapToUserResponse(savedUser);
    }

    public UserResponse deductFunds(String userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found with id : " + userId));
        if(user.getWalletBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient Balance");
        }
        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        User savedUser= userRepository.save(user);
        log.info(" funds deducted :{} successfully : {}",user.getId(), savedUser.getWalletBalance() );

        return mapToUserResponse(savedUser);
    }

    public UserResponse creditFunds(String userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found with id : " + userId));

        user.setWalletBalance(user.getWalletBalance().add(amount));
       User savedUser= userRepository.save(user);
        log.info(" funds Credited :{} successfully : {}",savedUser.getId(), savedUser.getWalletBalance() );
        return mapToUserResponse(savedUser);
    }
}
