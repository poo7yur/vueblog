package com.example.myApp.demos.controller;

import com.example.myApp.demos.aop.AccessLog;
import com.example.myApp.demos.dto.LoginDto;
import com.example.myApp.demos.dto.RegisterDto;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.UserService;
import com.example.myApp.demos.vo.UserVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/addUser")
    @AccessLog(module = "user manage" ,description = "add user")
    public R<String> addUser(@RequestBody RegisterDto registerDto) {
        try{
            String uid = userService.addUsr(registerDto);
            return R.ok(uid);
        } catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/login")
    @AccessLog(module = "user manage" ,description = "user login")
    public R<UserVo> login(@RequestBody LoginDto loginDto) {
        try{
            UserVo userVo =  userService.login(loginDto);
            return R.ok(userVo);
        }
        catch (Exception e){
            return R.fail(e.getMessage());
        }
    }
}
