package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.*;
import com.example.myApp.demos.entity.User;
import com.example.myApp.demos.entity.UserFollow;
import com.example.myApp.demos.mapper.UserMapper;
import com.example.myApp.demos.service.FileOptService;
import com.example.myApp.demos.service.UserService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.util.Md5Util;
import com.example.myApp.demos.vo.MyFollowUser;
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

    public static final List<String> KEEP_WORD =
            Arrays.asList("share", "public", "avatar" ,"book");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addUsr(RegisterDto dto) throws Exception {
        String name = dto.getName();
        if (StringUtils.isEmpty(dto.getPassword()) || StringUtils.isEmpty(name))
            throw new RuntimeException(Constants.PWD_NAME_NOT_NULL);
        //系统保留的特殊词不能当用户名
        if (KEEP_WORD.contains(name))
            throw new RuntimeException(Constants.USED_NAME);
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
        String userId = JwtUtil.parseUid(request);
        if (StringUtils.isEmpty(userId)) throw new RuntimeException(Constants.INVALID_TOKEN);
        validate(file);
        String shareUrl = fileOptService.uploadAvatarFile(file, userId);
        userMapper.updateAvatar(shareUrl, userId);
        return shareUrl;
    }

    @Override
    public boolean checkAdminRole(String userId) {
        if (StringUtils.isEmpty(userId)) throw new RuntimeException(Constants.ILLEGAl_OPT);
        boolean isAdmin = false;
        Set<String> roles = userMapper.getRoleByUid(userId);
        if (roles.contains("admin")) isAdmin = true;
        return isAdmin;
    }

    @Override
    public String updateUser(RegisterDto dto, String un) {
        User user = userMapper.findUser(un);
        if (ObjectUtils.isEmpty(user)) throw new RuntimeException(Constants.USER_NOT_FIND);
        if (!StringUtils.isEmpty(dto.getPassword())) {
            String newHashPwd = Md5Util.md5(dto.getPassword() + user.getSalt());
            dto.setPassword(newHashPwd);
        }
        dto.setName(user.getName());
        userMapper.updateUser(dto);
        return "修改用户信息成功";
    }

    @Override
    public UserVo userDetail(String uid) {
        if(StringUtils.isEmpty(uid))
            throw new RuntimeException(Constants.TOKEN_EXPIRED);
        UserVo uv = userMapper.getUserById(uid);
        if(ObjectUtils.isEmpty(uv))
            throw new RuntimeException(Constants.USER_NOT_FIND);
        return uv;
    }

    @Override
    public List<UserVo> listUser(String username) {
        return userMapper.listUser(username);
    }

    @Override
    public List<MyFollowUser> followInfo(String userId, String state) {
        //state取值 0 关注我的  1 被我关注的 2被我拉黑的
        List<MyFollowUser> list;
        switch (state) {
            case "0":
                list = userMapper.selectMyFans(userId);
                break;
            case "1":
                list = userMapper.selectMyFollow(userId);
                break;
            case "2":
                list = userMapper.selectMyBlock(userId);
                break;
            default:
                throw new RuntimeException(Constants.PARAM_ERR);
        }
        return list;
    }

    @Override
    public String action(ActDto dto) {
        String action = dto.getAction();
        String fromUser = dto.getFromUser();
        String toUser = dto.getToUser();
        String msg = Constants.PARAM_ERR;
        if (StringUtils.isAnyEmpty(fromUser, toUser) || fromUser.equals(toUser)) {
            throw new RuntimeException(Constants.ILLEGAl_OPT);
        }
        if (StringUtils.isEmpty(action))
            throw new IllegalArgumentException(msg);
        //先查看下当前的状态
        Integer status = userMapper.getFollowStatus(fromUser, toUser);
        switch (action) {
            case "follow": // 关注
                return follow(fromUser, toUser, status);

            case "cancelFollow": // 取消关注
                return cancelFollow(fromUser, toUser, status);

            case "block": // 拉黑
                return block(fromUser, toUser, status);

            case "unblock": //解除拉黑
                return unblock(fromUser, toUser, status);

            default:
                throw new RuntimeException(msg);
        }
    }

    private String unblock(String fromUser, String toUser, Integer status) {
        if (status == null || status != -1) {
            throw new RuntimeException("未拉黑该用户，无法解除");
        }

        userMapper.updateFollowStatus(new ActDto(fromUser, toUser, "unblock"));
        return "解除拉黑成功";
    }

    private String block(String fromUser, String toUser, Integer status) {
        if (status != null && status == -1) {
            return "已经拉黑，无需重复操作";
        }

        if (status == null) {
            // 创建拉黑记录
            UserFollow uf = new UserFollow();
            uf.setFollowId(fromUser);
            uf.setFollowingId(toUser);
            uf.setStatus(-1);
            uf.setCreateTime(new Date());
            userMapper.saveFollow(uf);
        } else {
            userMapper.updateFollowStatus(new ActDto(fromUser, toUser, "block"));
        }
        return "拉黑成功";
    }

    private String cancelFollow(String fromUser, String toUser, Integer status) {
        if (status == null || status != 1) {
            throw new RuntimeException("未关注该用户，无法取消关注");
        }

        // 逻辑删除
        userMapper.updateFollowStatus(new ActDto(fromUser, toUser, "cancelFollow"));
        return "取消关注成功";
    }

    private String follow(String fromUser, String toUser, Integer status) {
        //检查一下是否以及被对方拉黑
        Integer reverseStatus = userMapper.getFollowStatus(toUser, fromUser);
        if (reverseStatus != null && reverseStatus == -1) throw new RuntimeException("你被对方拉黑无法关注！");
        if (status != null && status == 1) {
            return "已经关注，无需重复操作";
        } else if (status != null && status == -1) {
            throw new RuntimeException("你已将对方拉黑，关注前需解除拉黑");
        }

        Date now = new Date();

        if (status == null) {
            UserFollow uf = new UserFollow();
            uf.setFollowId(fromUser);
            uf.setFollowingId(toUser);
            uf.setStatus(1);
            uf.setCreateTime(now);
            userMapper.saveFollow(uf);
        } else {
            // 之前取消过，重新关注：更新状态
            userMapper.updateFollowStatus(new ActDto(fromUser, toUser, "follow"));
        }
        return "关注成功";
    }

    private void validate(MultipartFile file) {
        int maxSize = 600 * 1024;
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("头像不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new RuntimeException("头像大小不能超过 " + (maxSize / 1024) + " KB");
        }
        if (!ALLOW_IMG.contains(Objects.requireNonNull(file.getContentType()).toLowerCase(Locale.ROOT))) {
            throw new RuntimeException("仅支持 png、jpg、jpeg、webp、bmp 格式");
        }
    }

    private static String getSuffix(String fileName) {
        if (fileName == null) return ".jpg";
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? ".jpg" : fileName.substring(dot);
    }

}
