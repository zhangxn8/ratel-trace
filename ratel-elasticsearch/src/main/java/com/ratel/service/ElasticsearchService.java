package com.ratel.service;

import com.ratel.domain.Attach;
import com.ratel.domain.PageList;
import com.ratel.domain.PageSortHighLight;
import com.ratel.enums.SqlFormat;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;

import java.util.List;
import java.util.Map;

/**
 * @author zhangxn
 * @date 2021/11/20  16:59
 */
public interface ElasticsearchService<T,M> {

    /**
     * 新增索引
     * @param t 索引pojo
     */
    public boolean save(T t) throws Exception;


    /**
     * 新增索引（路由方式）
     * @param t 索引pojo
     * @param routing 路由信息（默认路由为索引数据_id）
     * @return
     * @throws Exception
     */
    public boolean save(T t,String routing) throws Exception;

    /**
     *通用　ｊｓｏｎ对象
     */
    public boolean saveByIndex(String data, String indexName) throws Exception;

    /**
     * 新增索引集合
     * @param list 索引pojo集合
     */
    public BulkResponse save(List<T> list) throws Exception;

    /**
     * 新增索引集合（分批方式，提升性能，防止es服务内存溢出，每批默认5000条数据）
     * @param list 索引pojo集合
     */
    public BulkResponse[] saveBatch(List<T> list) throws Exception;

    /**
     * 更新索引集合
     * @param list 索引pojo集合
     * @return
     * @throws Exception
     */
    public BulkResponse bulkUpdate(List<T> list) throws Exception;

    /**
     * 更新索引集合（分批方式，提升性能，防止es服务内存溢出，每批默认5000条数据）
     * @param list 索引pojo集合
     * @return
     * @throws Exception
     */
    public BulkResponse[] bulkUpdateBatch(List<T> list) throws Exception;


    /**
     * 按照有值字段更新索引
     * @param t 索引pojo
     */
    public boolean update(T t) throws Exception;

    /**
     * 根据queryBuilder所查结果，按照有值字段更新索引
     *
     * @param queryBuilder 查询条件（官方）
     * @param t 索引pojo
     * @param clazz 索引pojo类类型
     * @param limitcount 更新字段不能超出limitcount
     * @param asyn true异步处理  否则同步处理
     * @return
     * @throws Exception
     */
    public BulkResponse batchUpdate(QueryBuilder queryBuilder, T t, Class clazz, int limitcount, boolean asyn) throws Exception;

    /**
     * 覆盖更新索引
     * @param t 索引pojo
     */
    public boolean updateCover(T t) throws Exception;

    /**
     * 删除索引
     * @param t 索引pojo
     */
    public boolean delete(T t) throws Exception;


    /**
     * 删除索引（路由方式）
     * @param t 索引pojo
     * @param routing 路由信息（默认路由为索引数据_id）
     * @return
     * @throws Exception
     */
    public boolean delete(T t,String routing) throws Exception;


    /**
     * 根据条件删除索引
     * @param queryBuilder 查询条件（官方）
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public BulkByScrollResponse deleteByCondition(QueryBuilder queryBuilder, Class<T> clazz) throws Exception;

    /**
     * 删除索引
     * @param id 索引主键
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public boolean deleteById(M id, Class<T> clazz) throws Exception;


    /**
     * 原生查询
     * @param searchRequest 原生查询请求对象
     * @return
     * @throws Exception
     */
    public SearchResponse search(SearchRequest searchRequest) throws Exception;

    /**
     * 非分页查询
     * @param queryBuilder 查询条件
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public List<T> search(QueryBuilder queryBuilder, Class<T> clazz) throws Exception;

    /**
     * 非分页查询(跨索引)
     * @param queryBuilder 查询条件
     * @param clazz 索引pojo类类型
     * @param indexs 索引名称
     * @return
     * @throws Exception
     */
    public List<T> search(QueryBuilder queryBuilder, Class<T> clazz,String... indexs) throws Exception;

    /**
     * 非分页查询，指定最大返回条数
     * @param queryBuilder 查询条件
     * @param limitSize 最大返回条数
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public List<T> searchMore(QueryBuilder queryBuilder,int limitSize, Class<T> clazz) throws Exception;

    /**
     * 非分页查询(跨索引)，指定最大返回条数
     * @param queryBuilder 查询条件
     * @param limitSize 最大返回条数
     * @param clazz 索引pojo类类型
     * @param indexs 索引名称
     * @return
     * @throws Exception
     */
    public List<T> searchMore(QueryBuilder queryBuilder,int limitSize, Class<T> clazz,String... indexs) throws Exception;


