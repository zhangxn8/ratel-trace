package com.ratel.trace.utils;

import com.alibaba.fastjson.JSON;
import com.ratel.service.ElasticsearchService;
import com.ratel.trace.enums.LogLevelEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangxn
 * @date 2021/12/5  21:16
 */
public class TraceLogger {

    private Logger logger;

    private ElasticsearchService elasticsearchService = SpringUtils.getBean(ElasticsearchService.class);

    private TraceLogger(Class<? extends Object> clz) {
        logger = LoggerFactory.getLogger(clz);
    }

    public static TraceLogger getLogger(Class<? extends Object> clz) {
        return new TraceLogger(clz);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * 根据类、方法，以及日志级别记录日志
     * @param message
     * @param className
     * @param methodName
     * @param logLevelEnum
     */
    public void logByClassMethodAndLogLevel(String message, String className, String methodName, LogLevelEnum logLevelEnum, String logType, String indexName) {
        // 如果日志级别为空，默认取info级别
        if(logLevelEnum == null) {
            logLevelEnum = LogLevelEnum.INFO;
        }

        // 根据不同日志级别打印
        String fullmsg = "";
        switch (logLevelEnum) {
            case DEBUG:
                fullmsg = debugByClassMethod(message, className, methodName);
                break;
            case INFO:
                fullmsg = infoByClassMethod(message, className, methodName);
                break;
            case WARN:
                fullmsg = warnByClassMethod(message, className, methodName);
                break;
            case ERROR:
                fullmsg = errorByClassMethod(message, className, methodName);
                break;
            default:
                break;
        }
        // 记录日志到持久化系统
        if (StringUtil.isNotEmpty(indexName)) {
            LogTool.putIndexName(indexName);
        } else {
            Map<String, String> logTags = LogTool.getNotNullContextLogTag();
            indexName = logTags.get(LogTool.LOG_INDEX_NAME);
        }
        if (StringUtil.isEmpty(indexName)) {
            logger.error("索引配置为空：无法建立数据关系");
            return;
        }
        if (StringUtil.isEmpty(logType)) {
            logType = "elasticsearch";
        }
        switch (logType) {
            case "elasticsearch":
                genLogToPersistSystem(fullmsg, indexName);
                break;
        }
    }

    protected String debugByClassMethod(String message, String className, String methodName) {
        Map<String, String> logTagMap = LogTool.genLogMsgWithInnerMapByClassMethod(className, methodName);
        String fullMessage = genLogMsgWithTags(message, logTagMap);
        logger.debug(fullMessage);
        return fullMessage;
    }

    protected String infoByClassMethod(String message, String className, String methodName) {
        Map<String, String> logTagMap = LogTool.genLogMsgWithInnerMapByClassMethod(className, methodName);
        String fullMessage = genLogMsgWithTags(message, logTagMap);
        logger.info(fullMessage);
        return fullMessage;
    }

    public String info(String message) {
        Map<String, String> logTagMap = LogTool.genLogMsgWithInnerMap(Thread.currentThread().getStackTrace()[2]);
        String fullMessage = genLogMsgWithTags(message, logTagMap);
        logger.info(fullMessage);
        return fullMessage;
    }

    public String warnByClassMethod(String message, String className, String methodName) {
        Map<String, String> logTagMap = LogTool.genLogMsgWithInnerMapByClassMethod(className, methodName);
        String fullMessage = genLogMsgWithTags(message, logTagMap);
        logger.warn(fullMessage);
        return fullMessage;
    }

    public String warn(String message, Throwable throwable) {
        Map<String, String> logTagMap = LogTool.genLogMsgWithInnerMap(Thread.currentThread().getStackTrace()[2]);
        String fullMessage = genLogMsgWithTags(message + getExceptionAllinformation(throwable), logTagMap);
        logger.warn(fullMessage);
        return fullMessage;
    }

    public String errorByClassMethod(String message, String className, String methodName) {
        Map<String, String> logTagMap = LogTool.genLogMsgWithInnerMapByClassMethod(className, methodName);
        String fullMessage = genLogMsgWithTags(message, logTagMap);
        logger.error(fullMessage);
        return fullMessage;
    }

    /**
     * 格式化的包含参数列表的message
     * 如果参数列表最后一位为throwable，会对throwable做特殊格式
     * @param message
     * @param arguments
     * @return
     */
    private static String formatArguments(String message, Object... arguments) {
        if(StringUtil.isEmpty(message) || arguments == null || arguments.length == 0) {
            return message;
        }
        FormattingTuple tp = MessageFormatter.arrayFormat(message, arguments);
        message = tp.getMessage();
        if(tp.getThrowable() != null) {
            message += getExceptionAllinformation(tp.getThrowable());
        }
        return message;
    }

    public static String getExceptionAllinformation(Throwable ex){
        if (ex == null) {
            return "";
        }

        String sOut = "\r\n" + ex.toString() + "\r\n";
        StackTraceElement[] trace = ex.getStackTrace();
        for (StackTraceElement s : trace) {
            sOut += "\tat " + s + "\r\n";
        }
        return sOut;
    }

    private String genLogMsgWithTags(String message, Map<String, String> tagMap) {
        if ( (null != tagMap) && (!tagMap.isEmpty()) ) {
            tagMap.put("msg", message == null ? "" : message.replaceAll("'", " ").replaceAll("\"", " "));
        } else {
            tagMap = new HashMap<>();
            tagMap.put("msg", message == null ? "" : message.replaceAll("'", " ").replaceAll("\"", " "));
        }

        return JSON.toJSONString(tagMap);
    }

    /**
     * 保存日志到elasticsearch
     */
    private void genLogToPersistSystem(String fullMessage, String indexName){
         BatchExecutorUtil.get().execute(new Runnable() {
             @Override
             public void run() {
                 try {
                     elasticsearchService.saveByIndex(fullMessage, indexName);
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
         });
    }
}
