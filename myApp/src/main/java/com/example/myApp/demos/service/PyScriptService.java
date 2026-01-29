package com.example.myApp.demos.service;

import java.util.function.Consumer;

public interface PyScriptService {

    void invokeBsScript(String taskId, String url ,Consumer<String> onSuccess, Consumer<Throwable> onError);
}
