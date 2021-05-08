package com.lipeng.common.utils;

public class IpTraceUtils {

    private static final ThreadLocal<String> IP = new ThreadLocal<String>();

    public static String getIp() {
        return IP.get();
    }

    public static void setIp(String traceId) {
        IP.set(traceId);
    }

    public static void clear() {
        IP.remove();
    }

}