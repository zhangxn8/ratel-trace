package com.ratel.constant;

/**
 * @author zhangxiangnan
 * @date 2021/10/18  22:31
 */
public class Constants {

    //搜索建议默认条数
    public static int COMPLETION_SUGGESTION_SIZE = 10;

    //非分页，默认的查询条数
    public static int DEFALT_PAGE_SIZE = 200;
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 成功标记
     */
    public static final Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    public static final Integer FAIL = 500;

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";


    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";

    /**
     * es 默认地址
     */
    public static String DEFAULT_ES_HOST = "127.0.0.1:9200";

    /**
     * SCROLL查询 2小时
     */
    public static long DEFAULT_SCROLL_TIME = 2;

    /**
     * SCROLL查询 每页默认条数
     */
    public static int DEFAULT_SCROLL_PERPAGE = 100;
    //默认百分比查询规格
    public static double[] DEFAULT_PERCSEGMENT = {50.0,95.0,99.0};

    //批量更新（新增）每批次条数
    public static int BULK_COUNT = 5000;

    //聚合查询返回最大条数
    public static int AGG_RESULT_COUNT = Integer.MAX_VALUE;

    public static long DEFAULT_PRECISION_THRESHOLD = 3000L;
}
