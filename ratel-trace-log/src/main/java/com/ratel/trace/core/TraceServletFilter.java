package com.ratel.trace.core;

import com.ratel.trace.utils.IdUtil;
import com.ratel.trace.utils.LogTool;
import com.ratel.trace.utils.StringUtil;
import com.ratel.trace.utils.TraceLogger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * @author zhangxn
 * @date 2021/12/5  22:48
 */
public class TraceServletFilter implements Filter {
    private static final TraceLogger logger = TraceLogger.getLogger(TraceServletFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String requestId = httpServletRequest.getHeader(LogTool.LOG_REQUEST_ID);
        requestId = StringUtil.isNotEmpty(requestId) ? requestId : IdUtil.simpleUUID();
        try {
            LogTool.putRequestId(requestId);
        } catch (Exception exception) {
            LogTool.removeContextLogTag();
            logger.warn("TraceServletFilter.doFilter 设置requestId失败.", exception);
        }
        try {
            chain.doFilter(request, response);
        } catch (Exception exception) {
            logger.warn("TraceServletFilter.doFilter chain.doFilter() 异常.", exception);
        } finally {
            // 要放在最后一步执行，关键步骤，清空上下文中的log, 用来防止内存泄露，或者产生脏数据
            LogTool.removeContextLogTag();
        }
    }
}
