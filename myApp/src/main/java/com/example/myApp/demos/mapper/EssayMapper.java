package com.example.myApp.demos.mapper;

import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.Essay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EssayMapper {
    List<Essay> queryEssay(@Param("dto") PageDto dto ,@Param("isShare") Integer isShare);

    void updateShare(@Param("id") String id, @Param("share") Integer share);

    void delEssayById(String id);

}
