package com.lipeng.provider.service;

import com.lipeng.common.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Override
    public String getUser(Long userId) {
        return String.valueOf(userId);
    }

}