package com.ratel.index;

import com.ratel.domain.MappingData;
import com.ratel.domain.MetaData;

import java.util.Map;

/***
 * @description 索引结构基础方法接口
 * @author zhangxn
 * @date 2021/12/9 0:42
 */
public interface ElasticsearchIndex<T> {
    /**
     * 创建索引
     * @param clazz
     * @throws Exception
     */
    public void createIndex(Class<T> clazz) throws Exception;


    /**
     * 切换Alias写入index
     * @param clazz
     * @throws Exception
     */
    public void switchAliasWriteIndex(Class<T> clazz,String writeIndex) throws Exception;


    /**
     * 创建Alias
     * @param clazz
     * @throws Exception
     */
    public void createAlias(Class<T> clazz) throws Exception;

    /**
     * 创建索引
     * @param settings settings map信息
     * @param settingsList settings map信息（列表）
     * @param mappingJson mapping json
     * @param indexName 索引名称
     * @throws Exception
     */
    public void createIndex(Map<String,String> settings,Map<String,String[]> settingsList,String mappingJson,String indexName) throws Exception;
    /**
     * 删除索引
     * @param clazz
     * @throws Exception
     */
    public void dropIndex(Class<T> clazz) throws Exception;

    /**
     * 索引是否存在
     * @param clazz
     * @throws Exception
     */
    public boolean exists(Class<T> clazz) throws Exception;

    /**
     * 滚动索引
     * @param clazz
     * @param isAsyn 是否异步
     * @throws Exception
     */
    public void rollover(Class<T> clazz,boolean isAsyn) throws Exception;

    /**
     * 获得索引名称
     * @param clazz
     * @return
     */
    public String getIndexName(Class<T> clazz);

    /**
     * 获得MetaData配置
     * @param clazz
     * @return
     */
    public MetaData getMetaData(Class<T> clazz);
    /**
     * 获得MappingData配置
     * @param clazz
     * @return
     */
    public MappingData[] getMappingData(Class<T> clazz);
}
