package com.ratel.trace.enums;

/**
 * 日志级别
 * @author zhangxn
 * @date 2021/12/5  21:12
 */
public enum LogLevelEnum {
    /**
     * info级别
     */
    DEBUG("DEBUG"),

    /**
     * info级别
     */
    INFO("INFO"),

    /**
     * warn级别
     */
    WARN("WARN"),

    /**
     * error级别
     */
    ERROR("ERROR"),
    ;

    /**
     * 日志级别名称
     */
    private final String name;

    LogLevelEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
