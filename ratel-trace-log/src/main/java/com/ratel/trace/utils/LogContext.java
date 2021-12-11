package com.ratel.trace.utils;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author zhangxn
 * @date 2021/12/5  21:20
 */
public class LogContext {
    /**
     * logTag ThreadLocal
     * 使用静态变量，可以解决ThreadLocal潜在的内存泄露的问题。
     * 但是线程在线程池中复用时，需要显示的调用ThreadLocal的remove()方法。
     * 不然会使用上一次线程运行时设置的logTag，而有脏数据问题，
     * 尤其是上一次RequestId继续在使用，会无法识别具体的一次调用链，影响定位问题
     */
    private static TransmittableThreadLocal<Map<String, String>> LOG_THREAD_LOCAL = new TransmittableThreadLocal<Map<String, String>>() {

        /**
         * 初始化数据
         * @return
         */
        @Override
        protected Map<String, String> initialValue() {
            return new LinkedHashMap<>();
        }

        /**
         * 作用：用于定制 任务提交给线程池时 的ThreadLocal值传递到 任务执行时 的拷贝行为，缺省传递的是引用.
         * 为解决logTags在多个线程中共用有线程安全问题，需要进行手动值拷贝
         * 注意：只需要拷贝值即可，TTL会在restore这一步解决引用的问题，不会存在ThreadLocal内存泄露的问题
         * @param parentLogTags
         * @return
         */
        @Override
        public Map<String, String> copy(Map<String, String> parentLogTags) {
            Map<String, String> currentLogTags = new LinkedHashMap<>();
            if(parentLogTags != null) {
                // 从调用方中获取flowNo
                currentLogTags.put(LogTool.LOG_REQUEST_ID, parentLogTags.get(LogTool.LOG_REQUEST_ID));
            }
            return currentLogTags;
        }
    };

    /**
     * 获取当前线程的logTag
     * (如果线程没有设置，会得到初始值)
     */
    public static Map<String, String> get() {
        return LOG_THREAD_LOCAL.get();
    }

    /**
     * 设置当前线程的logTag
     * (如果原来有值，会覆盖)
     */
    public static void set(Map<String, String> logTags) {
        LOG_THREAD_LOCAL.set(logTags);
    }

    /**
     * 删除当前线程与 logTagThreadLocal 的引用关系
     * (在线程执行完或者线程不需要logTag中的信息，要执行该方法，防止内存泄露和产生脏数据)
     */
    public static void remove() {
        LOG_THREAD_LOCAL.remove();
    }
}
