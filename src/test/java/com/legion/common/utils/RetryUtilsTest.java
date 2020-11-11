package com.legion.common.utils;

import com.legion.core.utils.Digest;
import com.legion.net.entities.SyncModuleInfo;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
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


    @Test
    public void versionTest() {
        List<SyncModuleInfo> groupMatchList = new ArrayList<>();
        String targetVersion = "v5.0.0";
        groupMatchList.add(new SyncModuleInfo("sdzw", "http:/1", "v0.0.0"));
        groupMatchList.add(new SyncModuleInfo("sdzw", "http:/4", "v4.0.0"));
//        long id = System.currentTimeMillis();

        Digest.findMatchVersionGroup(groupMatchList, SyncModuleInfo::getRouteVersion, targetVersion)
                .forEach(s -> System.out.println("M : " + s.toString()));

    }

    @Test
    public void versionTest1() {
        List<SyncModuleInfo> groupMatchList = new ArrayList<>();
        String targetVersion = "v0.0.0";
//        groupMatchList.add(new SyncModuleInfo("sdzw","http:/1","v0.0.0"));
        groupMatchList.add(new SyncModuleInfo("sdzw", "http:/4", "v4.0.0"));
//        long id = System.currentTimeMillis();

        Digest.findMatchVersionGroup(groupMatchList, SyncModuleInfo::getRouteVersion, targetVersion)
                .forEach(s -> System.out.println("M : " + s.toString()));

    }
}