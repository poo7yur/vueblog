package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.LoginDto;
import com.example.myApp.demos.dto.RegisterDto;
import com.example.myApp.demos.vo.UserVo;

public interface UserService {
    String addUsr(RegisterDto registerDto);

    UserVo login(LoginDto loginDto);
}
