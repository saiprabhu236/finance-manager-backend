package com.sai.finance.finance_manager.controller;

import com.sai.finance.finance_manager.dto.VerifyOtpRequest;
import com.sai.finance.finance_manager.service.OtpService;
import com.sai.finance.finance_manager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    // Send OTP
    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {

        String name = userService.getUserNameByEmail(email);

        String result = otpService.generateAndSendOtp(email, name);

        return ResponseEntity.ok(Map.of("message", result));
    }

    // Verify OTP
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {

        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid or expired OTP"));
        }

        userService.markUserVerified(request.getEmail());

        return ResponseEntity.ok(
                Map.of("message", "OTP verified successfully. You can now log in.")
        );
    }

    // Resend OTP
    @PostMapping("/resend")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {

        String name = userService.getUserNameByEmail(email);

        String result = otpService.generateAndSendOtp(email, name);

        return ResponseEntity.ok(Map.of("message", result));
    }
}
