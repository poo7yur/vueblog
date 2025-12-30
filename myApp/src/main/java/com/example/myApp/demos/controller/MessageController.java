package com.example.myApp.demos.controller;

import com.example.myApp.demos.utils.MyLogger;
import org.springframework.web.bind.annotation.GetMapping;
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

}


