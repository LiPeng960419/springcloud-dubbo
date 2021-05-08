package com.lipeng.common.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;

@Slf4j
@Component
@ConditionalOnBean(name = "grayConfig")
public class DubboReferenceFactory {

    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String KEY_REFERENCE_PROD = DubboReferenceFactory.class.getName() + "_key_reference_prod";
    private static final String KEY_REFERENCE_GRAY = DubboReferenceFactory.class.getName() + "_key_reference_gray";

    @Autowired
    private BasicConf basicConf;

    @Autowired
    @Qualifier("grayRegistryConfig")
    private RegistryConfig grayRegistryConfig;

    @Autowired
    @Qualifier("prodRegistryConfig")
    private RegistryConfig prodRegistryConfig;

    @Autowired
    private ConsumerConfig consumerConfig;

    @Autowired
    private ApplicationConfig applicationConfig;

    private DubboReferenceFactory() {
        log.info("DubboReferenceFactory初始化成功!");
    }

    public <T> T getDubboBean(Class<T> dubboClasss) {
        return getDubboBean(dubboClasss, DEFAULT_VERSION, null);
    }

    public <T> T getDubboBean(Class<T> dubboClasss, String dubboVersion) {
        return getDubboBean(dubboClasss, dubboVersion, null);
    }

    /**
     * 动态获取线上环境或者灰度环境的bean
     *
     * @param dubboClasss
     * @param dubboVersion
     * @param consumerConfig
     * @param <T>
     * @return
     */
    public <T> T getDubboBean(Class<T> dubboClasss, String dubboVersion, ConsumerConfig customConsumerConfig) {
        boolean gray = isGray();
        if (gray) {
            log.info("当前用户IP:{}访问灰度服务", IpTraceUtils.getIp());
        }
        // 连接注册中心配置
        RegistryConfig registryConfig = gray ? grayRegistryConfig : prodRegistryConfig;
        // 注意：ReferenceConfig为重对象，内部封装了与注册中心的连接，以及与服务提供方的连接
        // 引用远程服务
        ReferenceConfig<T> reference = new ReferenceConfig<T>();
        // 当前应用配置
        reference.setApplication(applicationConfig);
        // 消费端配置
        if (customConsumerConfig != null) {
            ConsumerConfig c = new ConsumerConfig();
            BeanUtils.copyProperties(consumerConfig, c);
            BeanUtils.copyProperties(customConsumerConfig, c);
            reference.setConsumer(c);
        } else {
            reference.setConsumer(consumerConfig);
        }
        // 多个注册中心可以用setRegistries()
        reference.setRegistry(registryConfig);
        reference.setInterface(dubboClasss);
        reference.setVersion(StringUtils.isEmpty(dubboVersion) ? DEFAULT_VERSION : dubboVersion);
        return gray ? ReferenceConfigCache.getCache(KEY_REFERENCE_GRAY).get(reference)
                : ReferenceConfigCache.getCache(KEY_REFERENCE_PROD).get(reference);
    }

    public void destoryReference(Class dubboClasss, String dubboVersion) {
        HashSet<String> ips = new HashSet<>(Arrays.asList(basicConf.getGrayPushIps().split(",")));
        boolean gray = ips.contains(IpTraceUtils.getIp());
        ReferenceConfigCache cache = gray ? ReferenceConfigCache.getCache(KEY_REFERENCE_GRAY) : ReferenceConfigCache.getCache(KEY_REFERENCE_PROD);
        ReferenceConfig reference = new ReferenceConfig();
        reference.setInterface(dubboClasss);
        reference.setVersion(dubboVersion);
        cache.destroy(reference);
    }

    public boolean isGray() {
        HashSet<String> ips = new HashSet<>(Arrays.asList(basicConf.getGrayPushIps().split(",")));
        return ips.contains(IpTraceUtils.getIp());
    }

    @Component
    @Data
    public static class BasicConf {

        @Value("${gray.grayPushIps:127.0.0.1}")
        private String grayPushIps;

    }

}