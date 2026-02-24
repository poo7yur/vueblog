package com.example.myApp.demos.controller;

import com.example.myApp.demos.Constants;
import com.example.myApp.demos.aop.AccessLog;
import com.example.myApp.demos.dto.*;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.UserService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.MyFollowUser;
import com.example.myApp.demos.vo.UserVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/addUser")
    @AccessLog(module = "user manage", description = "add user")
    public R<String> addUser(@RequestBody RegisterDto registerDto) {
        try {
            String uid = userService.addUsr(registerDto);
            return R.ok(uid);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/login")
    @AccessLog(module = "user manage", description = "user login")
    public R<UserVo> login(@RequestBody LoginDto loginDto) {
        try {
            UserVo userVo = userService.login(loginDto);
            return R.ok(userVo);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/updateUser")
    @AccessLog(module = "user manage", description = "update user")
    public R<String> updateUser(@RequestBody RegisterDto dto, HttpServletRequest request) {
        try {
            String un = JwtUtil.parseUser(request);
            if (StringUtils.isEmpty(un)) throw new RuntimeException(Constants.INVALID_TOKEN);
            return R.ok(userService.updateUser(dto, un));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/userDetail")
    public R<UserVo> userDetail(HttpServletRequest request) {
        try {
            String uid = JwtUtil.parseUid(request);
            return R.ok(userService.userDetail(uid));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/listUser")
    public R<List<UserVo>> listUser(@RequestParam(required = false) String username) {
        try {
            return R.ok(userService.listUser(username));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/changeAvatar")
    public R<String> changeAvatar(MultipartFile file, HttpServletRequest request) {
        try {
            String msg = userService.changeAvatar(file, request);
            return R.ok(msg);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/action")
    public R<String> action(@RequestBody ActDto dto, HttpServletRequest request) {
        try {
            String currentUser = JwtUtil.parseUid(request);
            dto.setFromUser(currentUser);
            String msg = userService.action(dto);
            return R.ok(msg);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/followInfo")
    public R<List<MyFollowUser>> followInfo(@RequestParam String state, HttpServletRequest request) {
        try {
            String userId = JwtUtil.parseUid(request);
            if (StringUtils.isEmpty(userId)) throw new RuntimeException(Constants.TOKEN_EXPIRED);
            return R.ok(userService.followInfo(userId, state));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/checkPayResult")
    public R<String> checkPayResult(@RequestBody PayDto dto) {
        try {
            return R.ok(userService.checkPayResult(dto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/generatePayQrCode")
    public R<String> generatePayQrCode(@RequestBody PayDto dto) {
        try {
            return R.ok(userService.generatePayQrCode(dto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/refreshToken")
    @AccessLog(module = "user manage", description = "refresh token")
    public R<TokenDto> refreshToken(@RequestBody TokenDto dto, HttpServletRequest request) {
        try {
            dto.setCurrentToken(request.getHeader("token"));
            return R.ok(userService.refreshToken(dto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
