package com.sai.finance.finance_manager.service;

import com.sai.finance.finance_manager.dto.LoginRequest;
import com.sai.finance.finance_manager.dto.LoginResponse;
import com.sai.finance.finance_manager.dto.RefreshTokenResponse;
import com.sai.finance.finance_manager.dto.RegisterRequest;
import com.sai.finance.finance_manager.model.RefreshToken;
import com.sai.finance.finance_manager.model.User;
import com.sai.finance.finance_manager.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.sai.finance.finance_manager.wallet.service.WalletService;

import java.util.Date;
import java.util.List;

@Service

public class UserService {

    private final UserRepository repo;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final RefreshTokenService refreshTokenService;


    public UserService(
            UserRepository repo,
            OtpService otpService,
            JwtService jwtService,
            WalletService walletService,
            RefreshTokenService refreshTokenService
    ) {
        this.repo = repo;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.walletService = walletService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }


    // ------------------ OLD CRUD METHODS (KEEP THEM) ------------------

    public List<User> getAllUsers() {
        return repo.findAll();
    }

    public User createUser(User user) {
        return repo.save(user);
    }

    public void deleteUser(Long id) {
        repo.deleteById(id);
    }

    // ------------------ NEW AUTH METHODS ------------------

    public String registerUser(RegisterRequest request) {

        // VALIDATION
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Name cannot be empty");
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!request.getEmail().matches(emailRegex)) {
            throw new RuntimeException("Invalid email format");
        }

        if (repo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        if (!request.getPassword().matches(passwordRegex)) {
            throw new RuntimeException(
                    "Password must be at least 8 characters long, contain 1 uppercase, 1 lowercase, 1 number, and 1 special character"
            );
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // CREATE USER
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setVerified(false);
        user.setLoginMethod("PASSWORD");
        user.setCreatedAt(new Date());

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(hashedPassword);

        repo.save(user);

        // SEND OTP
        otpService.generateAndSendOtp(request.getEmail(), request.getName());

        return "Registration successful. OTP sent to email for verification.";
    }

    public LoginResponse loginUser(LoginRequest request) {

        User user = repo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isVerified()) {
            throw new RuntimeException("Email not verified. Please verify OTP first.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate access token (5 minutes)
        String newAccessToken = jwtService.generateAccessToken(user.getEmail());

        // Generate refresh token (30 days, stored in DB)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Update last login
        user.setLastLogin(new Date());
        repo.save(user);

        // Return both tokens
        return new LoginResponse(newAccessToken, refreshToken.getToken());
    }

    public RefreshTokenResponse refreshAccessToken(String oldRefreshToken) {

        // 1. Validate old refresh token
        RefreshToken validToken = refreshTokenService.validateRefreshToken(oldRefreshToken);

        User user = validToken.getUser();

        // 2. Rotate refresh token (revoke old + create new)
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(validToken);

        // 3. Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user.getEmail());

        // 4. Return both tokens
        return new RefreshTokenResponse(newAccessToken, newRefreshToken.getToken());
    }

    public void markUserVerified(String email) {
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setVerified(true);
        repo.save(user);
        walletService.createWalletForUser(user.getId());

    }
    public String forgotPassword(String email) {
        if (!repo.existsByEmail(email)) {
            throw new RuntimeException("Email not found");
        }

        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        otpService.generateAndSendOtp(email, user.getName());

        return "OTP sent to your email for password reset.";
    }
    public String verifyResetOtp(String email, String otp) {
        boolean isValid = otpService.verifyOtp(email, otp);

        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        return "OTP verified. You can now reset your password.";
    }
    public String resetPassword(String email, String newPassword, String confirmPassword) {

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Passwords do not match");
        }

        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        if (!newPassword.matches(passwordRegex)) {
            throw new RuntimeException("Password does not meet security requirements");
        }

        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);

        repo.save(user);

        return "Password reset successful. You can now log in.";
    }
    public String verifyEmailOtp(String email, String otp) {

        boolean isValid = otpService.verifyOtp(email, otp);

        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Mark user verified + create wallet
        markUserVerified(email);

        return "Email verified successfully. You can now log in.";
    }

    public String resendOtp(String email) {

        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if (user.isVerified()) {
            throw new RuntimeException("User is already verified.");
        }

        return otpService.generateAndSendOtp(email, user.getName());
    }

    public String getUserNameByEmail(String email) {
        return repo.findByEmail(email)
                .map(User::getName)
                .orElse("User");
    }

}