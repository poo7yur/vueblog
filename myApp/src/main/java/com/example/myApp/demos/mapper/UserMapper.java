package com.example.myApp.demos.mapper;

import com.example.myApp.demos.dto.ActDto;
import com.example.myApp.demos.dto.RegisterDto;
import com.example.myApp.demos.dto.UserGroup;
import com.example.myApp.demos.entity.User;
import com.example.myApp.demos.entity.UserFollow;
import com.example.myApp.demos.vo.MyFollowUser;
import com.example.myApp.demos.vo.UserVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserMapper {

    User findUser(@Param("name") String username);

    void addUser(@Param("user") User user);

    Set<String> queryNames();

    void subscribeMsg(@Param("list") List<UserGroup> list);

    void updateAvatar(String shareUrl, String userId);

    Set<String> getRoleByUid(String userId);

    Integer getFollowStatus(String fromUser, String toUser);

    void updateFollowStatus(ActDto dto);

    void saveFollow(UserFollow uf);

    void deleteFollowRelation(@Param("fromUser") String fromUser, @Param("toUser") String toUser);

    List<UserVo> listUser(@Param("un") String username);

    List<MyFollowUser> selectMyFans(String userId);

    List<MyFollowUser> selectMyFollow(String userId);

    List<MyFollowUser> selectMyBlock(String userId);

    void updateUser(RegisterDto dto);

    UserVo getUserById(String uid);


}
