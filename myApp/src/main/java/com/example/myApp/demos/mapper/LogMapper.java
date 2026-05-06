package com.example.myApp.demos.mapper;

import com.example.myApp.demos.entity.LogEntity;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.entity.UserChat;
import com.example.myApp.demos.vo.ChatLogVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface LogMapper {

    void saveAccessLog(@Param("log") LogEntity log);

    List<MsgEntity> getMsg(@Param("list") Set<String> ids, @Param("kw") String keyword, @Param("userId") String userId);

    Set<String> getMsgGroupIds(@Param("userId") String userId);

    void addLog(@Param("tMsg") MsgEntity msgEntity);

    void aveUserChat(@Param("uc") UserChat userChat);

    void updateChatLog(@Param("id") String chatId, @Param("res") String summaryContent, @Param("status") String answerStatus);

    ChatLogVo getChat(String tmp, String uid);

    void modifyUserChat(String taskId, String fileUrl);

}
