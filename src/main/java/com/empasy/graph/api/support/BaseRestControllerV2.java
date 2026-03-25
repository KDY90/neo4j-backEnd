package com.empasy.graph.api.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Slf4j
public abstract class BaseRestControllerV2 {

    private static final long timeOut = 5000000L;
    
    // 비동기 처리를 위한 스레드 풀
    protected static final Executor shortTimeDbExecutor = Executors.newFixedThreadPool(20);

    protected <V> DeferredResult<V> deferShortTimeDb(Supplier<V> supplier) {
        return deferShortTimeDb(timeOut, supplier);
    }

    protected <V> DeferredResult<V> deferShortTimeDb(long timeOut, Supplier<V> supplier) {
        DeferredResult<V> dr = new DeferredResult<>(timeOut);
        CompletableFuture.runAsync(() -> {
            try {
                dr.setResult(supplier.get());
            } catch (Exception e) {
                log.error("Unhandled exception in deferShortTimeDb", e);
                dr.setErrorResult(e);
            }
        }, shortTimeDbExecutor).exceptionally(t -> {
            log.error("Unhandled exception in CompletableFuture", t);
            dr.setErrorResult(t.getCause());
            return null;
        });
        return dr;
    }
}
