package com.ratel.trace.core;

import com.ratel.trace.annotation.LogTrace;
import com.ratel.trace.enums.LogLevelEnum;
import com.ratel.trace.utils.TraceLogger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 日志切面处理类
 * @author zhangxn
 * @date 2021/12/5  21:03
 */
@Order(0)
@Component
@Aspect
public class LogAspect {

    private static TraceLogger logger = TraceLogger.getLogger(LogAspect.class);

    @Value("${ratel.trace.log.type}")
    private String logType;

    /**
     * 切入点
     */
    @Pointcut("@annotation(com.ratel.trace.annotation.LogTrace)")
    public void logTrace() {
    }

    /**
     * 方法环绕
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("logTrace()")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String simpleClassName = joinPoint.getTarget().getClass().getSimpleName();

        LogTrace logTraceAnno = method.getAnnotation(LogTrace.class);
        LogLevelEnum logLevelEnum = logTraceAnno != null ? logTraceAnno.logLevel() : null;
        boolean logParameter = logTraceAnno != null && logTraceAnno.logParameter();
        boolean logReturnValue = logTraceAnno != null && logTraceAnno.logReturnValue();
        String indexName = logTraceAnno.indexName();

        logMethodStart(joinPoint, simpleClassName, methodName, logParameter, logLevelEnum, indexName);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            // 拼装错误消息。只取exception message，不取堆栈信息，防止过多打印堆栈信息
            String warnMsg = "warning! " + simpleClassName + "." + methodName + " throws Exception! cost time: "
                    + (System.currentTimeMillis() - startTime) + "ms. exception message: " + throwable.getMessage();
            logger.logByClassMethodAndLogLevel(warnMsg, simpleClassName, methodName, logLevelEnum, logType,indexName);
            throw throwable;
        }

        String endStr = simpleClassName + "." + methodName + " end. cost time: " + (System.currentTimeMillis() - startTime) + "ms.";
        if(logReturnValue) {
            endStr += " return value: " + result;
        }

        logger.logByClassMethodAndLogLevel(endStr, simpleClassName, methodName, logLevelEnum, logType,indexName);

        // 保存到存储系统 例如mysql、pgsql、 elasticsearch等 目前支持elasticsearch
        if (StringUtils.isEmpty(logType)) {
            logType = "elasticsearch";
        }

        return result;
    }

    /**
     * 记录方法开始日志
     * @param joinPoint
     * @param simpleClassName
     * @param methodName
     * @param logParameter
     * @param logLevelEnum
     */
    private void logMethodStart(ProceedingJoinPoint joinPoint, String simpleClassName, String methodName, boolean logParameter, LogLevelEnum logLevelEnum, String indexName) {
        StringBuilder argsStr = new StringBuilder(simpleClassName + "." + methodName + " start.");

        // 如果设置不打印方法入参，则不打印入参
        if(!logParameter) {
            logger.logByClassMethodAndLogLevel(argsStr.toString(), simpleClassName, methodName, logLevelEnum, logType,indexName);
            return;
        }

        argsStr.append(" parameter: ");
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method jdkMethod = signature.getMethod();

            // 获取方法上的参数名称列表
            ParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();
            String[] parameterNames = pnd.getParameterNames(jdkMethod);
            if(parameterNames == null || parameterNames.length == 0) {
                argsStr.append("no parameter.");
            } else {
                Object[] argValues = joinPoint.getArgs();
                for (int i = 0; i < parameterNames.length; i++) {
                    argsStr.append(parameterNames[i]).append(" = [").append(argValues[i]).append("]");
                    if(i != parameterNames.length - 1) {
                        argsStr.append(", ");
                    }
                }
            }
        } catch (Exception exception) {
            logger.warn(LogAspect.class.getSimpleName() + ".logMethodStart, get parameter error! class=" + simpleClassName
                    + ", method=" + methodName, exception);
        }
        logger.logByClassMethodAndLogLevel(argsStr.toString(), simpleClassName, methodName, logLevelEnum, logType, indexName);
    }
}
