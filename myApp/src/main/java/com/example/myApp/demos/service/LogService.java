package com.example.myApp.demos.service;

import com.alipay.api.AlipayApiException;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.entity.UserChat;
import com.example.myApp.demos.vo.ChatLogVo;
import com.example.myApp.demos.vo.SongVo;
import com.github.pagehelper.PageInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


public interface LogService {

    PageInfo<MsgEntity> getMsg(PageDto dto , HttpServletRequest request);

    List<SongVo> listSongs(String uid);

    String alipayNotify(Map<String, String> paramsMap) throws AlipayApiException;

    void saveUserChat(UserChat userChat);

    void updateChatLog(String chatId, String summaryContent, String answerStatus);

    ChatLogVo getChat(String tmp, String uid);

}
