package com.sahil.project.StaySphere.controler;

import com.sahil.project.StaySphere.dto.LoginDto;
import com.sahil.project.StaySphere.dto.LoginResponseDto;
import com.sahil.project.StaySphere.dto.SignUpRequestDto;
import com.sahil.project.StaySphere.dto.UserDto;
import com.sahil.project.StaySphere.security.AuthService;
import jakarta.servlet.http.Cookie;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signUp(
            @RequestBody SignUpRequestDto signUpRequestDto) {

        UserDto userDto = authService.signUp(signUpRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginDto loginDto,
            HttpServletResponse httpServletResponse
    ){
        String[] token =  authService.login(loginDto);

        Cookie cookie = new Cookie("refreshtoken", token[1]);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(7*24*60*60);

        httpServletResponse.addCookie(cookie);

        return ResponseEntity.ok(new LoginResponseDto(token[0]));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(
            HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String refreshToken = null;

        for (Cookie cookie : cookies) {
            if ("refreshtoken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String accessToken = authService.refreshAccessToken(refreshToken);

            return ResponseEntity.ok(new LoginResponseDto(accessToken));

        } catch (io.jsonwebtoken.JwtException |
                 IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
