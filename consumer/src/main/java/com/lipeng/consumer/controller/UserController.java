package com.lipeng.consumer.controller;

import com.lipeng.common.service.UserService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Reference
    public UserService userService;

    @GetMapping("/user/{id}")
    public String get(@PathVariable Long id) {
        return userService.getUser(id);
    }

}