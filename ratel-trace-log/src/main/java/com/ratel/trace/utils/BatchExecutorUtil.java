package com.ratel.trace.utils;

import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * @author zhangxn
 * @date 2021/12/6  0:51
 */
public class BatchExecutorUtil {
    public static ThreadLocal<BatchExecutorUtil> threadCache = new ThreadLocal<BatchExecutorUtil>(){
        protected BatchExecutorUtil initialValue() {
            return new BatchExecutorUtil();
        }
        public BatchExecutorUtil get() {
            BatchExecutorUtil executor = initialValue();
            executor.ready();
            return executor;
        }
    };

    protected static ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
        protected int i = 1;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("BatchExecutor-Pool-"+(i++));
            return t;
        }
    });

    public static BatchExecutorUtil get() {
        return threadCache.get();
    }

    private LinkedList<Future<Object>> futures = new LinkedList<>();

    protected void ready() {
        futures.clear();
    }

    public void execute(final Runnable task){
        Future<Object> f = pool.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                task.run();
                return task;
            }
        });
        futures.add(f);
    }

    /**
     * 最多同步，超时时间为秒
     * @param timeout （秒）
     * @throws TimeoutException
    */
    public void sync(int timeout) throws TimeoutException {
        for(Future<Object> future : futures) {
            try {
                Object obj = future.get(timeout,TimeUnit.SECONDS);
                if(obj instanceof Exception) {
                    ((Exception)obj).printStackTrace();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
