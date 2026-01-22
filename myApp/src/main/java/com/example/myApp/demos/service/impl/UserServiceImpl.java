package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.LoginDto;
import com.example.myApp.demos.dto.RegisterDto;
import com.example.myApp.demos.dto.TokenDto;
import com.example.myApp.demos.dto.UserGroup;
import com.example.myApp.demos.entity.User;
import com.example.myApp.demos.mapper.UserMapper;
import com.example.myApp.demos.service.FileOptService;
import com.example.myApp.demos.service.UserService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.util.Md5Util;
import com.example.myApp.demos.vo.UserVo;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;


@Service
public class UserServiceImpl implements UserService {

    @Resource
    private FileOptService fileOptService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${file.dir}")
    private String fileDir;

    private static final List<String> ALLOW_IMG =
            Arrays.asList("image/png", "image/jpg", "image/jpeg", "image/webp", "image/bmp");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addUsr(RegisterDto dto) throws Exception {
        String name = dto.getName();
        if (StringUtils.isEmpty(dto.getPassword()) || StringUtils.isEmpty(name))
            throw new RuntimeException(Constants.PWD_NAME_NOT_NULL);
        //系统保留的特殊词不能当用户名
        if ("share".equals(name) || "public".equals(name)) {
            throw new RuntimeException(Constants.USED_NAME);
        }
        //判断下名称是否被用掉
        User u = userMapper.findUser(name);
        if (!ObjectUtils.isEmpty(u)) throw new RuntimeException(Constants.USED_NAME);
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
        userMapper.subscribeMsg(setSubscribe(uid));//订阅消费
        //创建一个文件夹分配给当前用户
        String newFolderPath = fileDir + name;
        Path dir = Paths.get(newFolderPath);
        Files.createDirectories(dir);
        return uid;
    }

    private List<UserGroup> setSubscribe(String uid) {
        List<UserGroup> list = new ArrayList<>();
        list.add(new UserGroup(uid, Constants.IMAGE_LIKE_CONSUMER_GROUP));
        list.add(new UserGroup(uid, Constants.COMMENT_CONSUMER_GROUP));
        return list;
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
        stringRedisTemplate.opsForValue().set(user.getUserId(), token, Duration.ofMinutes(120));
        UserVo userVo = new UserVo();
        userVo.setToken(token);
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }

    @Override
    public TokenDto refreshToken(TokenDto dto) {
        String currentToken = dto.getCurrentToken();
        if (StringUtils.isEmpty(currentToken)) throw new RuntimeException(Constants.ILLEGAl_OPT);
        Claims claims = JwtUtil.parseToken(currentToken);
        // 检查 Token 是否过期
        if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
            throw new RuntimeException(Constants.TOKEN_EXPIRED);
        }

        String userId = claims.get("userId", String.class);
        String userName = claims.get("userName", String.class);
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(userName)) {
            throw new RuntimeException(Constants.INVALID_TOKEN);
        }

        User user = new User(userId, userName);
        String newToken = JwtUtil.generateToken(user);

        //更新redis 中当前用户的token
        stringRedisTemplate.opsForValue().set(userId, newToken, Duration.ofMinutes(120));
        dto.setNewJwtToken(newToken);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String changeAvatar(MultipartFile file, HttpServletRequest request) throws Exception {
        //使用minio存头像图片 返回共享一个url 存在用户表head_pic_url字段
        String userId = JwtUtil.parseUidFromToken(request);
        validate(file);
        String folder = "avatar/" + userId;
        String suffix = getSuffix(file.getOriginalFilename());
        String objName = folder + "/" + RandomUtil.randomNumbers(6) + suffix;
        String shareUrl = fileOptService.uploadFile(file, objName);
        userMapper.updateAvatar(shareUrl, userId);
        return shareUrl;
    }

    private void validate(MultipartFile file) {
        int maxSize = 600 * 1024;
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("头像不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new RuntimeException("头像大小不能超过 " + (maxSize / 1024) + " KB");
        }
        if (!ALLOW_IMG.contains(file.getContentType().toLowerCase(Locale.ROOT))) {
            throw new RuntimeException("仅支持 png、jpg、jpeg、webp、bmp 格式");
        }
    }

    private static String getSuffix(String fileName) {
        if (fileName == null) return ".jpg";
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? ".jpg" : fileName.substring(dot);
    }

}
