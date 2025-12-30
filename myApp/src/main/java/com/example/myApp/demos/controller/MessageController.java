package com.example.myApp.demos.controller;

import com.example.myApp.demos.aop.AccessLog;
import com.example.myApp.demos.entity.OrderDto;
import com.example.myApp.demos.mq.MyLogger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class MessageController {

    @Resource
    private MyLogger myLogger;

    @GetMapping("/do")
    public String doSomething(){
        myLogger.log("INFO" ,"querySomething");
        return "ok";
    }

    @PostMapping("/order")
    @AccessLog(module = "order manage" ,description = "create order")
    public String order(@RequestBody OrderDto order){
        //todo
        return "create order done";
    }

}


