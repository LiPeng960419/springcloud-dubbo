package com.lipeng.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@ConditionalOnBean(name = "grayConfig")
public class DubboReferenceUtils implements InitializingBean {

    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String KEY_REFERENCE_PROD = DubboReferenceUtils.class.getName() + "_key_reference_prod";
    private static final String KEY_REFERENCE_GRAY = DubboReferenceUtils.class.getName() + "_key_reference_gray";
    private static final String KEY_INVOKE_REFERENCE_PROD = DubboReferenceUtils.class.getName() + "_key_invoke_reference_prod";
    private static final String KEY_INVOKE_REFERENCE_GRAY = DubboReferenceUtils.class.getName() + "_key_invoke_reference_gray";

    @Autowired
    @Qualifier("grayRegistryConfig")
    private RegistryConfig grayRegistryConfig1;
    private static RegistryConfig grayRegistryConfig;

    @Autowired
    @Qualifier("prodRegistryConfig")
    private RegistryConfig prodRegistryConfig1;
    private static RegistryConfig prodRegistryConfig;

    @Autowired
    private ConsumerConfig consumerConfig1;
    private static ConsumerConfig consumerConfig;

    @Autowired
    private ApplicationConfig applicationConfig1;
    private static ApplicationConfig applicationConfig;

    public static <T> T getDubboBean(Class<T> dubboClasss) {
        return getDubboBean(null, dubboClasss, false);
    }

    public static <T> T getDubboBean(Class<T> dubboClasss, String dubboVersion) {
        return getDubboBean(dubboVersion, dubboClasss, false);
    }

    public static <T> T getGrayDubboBean(Class<T> dubboClasss) {
        return getDubboBean(null, dubboClasss, true);
    }

    public static <T> T getGrayDubboBean(Class<T> dubboClasss, String dubboVersion) {
        return getDubboBean(dubboVersion, dubboClasss, true);
    }

    /**
     * https://blog.csdn.net/DCBTB/article/details/102555612
     * 指定调用某个reference  如果想要动态获取bean 用另一个工具类
     *
     * @param dubboVersion
     * @param dubboClasss
     * @param isGray
     * @param <T>
     * @return
     */
    public static <T> T getDubboBean(String dubboVersion, Class<T> dubboClasss, boolean isGray) {
        try {
            // 连接注册中心配置
            RegistryConfig registryConfig = isGray ? grayRegistryConfig : prodRegistryConfig;
            // 注意：ReferenceConfig为重对象，内部封装了与注册中心的连接，以及与服务提供方的连接
            // 引用远程服务
            ReferenceConfig<T> reference = new ReferenceConfig<T>();
            // 当前应用配置
            reference.setApplication(applicationConfig);
            // 消费端配置
            reference.setConsumer(consumerConfig);
            // 多个注册中心可以用setRegistries()
            reference.setRegistry(registryConfig);
            reference.setInterface(dubboClasss);
            reference.setVersion(StringUtils.isEmpty(dubboVersion) ? DEFAULT_VERSION : dubboVersion);
            return isGray ? ReferenceConfigCache.getCache(KEY_REFERENCE_GRAY).get(reference)
                    : ReferenceConfigCache.getCache(KEY_REFERENCE_PROD).get(reference);
        } catch (Exception e) {
            log.error("getDubboBean error", e);
            return null;
        }
    }

    public static Object genericInvoke(Class interfaceClass, String methodName, List<Map<String, Object>> parameters) {
        return genericInvoke(interfaceClass, null, false, methodName, parameters);
    }

