package com.legion.common.utils;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RetryUtilsTest {

    @Test
    public void testRunnableWithException() throws Exception {
        RetryUtils.retry(() -> {
            throw new Exception();
        }, new IOException(), "error occurs");
    }

    @Test
    public void testCallable() throws Exception {
        String result = RetryUtils.retry(() -> "hi", new IOException(), "error occurs");
        System.out.println(result);

        List<Boolean> results = RetryUtils.retry(() ->
                        IntStream.range(1, 10).mapToObj(i -> i % 0 == 0).collect(Collectors.toList())
                , new IOException(), "error occurs");

        results.forEach(str -> System.out.println("List : " + str));
    }

    @Test
    public void testCallableException() throws Exception {
        String result = RetryUtils.retry(() -> {
            throw new Exception();
        }, new IOException(), "error occurs");
    }
}