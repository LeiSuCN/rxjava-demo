package com.leisucn.test.rxjavademo;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {


        System.setProperty("rx2.buffer-size", "1");


        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        long speedOfFlowable = 1000;
        long speedOfSubcribe = 3000;

        log.debug("Application has started!!!");

        final AtomicInteger couter = new AtomicInteger(0);

        Flowable.create(
                emitter -> {
                    while (true) {
                        if (((AtomicLong) emitter).get() >= 0) {
                            emitter.onNext(String.valueOf(couter.get()));
                            log.info("Signal Next: {}", couter.getAndIncrement());
                        } else {
                            log.info("Signal Wait: {}", couter.get());
                        }

                        Thread.sleep(speedOfFlowable);
                    }
                }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new FlowableSubscriber<Object>() {

                    private AtomicInteger summary = new AtomicInteger();

                    private Subscription subscription;

                    @Override
                    public void onSubscribe(Subscription s) {
                        log.info("onSubscribe: s", s.getClass());
                        subscription = s;
                        s.request(3);
                    }

                    @Override
                    public void onNext(Object o) {
                        log.info("onNext[{}]: {}", summary.getAndIncrement(), o);
                        if (summary.get() % 3 == 0) {
                            final Subscription s = subscription;
                            scheduledExecutorService.schedule(() -> s.request(3), 5, TimeUnit.SECONDS);
                        }

                        try {
                            Thread.sleep(speedOfSubcribe);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.info("onError: {}", t);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        countDownLatch.countDown();
                        log.info("onComplete");
                    }
                });
        countDownLatch.await();
    }
}
