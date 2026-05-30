package com.yuno.payment.application;

/**
 * Functional interface abstracting Thread.sleep().
 * In production: Thread.sleep(ms).
 * In tests: no-op — keeps tests fast without real delays.
 */
@FunctionalInterface
public interface Sleeper {
    void sleep(long milliseconds) throws InterruptedException;
}