    public static Object genericInvoke(Class interfaceClass, String dubboVersion, boolean isGray, String methodName, List<Map<String, Object>> parameters) {
        // 用com.alibaba.dubbo.rpc.service.GenericService可以替代所有接口引用
        ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
        reference.setApplication(applicationConfig);
        reference.setConsumer(consumerConfig);
        reference.setRegistry(isGray ? grayRegistryConfig : prodRegistryConfig);
        reference.setInterface(interfaceClass); // 接口名
        reference.setVersion(StringUtils.isEmpty(dubboVersion) ? DEFAULT_VERSION : dubboVersion);
        reference.setGeneric(Boolean.TRUE.toString()); // 声明为泛化接口
        /*ReferenceConfig实例很重，封装了与注册中心的连接以及与提供者的连接，
        需要缓存，否则重复生成ReferenceConfig可能造成性能问题并且会有内存和连接泄漏。
        API方式编程时，容易忽略此问题。
        这里使用dubbo内置的简单缓存工具类进行缓存*/
        ReferenceConfigCache cache = isGray ? ReferenceConfigCache.getCache(KEY_INVOKE_REFERENCE_GRAY) : ReferenceConfigCache.getCache(KEY_INVOKE_REFERENCE_PROD);
        GenericService genericService = cache.get(reference);
        return getObject(methodName, parameters, genericService);
    }

    public static Object genericInvokeNew(Class interfaceClass, String methodName, List<Map<String, Object>> parameters) {
        return genericInvokeNew(interfaceClass, null, false, methodName, parameters);
    }

    public static Object genericInvokeNew(Class interfaceClass, String dubboVersion, boolean isGray, String methodName, List<Map<String, Object>> parameters) {
        ReferenceConfig<GenericService> reference = getReferenceConfig(interfaceClass, dubboVersion, isGray);
        GenericService genericService = reference.get();
        return getObject(methodName, parameters, genericService);
    }

    private static Object getObject(String methodName, List<Map<String, Object>> parameters, GenericService genericService) {
        int len = parameters.size();
        String[] invokeParamTyeps = new String[len];
        Object[] invokeParams = new Object[len];
        for (int i = 0; i < len; i++) {
            invokeParamTyeps[i] = String.valueOf(parameters.get(i).get("ParamType"));
            invokeParams[i] = parameters.get(i).get("Object");
        }
        return genericService.$invoke(methodName, invokeParamTyeps, invokeParams);
    }

    private static final ConcurrentMap<String, WeakReference<ReferenceConfig<GenericService>>> refConfigCache = new ConcurrentHashMap<String, WeakReference<ReferenceConfig<GenericService>>>();

    private static ReferenceConfig<GenericService> getReferenceConfig(Class interfaceClass, String version, boolean isGray) {
        String refConfigCacheKey = interfaceClass.getName() + ":" + version;
        WeakReference<ReferenceConfig<GenericService>> referenceConfigWeakReference = refConfigCache.get(refConfigCacheKey);

        if (referenceConfigWeakReference != null) {//缓存有弱引用
            ReferenceConfig<GenericService> referenceConfigFromWR = referenceConfigWeakReference.get();
            if (referenceConfigFromWR == null) {//证明没人引用自己被GC了，需要重建
                ReferenceConfig<GenericService> referenceConfig = newRefConifg(interfaceClass.getName(), version, isGray);
                refConfigCache.put(refConfigCacheKey, new WeakReference<>(referenceConfig));//放入缓存中，用弱应用hold住，不影响该有GC
                return referenceConfig;
            } else {
                return referenceConfigFromWR;
            }

        } else {//缓存没有，则创建
            ReferenceConfig<GenericService> referenceConfig = newRefConifg(interfaceClass.getName(), version, isGray);
            refConfigCache.put(refConfigCacheKey, new WeakReference<>(referenceConfig));//放入缓存中，用弱应用hold住，不影响该有GC
            return referenceConfig;
        }
    }

    private static ReferenceConfig<GenericService> newRefConifg(String interfaceClass, String version, boolean isGray) {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
        reference.setApplication(applicationConfig);
        reference.setConsumer(consumerConfig);
        reference.setRegistry(isGray ? grayRegistryConfig : prodRegistryConfig);
        reference.setInterface(interfaceClass);
        reference.setVersion(version);
        reference.setGeneric(Boolean.TRUE.toString());
        return reference;
    }

    @Override
    public void afterPropertiesSet() {
        prodRegistryConfig = prodRegistryConfig1;
        grayRegistryConfig = grayRegistryConfig1;
        consumerConfig = consumerConfig1;
        applicationConfig = applicationConfig1;
        log.info("DubboReferenceUtils初始化成功!");
    }

}