package com.example.myApp.demos.mapper;

import com.example.myApp.demos.dto.UserGroup;
import com.example.myApp.demos.entity.User;
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

}
