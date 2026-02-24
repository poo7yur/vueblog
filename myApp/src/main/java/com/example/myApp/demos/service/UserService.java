package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.*;
import com.example.myApp.demos.vo.MyFollowUser;
import com.example.myApp.demos.vo.UserVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserService {
    String addUsr(RegisterDto registerDto) throws Exception;

    UserVo login(LoginDto loginDto);

    TokenDto refreshToken(TokenDto dto);

    String changeAvatar(MultipartFile file, HttpServletRequest request) throws Exception;

    boolean checkAdminRole(String userId);

    String action(ActDto dto);

    List<UserVo> listUser(String username);

    List<MyFollowUser> followInfo(String userId, String state);

    String updateUser(RegisterDto dto ,String un);

    UserVo userDetail(String uid);

    String generatePayQrCode(PayDto dto) throws Exception;

    String checkPayResult(PayDto dto);

}
