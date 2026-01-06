package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.LoginDto;
import com.example.myApp.demos.dto.RegisterDto;
import com.example.myApp.demos.vo.UserVo;

import java.io.IOException;

public interface UserService {
    String addUsr(RegisterDto registerDto) throws IOException;

    UserVo login(LoginDto loginDto);
}
