package com.example.myApp.demos.mapper;

import com.example.myApp.demos.entity.LogEntity;
import com.example.myApp.demos.entity.LogEvent;
import com.example.myApp.demos.entity.MsgEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface LogEventMapper {

    void saveLogEvent(@Param("event") LogEvent event);

    void saveAccessLog(@Param("log") LogEntity log);

    List<MsgEntity> getMsg(@Param("list") Set<String> ids);

    Set<String> getMsgGroupIds(@Param("userId") String userId);

}
