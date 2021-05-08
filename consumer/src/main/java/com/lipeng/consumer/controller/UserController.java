package com.lipeng.consumer.controller;

import com.google.common.collect.Lists;
import com.lipeng.common.service.UserService;
import com.lipeng.common.utils.DubboReferenceFactory;
import com.lipeng.common.utils.DubboReferenceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class UserController {

    @Autowired
    private DubboReferenceFactory factory;

    @Reference
    public UserService userService;

    @GetMapping("/user/{id}")
    public String get(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @GetMapping("/factory/gray/{id}")
    public String factory(@PathVariable Long id) {
        UserService dubboBean = factory.getDubboBean(UserService.class);
        return dubboBean.getUser(id);
    }

    @GetMapping("/utils/gray/{id}")
    public String utils(@PathVariable Long id) {
        UserService dubboBean = DubboReferenceUtils.getDubboBean(UserService.class);
        return dubboBean.getUser(id);
    }

    @GetMapping("/genericInvoke/{id}")
    public String genericInvoke(@PathVariable Long id) {
        List<Map<String, Object>> parameters = Lists.newArrayList(new HashMap() {
            private static final long serialVersionUID = -6277757708826939814L;

            {
                put("ParamType", "java.lang.Long");  //后端接口参数类型
                put("Object", id);  //用以调用后端接口的实参
            }
        });
        Object getUser = DubboReferenceUtils.genericInvoke(UserService.class, "getUser", parameters);
        return getUser.toString();
    }

    @GetMapping("/genericInvokeNew/{id}")
    public String genericInvokeNew(@PathVariable Long id) {
        List<Map<String, Object>> parameters = Lists.newArrayList(new HashMap() {
            private static final long serialVersionUID = -6277757708826939814L;

            {
                put("ParamType", "java.lang.Long");  //后端接口参数类型
                put("Object", id);  //用以调用后端接口的实参
            }
        });

        Object getUser = DubboReferenceUtils.genericInvokeNew(UserService.class, "getUser", parameters);
        return getUser.toString();
    }

}