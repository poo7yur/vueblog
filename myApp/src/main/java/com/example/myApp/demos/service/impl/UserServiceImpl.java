package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.LoginDto;
import com.example.myApp.demos.dto.RegisterDto;
import com.example.myApp.demos.entity.User;
import com.example.myApp.demos.mapper.UserMapper;
import com.example.myApp.demos.service.UserService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.util.Md5Util;
import com.example.myApp.demos.vo.UserVo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;


@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String addUsr(RegisterDto dto) {
        if (StringUtils.isEmpty(dto.getPassword()) || StringUtils.isEmpty(dto.getName()))
            throw new RuntimeException(Constants.PWD_NAME_NOT_NULL);
        //对用户密码进行加密 md5+salt
        String salt = RandomUtil.randomNumbers(8);
        String uid = RandomUtil.randomString(18);
        String newPwd = Md5Util.md5(dto.getPassword() + salt);
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setSalt(salt);
        user.setPassword(newPwd);
        user.setUserId(uid);
        userMapper.addUser(user);
        return uid;
    }

    @Override
    public UserVo login(LoginDto loginDto) {
        if (StringUtils.isEmpty(loginDto.getUsername()) || StringUtils.isEmpty(loginDto.getPassword()))
            throw new RuntimeException(Constants.PWD_NAME_NOT_NULL);
        User user = userMapper.findUser(loginDto.getUsername());
        if (ObjectUtils.isEmpty(user)) throw new RuntimeException(Constants.USER_NOT_FIND);
        String salt = user.getSalt();
        String hashPwd = Md5Util.md5(loginDto.getPassword() + salt);
        if (!hashPwd.equals(user.getPassword())) throw new RuntimeException(Constants.PWD_NAME_ERROR);
        //使用jwt生成token 存入redis并返回前端
        String token = JwtUtil.generateToken(user);
        stringRedisTemplate.opsForValue().set(token, hashPwd, Duration.ofMinutes(30));
        UserVo userVo = new UserVo();
        userVo.setToken(token);
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }
}
