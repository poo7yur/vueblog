package com.example.myApp.demos.mapper;

import com.example.myApp.demos.entity.LogEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LogEventMapper {

    void saveLogEvent(@Param("event") LogEvent event);

}
