package com.ratel.service.impl;

import com.ratel.annotation.ESID;
import com.ratel.config.ElasticSearchConfig;
import com.ratel.config.ElasticsearchProperties;
import com.ratel.constant.Constants;
import com.ratel.domain.*;
import com.ratel.enums.SqlFormat;
import com.ratel.index.ElasticsearchIndex;
import com.ratel.service.ElasticsearchService;
import com.ratel.utils.BeanTool;
import com.ratel.utils.HttpClientTool;
import com.ratel.utils.JsonUtil;
import com.ratel.utils.Tools;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.script.mustache.SearchTemplateResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhangxn
 * @date 2021/11/20  17:16
 */
@Component
public class ElasticsearchServiceImpl<T, M> implements ElasticsearchService<T, M> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ElasticsearchIndex elasticsearchIndex;

    @Autowired
    ElasticsearchProperties elasticsearchProperties;

    private static Map<Class,String> classIDMap = new ConcurrentHashMap();

    public void init(){
        this.restHighLevelClient = new ElasticSearchConfig().createInstance();
    }

    @Override
    public BulkResponse save(List<T> list) throws Exception {
        return null;
    }

    @Override
    public boolean deleteById(M id, Class<T> clazz) throws Exception {
        return false;
    }

    @Override
    public boolean save(T t) throws Exception {
        return save(t,null);
    }

    @Override
    public boolean save(T t, String routing) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(t.getClass());
        String indexname = metaData.getIndexname();
        String id = Tools.getESId(t);
        IndexRequest indexRequest = new IndexRequest(indexname);
        if (!StringUtils.isEmpty(id)) {
            indexRequest.id(id);
        }
        String source = JsonUtil.obj2String(t);
        indexRequest.source(source, XContentType.JSON);
        if(!StringUtils.isEmpty(routing)){
            indexRequest.routing(routing);
        }
        IndexResponse indexResponse = null;
        indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
            logger.info("INDEX CREATE SUCCESS");
            //异步执行rollover
            elasticsearchIndex.rollover(t.getClass(),true);
        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            logger.info("INDEX UPDATE SUCCESS");
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean saveByIndex(String data, String indexName) throws Exception {
        IndexRequest indexRequest = new IndexRequest(indexName);
        indexRequest.source(data, XContentType.JSON);
        IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
            logger.info("INDEX CREATE SUCCESS");
        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            logger.info("INDEX UPDATE SUCCESS");
        } else {
            return false;
        }
        return true;
    }

    @Override
    public BulkResponse[] saveBatch(List<T> list) throws Exception {
        if (list == null || list.size() == 0) {
            return null;
        }
        T t = list.get(0);
        MetaData metaData = elasticsearchIndex.getMetaData(t.getClass());
        String indexname = metaData.getIndexname();
        String indextype = metaData.getIndextype();
        List<List<T>> lists = Tools.splitList(list, true);
        BulkResponse[] bulkResponses = new BulkResponse[lists.size()];
        for (int i = 0; i < lists.size(); i++) {
            bulkResponses[i] = savePart(lists.get(i),indexname,indextype);
        }
        return bulkResponses;
    }

    private BulkResponse savePart(List<T> list,String indexname,String indextype) throws Exception {
        BulkRequest bulkRequest = new BulkRequest();
        for (int i = 0; i < list.size(); i++) {
            T tt = list.get(i);
            String id = Tools.getESId(tt);
            String sourceJsonStr = JsonUtil.obj2String(tt);
            bulkRequest.add(new IndexRequest(indexname).id(id)
                    .source(sourceJsonStr, XContentType.JSON));
        }
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return bulkResponse;
    }

    @Override
    public BulkResponse bulkUpdate(List<T> list) throws Exception {
        if (list == null || list.size() == 0) {
            return null;
        }
        T t = list.get(0);
        if(Tools.checkNested(t)){
            throw new Exception("nested对象更新，请使用覆盖更新");
        }
        MetaData metaData = elasticsearchIndex.getMetaData(t.getClass());
        String indexname = metaData.getIndexname();
        String indextype = metaData.getIndextype();
        return updatePart(list, indexname, indextype);
    }

    @Override
    public BulkResponse[] bulkUpdateBatch(List<T> list) throws Exception {
        if (list == null || list.size() == 0) {
            return null;
        }
        T t = list.get(0);
        if(Tools.checkNested(t)){
            throw new Exception("nested对象更新，请使用覆盖更新");
        }
        MetaData metaData = elasticsearchIndex.getMetaData(t.getClass());
        String indexname = metaData.getIndexname();
        String indextype = metaData.getIndextype();
        List<List<T>> lists = Tools.splitList(list, true);
        BulkResponse[] bulkResponses = new BulkResponse[lists.size()];
        for (int i = 0; i < lists.size(); i++) {
            bulkResponses[i] = updatePart(lists.get(i),indexname,indextype);
        }
        return bulkResponses;
    }

    private BulkResponse updatePart(List<T> list,String indexname,String indextype) throws Exception {
        BulkRequest rrr = new BulkRequest();
        for (int i = 0; i < list.size(); i++) {
            T tt = list.get(i);
            String id = Tools.getESId(tt);
            rrr.add(new UpdateRequest(indexname, id)
                    .doc(Tools.getFieldValue(tt)));
        }
        BulkResponse bulkResponse = restHighLevelClient.bulk(rrr, RequestOptions.DEFAULT);
        return bulkResponse;
    }

    @Override
    public boolean update(T t) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(t.getClass());
        String indexname = metaData.getIndexname();
        String indextype = metaData.getIndextype();
        String id = Tools.getESId(t);
        if (StringUtils.isEmpty(id)) {
            throw new Exception("ID cannot be empty");
        }
        if(Tools.checkNested(t)){
            throw new Exception("nested对象更新，请使用覆盖更新");
        }
        UpdateRequest updateRequest = new UpdateRequest(indexname, indextype, id);
        updateRequest.doc(Tools.getFieldValue(t));
        UpdateResponse updateResponse = null;
        updateResponse = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
            logger.info("INDEX CREATE SUCCESS");
        } else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            logger.info("INDEX UPDATE SUCCESS");
        } else {
            return false;
        }
        return true;
    }

    @Override
    public BulkResponse batchUpdate(QueryBuilder queryBuilder, T t, Class clazz, int limitcount, boolean asyn) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(t.getClass());
        String indexname = metaData.getIndexname();
        if (queryBuilder == null) {
            throw new NullPointerException();
        }
        if(Tools.checkNested(t)){
            throw new Exception("nested对象更新，请使用覆盖更新");
        }
        if(Tools.getESId(t) == null || "".equals(Tools.getESId(t))) {
            PageSortHighLight psh = new PageSortHighLight(1, limitcount);
            psh.setHighLight(null);
            PageList pageList = this.search(queryBuilder, psh, clazz, indexname);
            if (pageList.getTotalElements() > limitcount) {
                throw new Exception("beyond the limitcount");
            }
            if (asyn) {
                new Thread(() -> {
                    try {
                        batchUpdate(pageList.getList(), indexname, t);
                        logger.info("asyn batch finished update");
                    } catch (Exception e) {
                        logger.error("asyn batch update fail", e);
                    }
                }).start();
                return null;
            } else {
                return batchUpdate(pageList.getList(), indexname, t);
            }
        }else{
            throw new Exception("批量更新请不要给主键传值");
        }
    }

    private BulkResponse batchUpdate(List<T> list, String indexname,T tot) throws Exception {
        Map map = Tools.getFieldValue(tot);
        BulkRequest rrr = new BulkRequest();
        for (int i = 0; i < list.size(); i++) {
            T tt = list.get(i);
            rrr.add(new UpdateRequest(indexname, Tools.getESId(tt))
                    .doc(map));
        }
        BulkResponse bulkResponse = restHighLevelClient.bulk(rrr, RequestOptions.DEFAULT);
        return bulkResponse;
    }


    @Override
    public boolean updateCover(T t) throws Exception {
        return save(t);
    }

    @Override
    public boolean delete(T t) throws Exception {
        return delete(t,null);
    }

    @Override
    public boolean delete(T t, String routing) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(t.getClass());
        String indexname = metaData.getIndexname();
        String id = Tools.getESId(t);
        if (StringUtils.isEmpty(id)) {
            throw new Exception("ID cannot be empty");
        }
        DeleteRequest deleteRequest = new DeleteRequest(indexname, id);
        if(!StringUtils.isEmpty(routing)){
            deleteRequest.routing(routing);
        }
        DeleteResponse deleteResponse = null;
        deleteResponse = restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        if (deleteResponse.getResult() == DocWriteResponse.Result.DELETED) {
            logger.info("INDEX DELETE SUCCESS");
        } else {
            return false;
        }
        return true;
    }

    @Override
    public BulkByScrollResponse deleteByCondition(QueryBuilder queryBuilder, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String[] indexname = metaData.getSearchIndexNames();
        DeleteByQueryRequest request = new DeleteByQueryRequest(indexname);
        request.setQuery(queryBuilder);
        BulkByScrollResponse bulkResponse = restHighLevelClient.deleteByQuery(request, RequestOptions.DEFAULT);
        return bulkResponse;
    }


    @Override
    public SearchResponse search(SearchRequest searchRequest) throws IOException {
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return searchResponse;
    }

    @Override
    public List<T> search(QueryBuilder queryBuilder, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String[] indexname = metaData.getSearchIndexNames();
        return search(queryBuilder, clazz, indexname);
    }

    @Override
    public List<T> search(QueryBuilder queryBuilder, Class<T> clazz, String... indexs) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        List<T> list = new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest(indexs);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(Constants.DEFALT_PAGE_SIZE);
        searchRequest.source(searchSourceBuilder);
        if (metaData.isPrintLog()) {
            logger.info(searchSourceBuilder.toString());
        }
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            T t = JsonUtil.string2Obj(hit.getSourceAsString(), clazz);
            //将_id字段重新赋值给@ESID注解的字段
            correctID(clazz, t, (M)hit.getId());
            list.add(t);
        }
        return list;
    }

    @Override
    public List<T> searchMore(QueryBuilder queryBuilder,int limitSize, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String[] indexname = metaData.getSearchIndexNames();
        return searchMore(queryBuilder,limitSize,clazz,indexname);
    }

    @Override
    public List<T> searchMore(QueryBuilder queryBuilder,int limitSize, Class<T> clazz, String... indexs) throws Exception {
        PageSortHighLight pageSortHighLight = new PageSortHighLight(1, limitSize);
        PageList pageList = search(queryBuilder, pageSortHighLight, clazz, indexs);
        if(pageList != null){
            return pageList.getList();
        }
        return null;
    }

    @Override
    public List<T> searchUri(String uri, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String indexname = metaData.getIndexname();
        String indextype = metaData.getIndextype();
        List<T> list = new ArrayList<>();
        Request request = new Request("GET","/"+indexname+"/"+indextype+"/_search/?"+uri);
        Response response = request(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        if (metaData.isPrintLog()) {
            logger.info("searchUri请求报文："+"/"+indexname+"/"+indextype+"/_search/?"+uri);
            logger.info("searchUri返回报文："+responseBody);
        }
        UriResponse uriResponse = JsonUtil.string2Obj(responseBody, UriResponse.class);

        T[] ts = (T[]) Array.newInstance(clazz, uriResponse.getHits().getHits().size());
        for (int i = 0; i < uriResponse.getHits().getHits().size(); i++) {
            T t = (T) clazz.newInstance();
            //先将LinkedHashMap（json解析后是Map类型）转化成Object
            Object obj = BeanTool.mapToObject((Map) uriResponse.getHits().getHits().get(i).get_source(),clazz);
            //将Object属性拷贝
            BeanUtils.copyProperties(obj, t);
            //将_id字段重新赋值给@ESID注解的字段
            correctID(clazz, t, (M)uriResponse.getHits().getHits().get(i).get_id());
            ts[i] = t;
        }
        return Arrays.asList(ts);
    }

    @Override
    public String queryBySQL(String sql, SqlFormat sqlFormat) throws Exception {
        String host = elasticsearchProperties.getHost();
        if(StringUtils.isEmpty(host)){
            host = Constants.DEFAULT_ES_HOST;
        }
        String ipport = "";
        String[] hosts = host.split(",");
        if(hosts.length == 1){
            ipport = hosts[0];
        }else{//随机选择配置的地址
            int randomindex = new Random().nextInt(hosts.length);
            ipport = hosts[randomindex];
        }
        ipport = "http://"+ipport;
        logger.info(ipport+"/_sql?format="+sqlFormat.getFormat());
        logger.info("{\"query\":\""+sql+"\"}");

        String username = elasticsearchProperties.getUseName();
        String password = elasticsearchProperties.getPassWord();
        if(!StringUtils.isEmpty(username)) {
            return HttpClientTool.execute(ipport+"/_sql?format="+sqlFormat.getFormat(),"{\"query\":\""+sql+"\"}",username,password);
        }
        return HttpClientTool.execute(ipport+"/_sql?format="+sqlFormat.getFormat(),"{\"query\":\""+sql+"\"}");
    }


    @Override
    public long count(QueryBuilder queryBuilder, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String[] indexname = metaData.getSearchIndexNames();
        return count(queryBuilder, clazz, indexname);
    }

    @Override
    public long count(QueryBuilder queryBuilder, Class<T> clazz, String... indexs) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        CountRequest countRequest = new CountRequest(indexs);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        countRequest.source(searchSourceBuilder);
        CountResponse countResponse = restHighLevelClient.count(countRequest, RequestOptions.DEFAULT);
        long count = countResponse.getCount();
        return count;
    }

    @Override
    public PageList<T> search(QueryBuilder queryBuilder, PageSortHighLight pageSortHighLight, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String[] indexname = metaData.getSearchIndexNames();
        if (pageSortHighLight == null) {
            throw new NullPointerException("PageSortHighLight不能为空!");
        }
        return search(queryBuilder, pageSortHighLight, clazz, indexname);
    }

    @Override
    public PageList<T> search(QueryBuilder queryBuilder, PageSortHighLight pageSortHighLight, Class<T> clazz, String... indexs) throws Exception {
        if (pageSortHighLight == null) {
            throw new NullPointerException("PageSortHighLight不能为空!");
        }
        Attach attach = new Attach();
        attach.setPageSortHighLight(pageSortHighLight);
        return search(queryBuilder,attach,clazz,indexs);
    }

    @Override
    public PageList<T> search(QueryBuilder queryBuilder, Attach attach, Class<T> clazz, String... indexs) throws Exception {
        if (attach == null) {
            throw new NullPointerException("Attach不能为空!");
        }
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        PageList<T> pageList = new PageList<>();
        List<T> list = new ArrayList<>();
        PageSortHighLight pageSortHighLight = attach.getPageSortHighLight();
        SearchRequest searchRequest = new SearchRequest(indexs);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        boolean highLightFlag = false;
        boolean idSortFlag= false;
        if(pageSortHighLight != null) {
            //设置当前页码
            pageList.setCurrentPage(pageSortHighLight.getCurrentPage());
            pageList.setPageSize(pageSortHighLight.getPageSize());
            //分页
            if (pageSortHighLight.getPageSize() != 0) {
                //search after不可指定from
                if(!attach.isSearchAfter()) {
                    searchSourceBuilder.from((pageSortHighLight.getCurrentPage() - 1) * pageSortHighLight.getPageSize());
                }
                searchSourceBuilder.size(pageSortHighLight.getPageSize());
            }
            //排序
            if (pageSortHighLight.getSort() != null) {
                Sort sort = pageSortHighLight.getSort();
                List<Sort.Order> orders = sort.listOrders();
                for (int i = 0; i < orders.size(); i++) {
                    if(orders.get(i).getProperty().equals("_id")){
                        idSortFlag = true;
                    }
                    searchSourceBuilder.sort(new FieldSortBuilder(orders.get(i).getProperty()).order(orders.get(i).getDirection()));
                }
            }
            //高亮
            HighLight highLight = pageSortHighLight.getHighLight();
            if(highLight != null && highLight.getHighlightBuilder() != null){
                searchSourceBuilder.highlighter(highLight.getHighlightBuilder());
            }
            else if (highLight != null && highLight.getHighLightList() != null && highLight.getHighLightList().size() != 0) {
                HighlightBuilder highlightBuilder = new HighlightBuilder();
                if (!StringUtils.isEmpty(highLight.getPreTag()) && !StringUtils.isEmpty(highLight.getPostTag())) {
                    highlightBuilder.preTags(highLight.getPreTag());
                    highlightBuilder.postTags(highLight.getPostTag());
                }
                for (int i = 0; i < highLight.getHighLightList().size(); i++) {
                    highLightFlag = true;
                    // You can set fragment_size to 0 to never split any sentence.
                    //不对高亮结果进行拆分
                    highlightBuilder.field(highLight.getHighLightList().get(i), 0);
                }
                searchSourceBuilder.highlighter(highlightBuilder);
            }
        }
        //设定searchAfter
        if(attach.isSearchAfter()){
            if(pageSortHighLight == null || pageSortHighLight.getPageSize() == 0){
                searchSourceBuilder.size(10);
            }else{
                searchSourceBuilder.size(pageSortHighLight.getPageSize());
            }
            if(attach.getSortValues() != null && attach.getSortValues().length != 0) {
                searchSourceBuilder.searchAfter(attach.getSortValues());
            }
            //如果没拼_id的排序，自动添加保证排序唯一性
            if(!idSortFlag){
                Sort.Order order = new Sort.Order(SortOrder.ASC,"_id");
                pageSortHighLight.getSort().and(new Sort(order));
                searchSourceBuilder.sort(new FieldSortBuilder("_id").order(SortOrder.ASC));
            }
        }
        //TrackTotalHits设置为true，解除查询结果超出10000的限制
        if(attach.isTrackTotalHits()){
            searchSourceBuilder.trackTotalHits(attach.isTrackTotalHits());
        }

        //设定返回source
        if(attach.getExcludes()!= null || attach.getIncludes() != null){
            searchSourceBuilder.fetchSource(attach.getIncludes(),attach.getExcludes());
        }
        searchRequest.source(searchSourceBuilder);
        //设定routing
        if(!StringUtils.isEmpty(attach.getRouting())){
            searchRequest.routing(attach.getRouting());
        }
        if (metaData.isPrintLog()) {
            logger.info(searchSourceBuilder.toString());
        }
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            T t = JsonUtil.string2Obj(hit.getSourceAsString(), clazz);
            //将_id字段重新赋值给@ESID注解的字段
            correctID(clazz, t, (M)hit.getId());
            //替换高亮字段
            if (highLightFlag) {
                Map<String, HighlightField> hmap = hit.getHighlightFields();
                hmap.forEach((k, v) ->
                        {
                            try {
                                Object obj = mapToObject(hmap, clazz);
                                BeanUtils.copyProperties(obj, t, BeanTool.getNoValuePropertyNames(obj));
                            } catch (Exception e) {
                                logger.error("convert object error", e);
                            }
                        }
                );
            }
            list.add(t);
            //最后一条SearchAfter用于searchAfter
            pageList.setSortValues(hit.getSortValues());
        }
        pageList.setList(list);
        pageList.setTotalElements(hits.getTotalHits().value);
        if(pageSortHighLight != null && pageSortHighLight.getPageSize() != 0) {
            pageList.setTotalPages(getTotalPages(hits.getTotalHits().value, pageSortHighLight.getPageSize()));
        }
        return pageList;
    }

    @Override
    public T getById(M id, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String indexname = metaData.getIndexname();
        String indextype = metaData.getIndextype();
        if (StringUtils.isEmpty(id)) {
            throw new Exception("ID cannot be empty");
        }
        GetRequest getRequest = new GetRequest(indexname, indextype, id.toString());
        GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            return JsonUtil.string2Obj(getResponse.getSourceAsString(), clazz);
        }
        return null;
    }

    @Override
    public List<T> mgetById(M[] ids, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String indexname = metaData.getIndexname();
        String indextype = metaData.getIndextype();
        MultiGetRequest request = new MultiGetRequest();
        for (int i = 0; i < ids.length; i++) {
            request.add(new MultiGetRequest.Item(indexname, indextype, ids[i].toString()));
        }
        MultiGetResponse response = restHighLevelClient.mget(request, RequestOptions.DEFAULT);
        List<T> list = new ArrayList<>();
        for (int i = 0; i < response.getResponses().length; i++) {
            MultiGetItemResponse item = response.getResponses()[i];
            GetResponse getResponse = item.getResponse();
            if (getResponse.isExists()) {
                list.add(JsonUtil.string2Obj(getResponse.getSourceAsString(), clazz));
            }
        }
        return list;
    }

    @Override
    public boolean exists(M id, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String indexname = metaData.getIndexname();
        String indextype = metaData.getIndextype();
        if (StringUtils.isEmpty(id)) {
            throw new Exception("ID cannot be empty");
        }
        GetRequest getRequest = new GetRequest(indexname, indextype, id.toString());
        GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            return true;
        }
        return false;
    }

    @Override
    public List<T> searchTemplate(Map<String, Object> template_params, String templateName, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String[] indexname = metaData.getSearchIndexNames();
        SearchTemplateRequest request = new SearchTemplateRequest();
        request.setRequest(new SearchRequest(indexname));

        request.setScriptType(ScriptType.STORED);
        request.setScript(templateName);
        Map<String, Object> params = new HashMap<>();
        if(template_params != null){
            template_params.forEach((k,v) -> {
                params.put(k, v);
            });
        }
        request.setScriptParams(params);
        SearchTemplateResponse response = restHighLevelClient.searchTemplate(request, RequestOptions.DEFAULT);
        SearchResponse searchResponse = response.getResponse();
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        List<T> list = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            T t = JsonUtil.string2Obj(hit.getSourceAsString(), clazz);
            list.add(t);
        }
        return list;
    }

    @Override
    public List<T> searchTemplateBySource(Map<String, Object> template_params, String templateSource, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String indexname = metaData.getIndexname();
        SearchTemplateRequest request = new SearchTemplateRequest();
        request.setRequest(new SearchRequest(indexname));
        request.setScriptType(ScriptType.INLINE);
        request.setScript(templateSource);
        Map<String, Object> scriptParams = new HashMap<>();
        if(template_params != null){
            template_params.forEach((k,v) -> {
                scriptParams.put(k, v);
            });
        }
        request.setScriptParams(scriptParams);
        SearchTemplateResponse response = restHighLevelClient.searchTemplate(request, RequestOptions.DEFAULT);
        SearchResponse searchResponse = response.getResponse();
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        List<T> list = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            T t = JsonUtil.string2Obj(hit.getSourceAsString(), clazz);
            list.add(t);
        }
        return list;
    }

    @Override
    public Response saveTemplate(String templateName, String templateSource) throws Exception {
        Request scriptRequest = new Request("POST", "_scripts/"+templateName);
        scriptRequest.setJsonEntity(templateSource);
        Response scriptResponse = request(scriptRequest);
        return scriptResponse;
    }

    @Override
    public List<String> completionSuggest(String fieldName, String fieldValue, Class<T> clazz) throws Exception {
        MetaData metaData = elasticsearchIndex.getMetaData(clazz);
        String[] indexname = metaData.getSearchIndexNames();
        return completionSuggest(fieldName, fieldValue, clazz, indexname);
    }

    @Override
    public List<String> completionSuggest(String fieldName, String fieldValue, Class<T> clazz, String... indexs) throws Exception {
        String[] indexname = indexs;
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        CompletionSuggestionBuilder completionSuggestionBuilder = new
                CompletionSuggestionBuilder(fieldName + ".suggest");
        completionSuggestionBuilder.text(fieldValue);
        completionSuggestionBuilder.skipDuplicates(true);
        completionSuggestionBuilder.size(Constants.COMPLETION_SUGGESTION_SIZE);
        suggestBuilder.addSuggestion("suggest_" + fieldName, completionSuggestionBuilder);
        searchSourceBuilder.suggest(suggestBuilder);
        SearchRequest searchRequest = new SearchRequest(indexname);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        Suggest suggest = searchResponse.getSuggest();
        if (suggest == null) {
            return null;
        }
        CompletionSuggestion completionSuggestion = suggest.getSuggestion("suggest_" + fieldName);
        List<String> list = new ArrayList<>();
        for (CompletionSuggestion.Entry entry : completionSuggestion.getEntries()) {
            for (CompletionSuggestion.Entry.Option option : entry) {
                String suggestText = option.getText().string();
                list.add(suggestText);
            }
        }
        return list;
    }

    public Response request(Request request) throws Exception {
        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        return response;
    }

    /**
     * 将_id字段重新赋值给@ESID注解的字段
     * @param clazz
     * @param t
     * @param _id
     */
    private void correctID(Class clazz, T t, M _id){
        try{
            if(StringUtils.isEmpty(_id)){
                return;
            }
            if(classIDMap.containsKey(clazz)){
                Field field = clazz.getDeclaredField(classIDMap.get(clazz));
                field.setAccessible(true);
                //这里不支持非String类型的赋值，如果用默认的id，则id的类型一定是String类型的
                if(field.get(t) == null) {
                    field.set(t, _id);
                }
                return;
            }
            for (int i = 0; i < clazz.getDeclaredFields().length; i++) {
                Field field = clazz.getDeclaredFields()[i];
                field.setAccessible(true);
                if(field.getAnnotation(ESID.class) != null){
                    classIDMap.put(clazz,field.getName());
                    //这里不支持非String类型的赋值，如果用默认的id，则id的类型一定是String类型的
                    if(field.get(t) == null) {
                        field.set(t, _id);
                    }
                }
            }
        }catch (Exception e){
            logger.error("correctID error!",e);
        }
    }

    private Object mapToObject(Map map, Class<?> beanClass) throws Exception {
        if (map == null) {
            return null;
        }
        Object obj = beanClass.newInstance();
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (map.get(field.getName()) != null && !StringUtils.isEmpty(map.get(field.getName()))) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                    continue;
                }
                field.setAccessible(true);
                if (map.get(field.getName()) instanceof HighlightField && ((HighlightField) map.get(field.getName())).fragments().length > 0) {
                    field.set(obj, ((HighlightField) map.get(field.getName())).fragments()[0].string());
                }
            }
        }
        return obj;
    }

    private int getTotalPages(long totalHits, int pageSize) {
        return pageSize == 0 ? 1 : (int) Math.ceil((double) totalHits / (double) pageSize);
    }




}
