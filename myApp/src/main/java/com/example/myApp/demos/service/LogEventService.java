package com.example.myApp.demos.service;

import com.example.myApp.demos.entity.LogEvent;
import org.springframework.stereotype.Service;

@Service
public interface LogEventService {
    void saveLogEvent(LogEvent event);

}
