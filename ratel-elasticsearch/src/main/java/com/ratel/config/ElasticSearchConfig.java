package com.ratel.config;

import com.ratel.constant.Constants;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author zhangxn
 * @date 2021/11/20  16:32
 */
@Configuration
@ConditionalOnProperty(name = "ratel.elastic.enabled", matchIfMissing = true)
public class ElasticSearchConfig {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ElasticsearchProperties elasticsearchProperties;

    private RestHighLevelClient restHighLevelClient;

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchProperties elasticsearchProperties() {
        return new ElasticsearchProperties();
    }


    @Bean
    public RestHighLevelClient createInstance() {
        String host = elasticsearchProperties.getHost();
        String username = elasticsearchProperties.getUseName();
        String password = elasticsearchProperties.getPassWord();
        Integer maxConnectTotal = elasticsearchProperties.getMaxConnectTotal();
        Integer maxConnectPerRoute = elasticsearchProperties.getMaxConnectPerRoute();
        Integer connectionRequestTimeoutMillis = elasticsearchProperties.getConnectionRequestTimeoutMillis();
        Integer socketTimeoutMillis = elasticsearchProperties.getSocketTimeoutMillis();
        Integer connectTimeoutMillis = elasticsearchProperties.getConnectTimeoutMillis();
        Long strategy = elasticsearchProperties.getKeepAliveStrategy();
        try {
            if (StringUtils.isEmpty(host)) {
                host = Constants.DEFAULT_ES_HOST;
            }
            String[] hosts = host.split(",");
            HttpHost[] httpHosts = new HttpHost[hosts.length];
            // 集群地址解析
            for (int i = 0; i < httpHosts.length; i++) {
                String h = hosts[i];
                httpHosts[i] = new HttpHost(h.split(":")[0], Integer.parseInt(h.split(":")[1]), "http");
            }
            RestClientBuilder builder = RestClient.builder(httpHosts);
            builder.setRequestConfigCallback(requestConfigBuilder -> {
                requestConfigBuilder.setConnectTimeout(connectTimeoutMillis);
                requestConfigBuilder.setSocketTimeout(socketTimeoutMillis);
                requestConfigBuilder.setConnectionRequestTimeout(connectionRequestTimeoutMillis);
                return requestConfigBuilder;
            });
            // 用户密码认证
            if (!StringUtils.isEmpty(username)) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));  //es账号密码（默认用户名为elastic）

                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.disableAuthCaching();
                    httpClientBuilder.setMaxConnTotal(maxConnectTotal);
                    httpClientBuilder.setMaxConnPerRoute(maxConnectPerRoute);
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    if (strategy > 0){
                        httpClientBuilder.setKeepAliveStrategy((httpResponse, httpContext) -> strategy);
                    }
                    return httpClientBuilder;
                });
            } else {
                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.disableAuthCaching();
                    httpClientBuilder.setMaxConnTotal(maxConnectTotal);
                    httpClientBuilder.setMaxConnPerRoute(maxConnectPerRoute);
                    if (strategy > 0){
                        httpClientBuilder.setKeepAliveStrategy((httpResponse, httpContext) -> strategy);
                    }
                    return httpClientBuilder;
                });
            }
            //初始化客户端
            restHighLevelClient = new RestHighLevelClient(builder);
        } catch (Exception e) {
            logger.error("create RestHighLevelClient error", e);
            return null;
        }
        return restHighLevelClient;
    }
}
