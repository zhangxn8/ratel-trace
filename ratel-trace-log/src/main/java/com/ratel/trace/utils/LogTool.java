package com.ratel.trace.utils;


import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/***
 * @日志工具
 * @author zhangxn
 * @date 2021/12/5 21:17
 */
public class LogTool {

    /**
     * ip 在第一次设值后存储在变量中，不需要频繁设值
     */
    private static String IP = null;
    /**
     * 主机名 在第一次设值后存储在变量中，不需要频繁设值
     */
    private static String HOST_NAME = null;

    public static final String LOG_REQUEST_ID = "requestId";
    private static final String LOG_TRACE_ID = "traceId";

    /** 行号 **/
    private static final String LOG_LINE_NUMBER = "lineNum";
    private static final String LOG_SERVICE_NAME = "service";
    private static final String LOG_FUNCTION_NAME = "funcName";
    public static final String LOG_INDEX_NAME = "indexName";

    private static final String LOG_TIMESTAMP = "time";
    private static final String LOG_IP = "ip";
    private static final String LOG_HOST_NAME = "hostName";

    /**
     * 获取堆栈中的类的简称
     * @param stackTraceElement
     * @return
     */
    public static String getStackTraceElementClassSimpleName(StackTraceElement stackTraceElement) {
        return stackTraceElement.getClassName().substring(stackTraceElement.getClassName().lastIndexOf(".") + 1);
    }

    /**
     * 获取全局logTag，不会存在null的返回值
     * @return
     */
    public static Map<String, String> getNotNullContextLogTag() {
        Map<String, String> logTags = LogContext.get();
        if(logTags == null) {
            logTags = new LinkedHashMap<>();
            LogContext.set(logTags);
        }

        if (null == IP || null == HOST_NAME) {
            // 获取inetAddress有一定性能消耗，不要频繁获取
            InetAddress inetAddress = IPUtil.getLocalHostLANAddress();
            if (inetAddress != null) {
                // 在第一次设值后存储在变量中，不需要频繁设值
                IP = inetAddress.getHostAddress();
                HOST_NAME = inetAddress.getHostName();
            }
        }
        logTags.put(LogTool.LOG_IP, IP);
        logTags.put(LogTool.LOG_HOST_NAME, HOST_NAME);
        return logTags;
    }

    /**
     * 设置requestId
     * @param value
     */
    public static void putRequestId(String value) {
        Map<String, String> logTags = getNotNullContextLogTag();
        logTags.put(LogTool.LOG_REQUEST_ID, value);
    }

    /**
     * 设置indexName
     * @param indexName
     */
    public static void putIndexName(String indexName) {
        Map<String, String> logTags = getNotNullContextLogTag();
        logTags.put(LogTool.LOG_INDEX_NAME, indexName);
    }


    /**
     * 获取上下文中的指定key对应的value
     * @param logTagsKey
     * @return
     */
    public static String getContextLogTagValue(String logTagsKey) {
        Map<String, String> logTags = LogContext.get();
        return logTags == null ? null : logTags.get(logTagsKey);
    }

    /**
     * 移除上下文全局日志参数
     */
    public static void removeContextLogTag() {
        LogContext.remove();
    }

    /**
     * 通过内部的map生产log信息
     * @param stackTraceElement
     * @return
     */
    public static Map<String, String> genLogMsgWithInnerMap(StackTraceElement stackTraceElement) {
        String simpleClassName = getStackTraceElementClassSimpleName(stackTraceElement);
        String methodName = stackTraceElement.getMethodName();
        int lineNumber = stackTraceElement.getLineNumber();
        return genLogMsgWithInnerMapByClassMethod(simpleClassName, methodName, lineNumber);
    }

    /**
     * 通过内部的map生产log信息
     * @param className
     * @param methodName
     * @return
     */
    public static Map<String, String> genLogMsgWithInnerMapByClassMethod(String className, String methodName){
        return genLogMsgWithInnerMapByClassMethod(className, methodName, 0);
    }

    /**
     * 通过内部的map生产log信息
     * @param className
     * @param methodName
     * @param lineNumber
     * @return
     */
    public static Map<String, String> genLogMsgWithInnerMapByClassMethod(String className, String methodName, int lineNumber) {
        Map<String, String> logTags = getNotNullContextLogTag();
        // 如果上下文没有RequestId，则生成RequestId
        if(StringUtil.isEmpty(logTags.get(LogTool.LOG_REQUEST_ID))) {
            logTags.put(LogTool.LOG_REQUEST_ID, IdUtil.simpleUUID());
        }
        logTags.put(LogTool.LOG_SERVICE_NAME, className);
        logTags.put(LogTool.LOG_FUNCTION_NAME, methodName);
        logTags.put(LOG_LINE_NUMBER, String.valueOf(lineNumber));
        logTags.put(LogTool.LOG_TIMESTAMP, DateUtil.format(new Date(), DateUtil.YYYY_MM_DD_HH_MM_SS));

        // 加入SkyWalking traceId
        try {
            logTags.put(LogTool.LOG_TRACE_ID, TraceContext.traceId());
        } catch (Exception e) {
            logTags.put(LogTool.LOG_TRACE_ID, "");
        }

        return logTags;
    }
}