    /**
     * 通过uri querystring进行查询
     * @param uri uri查询的查询条件
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public List<T> searchUri(String uri,Class<T> clazz)throws Exception;


    /**
     * 通过sql进行查询
     * @param sql sql脚本（支持mysql语法）
     * @param sqlFormat sql请求返回类型
     * @return
     * @throws Exception
     */
    public String queryBySQL(String sql, SqlFormat sqlFormat) throws Exception;

    /**
     * 查询数量
     * @param queryBuilder 查询条件
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public long count(QueryBuilder queryBuilder, Class<T> clazz) throws Exception;


    /**
     * 查询数量(跨索引)
     * @param queryBuilder 查询条件
     * @param clazz 索引pojo类类型
     * @param indexs 索引名称
     * @return
     * @throws Exception
     */
    public long count(QueryBuilder queryBuilder, Class<T> clazz,String... indexs) throws Exception;

    /**
     * 支持分页、高亮、排序的查询
     * @param queryBuilder 查询条件
     * @param pageSortHighLight 分页+排序+高亮对象封装
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public PageList<T> search(QueryBuilder queryBuilder, PageSortHighLight pageSortHighLight, Class<T> clazz) throws Exception;


    /**
     * 支持分页、高亮、排序的查询（跨索引）
     * @param queryBuilder 查询条件
     * @param pageSortHighLight 分页+排序+高亮对象封装
     * @param clazz 索引pojo类类型
     * @param indexs 索引名称
     * @return
     * @throws Exception
     */
    public PageList<T> search(QueryBuilder queryBuilder, PageSortHighLight pageSortHighLight, Class<T> clazz,String... indexs) throws Exception;

    /**
     * 支持分页、高亮、排序、指定返回字段、路由的查询（跨索引）
     * @param queryBuilder 查询条件
     * @param attach 查询增强对象（可支持分页、高亮、排序、指定返回字段、路由、searchAfter信息的定制）
     * @param clazz 索引pojo类类型
     * @param indexs 索引名称
     * @return
     * @throws Exception
     */
    public PageList<T> search(QueryBuilder queryBuilder, Attach attach, Class<T> clazz,String... indexs) throws Exception;

    /**
     * Template方式搜索，Template已经保存在script目录下
     * @param template_params 模版参数
     * @param templateName 模版名称
     * @param clazz 索引pojo类类型
     * @return
     */
    public List<T> searchTemplate(Map<String, Object> template_params, String templateName, Class<T> clazz) throws Exception;

    /**
     * Template方式搜索，Template内容以参数方式传入
     * @param template_params 模版参数
     * @param templateSource 模版内容
     * @param clazz 索引pojo类类型
     * @return
     */
    public List<T> searchTemplateBySource(Map<String, Object> template_params,String templateSource,Class<T> clazz) throws Exception;

    /**
     * 保存Template
     * @param templateName 模版名称
     * @param templateSource 模版内容
     * @return
     */
    public Response saveTemplate(String templateName,String templateSource) throws Exception;

    /**
     * 搜索建议Completion Suggester
     * @param fieldName 搜索建议对应查询字段
     * @param fieldValue 搜索建议查询条件
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public List<String> completionSuggest(String fieldName,String fieldValue,Class<T> clazz) throws Exception;


    /**
     * 搜索建议Completion Suggester
     * @param fieldName 搜索建议对应查询字段
     * @param fieldValue 搜索建议查询条件
     * @param clazz 索引pojo类类型
     * @param indexs 索引名称
     * @return
     * @throws Exception
     */
    public List<String> completionSuggest(String fieldName,String fieldValue,Class<T> clazz,String... indexs) throws Exception;


    /**
     * 根据ID查询
     * @param id 索引数据id值
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public T getById(M id, Class<T> clazz) throws Exception;

    /**
     * 根据ID列表批量查询
     * @param ids 索引数据id值数组
     * @param clazz 索引pojo类类型
     * @return
     * @throws Exception
     */
    public List<T> mgetById(M[] ids, Class<T> clazz) throws Exception;

    /**
     * id数据是否存在
     * @param id 索引数据id值
     * @param clazz 索引pojo类类型
     * @return
     */
    public boolean exists(M id,Class<T> clazz) throws Exception;

}
