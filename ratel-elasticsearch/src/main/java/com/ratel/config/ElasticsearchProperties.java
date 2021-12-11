package com.ratel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zhangxn
 * @date 2021/11/20  16:23
 */

@ConfigurationProperties(prefix = "ratel.elastic")
@Component
public class ElasticsearchProperties {
    private String host;
    private String useName;
    private String passWord;
    private Boolean enabled;
    /**
     * 连接池里的最大连接数
     */
    private Integer maxConnectTotal = 30;

    /**
     * 某一个/每服务每次能并行接收的请求数量
     */
    private Integer maxConnectPerRoute = 10;

    /**
     * http clilent中从connetcion pool中获得一个connection的超时时间
     */
    private Integer connectionRequestTimeoutMillis = 2000;

    /**
     * 响应超时时间，超过此时间不再读取响应
     */
    private Integer socketTimeoutMillis = 30000;

    /**
     * 链接建立的超时时间
     */
    private Integer connectTimeoutMillis = 2000;

    /**
     * keep_alive_strategy
     */
    private Long keepAliveStrategy = -1L;


    /**
     * 索引后后缀配置
     */
    private String suffix;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUseName() {
        return useName;
    }

    public void setUseName(String useName) {
        this.useName = useName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public Integer getMaxConnectTotal() {
        return maxConnectTotal;
    }

    public void setMaxConnectTotal(Integer maxConnectTotal) {
        this.maxConnectTotal = maxConnectTotal;
    }

    public Integer getMaxConnectPerRoute() {
        return maxConnectPerRoute;
    }

    public void setMaxConnectPerRoute(Integer maxConnectPerRoute) {
        this.maxConnectPerRoute = maxConnectPerRoute;
    }

    public Integer getConnectionRequestTimeoutMillis() {
        return connectionRequestTimeoutMillis;
    }

    public void setConnectionRequestTimeoutMillis(Integer connectionRequestTimeoutMillis) {
        this.connectionRequestTimeoutMillis = connectionRequestTimeoutMillis;
    }

    public Integer getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public void setSocketTimeoutMillis(Integer socketTimeoutMillis) {
        this.socketTimeoutMillis = socketTimeoutMillis;
    }

    public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(Integer connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Long getKeepAliveStrategy() {
        return keepAliveStrategy;
    }

    public void setKeepAliveStrategy(Long keepAliveStrategy) {
        this.keepAliveStrategy = keepAliveStrategy;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
