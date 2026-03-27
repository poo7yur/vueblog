package com.example.myApp.demos.mapper;

import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.Essay;
import com.example.myApp.demos.entity.UserRssRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EssayMapper {
    List<Essay> queryEssay(@Param("dto") PageDto dto ,@Param("uid") String uid);

    void updateShare(@Param("id") String id, @Param("share") Integer share);

    void delEssayById(String id);

    void delEssayByName(@Param("t") String name ,@Param("u") String userId);

    void createEssay(@Param("e") Essay essay);

    void updateEssay(Essay essay);

    int addRss(UserRssRel dto);

    void batchSaveEssay(@Param("es") List<Essay> essays);

    List<UserRssRel> getUserRssJob();

}
