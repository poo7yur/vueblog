package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.LoginDto;
import com.example.myApp.demos.dto.RegisterDto;
import com.example.myApp.demos.dto.TokenDto;
import com.example.myApp.demos.vo.UserVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

public interface UserService {
    String addUsr(RegisterDto registerDto) throws Exception;

    UserVo login(LoginDto loginDto);

    TokenDto refreshToken(TokenDto dto);

    String changeAvatar(MultipartFile file, HttpServletRequest request) throws Exception;
}
