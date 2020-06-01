package com.legion.process;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.legion.core.api.X;
import com.legion.node.handler.ProcessSendMessageHandler;
import com.legion.process.conditions.Condition;
import com.legion.process.info.Choice;
import com.legion.process.state.*;
import com.legion.process.transitions.NextStateTransition;
import com.legion.process.transitions.Transition;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * StateMachineTests
 *
 * @author lance
 * 8/8/2019 10:55
 */
@Slf4j
public class StateMachineTests {

    @Test
    @Ignore
    public void run() {
        StateMachine.Builder builder = StateMachine.fromJson(json("demoSimple"));

        StateMachine machine = builder.build();
        Map<String, State> states = machine.getStates();
        Single<X.XMessage> single = Single.create(singleEmitter -> singleEmitter.onSuccess(X.XMessage.newBuilder().build()));

        StateRunner.newInstance().run(machine.getStart(), states, null, single, null)
                .subscribe(xMessage -> log.info("===>Message: {}", xMessage), throwable -> log.info("===> fail: ", throwable));
    }

    @Test
    public void flatMap() throws InterruptedException {
        Single<String> result = Single.just("One");
        result = create(result);
        result = result.flatMap(s -> Single.create(emitter -> {
            Thread.sleep(500L);
            emitter.onSuccess(s + "_"+ "Three");
        }));
        result = result.flatMap(s -> Single.create(emitter -> emitter.onSuccess(s + "_"+ "Four")));
        result = result.flatMap(s -> Single.create(emitter -> emitter.onSuccess(s + "_"+ "Five")));

        result.observeOn(Schedulers.io()).subscribe(s -> log.info("==>success: {}", s), throwable -> log.info("===Fail: ", throwable.getCause()));

        Thread.sleep(2000L);
    }

    private Single<String> create(Single<String> result){
        result = result.flatMap(s -> Single.create(emitter -> emitter.onSuccess(s + "_"+ "Two")));
        return result;
    }

    @Test
    @Ignore
    public void start(){
        StateMachine.Builder builder = StateMachine.fromJson(json("BankPayForCard"));
        StateMachine machine = builder.build();
        Map<String, State> states = machine.getStates();
        String start = machine.getStart();
        List<ProcessNext> list = Lists.newArrayList();
        handler(start, states, Maps.newHashMap(), list);
        log.info("===>states: {}", states);
        Single<ProcessNext> single = Single.just(new ProcessNext());
        for(ProcessNext next: list){
            single = single.flatMap(p -> Single.create(singleEmitter -> {
                log.info("===>{}/{}", next.getGroup(), next.getTag());
                p.setNext(p.getNext() +  " ===> " + String.format("%s/%s", next.getGroup(), next.getTag()));
                singleEmitter.onSuccess(p);
            }));
        }

        single.subscribe(processNext -> log.info("===>Result: {}", processNext.getNext()), throwable -> log.info("====>Cause: ", throwable.getCause()));
    }

    private void handler( String start, Map<String, State> processes,Map<String, Object> processParams, List<ProcessNext> list){
        State state = processes.get(start);
        switch (state.getType()) {
            case State.CHOICE:
                //handlerChoice(state, processes, processParams);
                break;
            case State.PASS:
                PassState passState = (PassState) state;
                break;
            case State.SUCCEED:
                SucceedState succeedState = (SucceedState) state;
                break;
            case State.TASK:
                ProcessNext next = handlerTask(state, processes, processParams);
                list.add(next);
                handler(next.getNext(), next.getProcesses(), next.getProcessParams(), list);
                break;
            case State.FAIL:
            default:
                FailState failState = (FailState) state;
                log.info("===>FailState: {}", failState);
        }
    }

    private ProcessNext handlerTask(State state, Map<String, State> processes, Map<String, Object> processParams) {
        TaskState taskState = (TaskState) state;
        Transition transition = taskState.getTransition();
        NextStateTransition nextStateTransition = (NextStateTransition) transition;

        ProcessNext processNext = new ProcessNext();
        processNext.setGroup(taskState.getGroup());
        processNext.setTag(taskState.getTag());
        processNext.setNext(nextStateTransition.getNextStateName());
        processNext.setProcessParams(processParams);
        processNext.setProcesses(processes);
        return processNext;
    }

    /**
     * 条件表达式
     */
    private void handlerChoice(State state, Map<String, State> processes, Map<String, Object> processParams, List<ProcessNext> list) {
        ChoiceState choiceState = (ChoiceState) state;
        final List<Choice> choices = choiceState.getChoices();

        for (Choice choice : choices) {
            Condition condition = choice.getCondition();
            if (condition.check(processParams)) {
                Transition transition = choice.getTransition();
                NextStateTransition nextStateTransition = (NextStateTransition) transition;
                return;
            }
        }
    }

    @Test
    @Ignore
    public void convert() {
        StateMachine.Builder builder = StateMachine.fromJson(json("BankPurchase"));
        log.info("===>{}", builder.toString());
    }

    /**
     * 加载配置文件
     *
     * @param jsonName jsonName
     * @return jsonContext
     */
    public static String json(String jsonName) {
        try {
            Resource resource = new ClassPathResource("process/" + jsonName + ".json", StateRunner.class.getClassLoader());
            InputStream inputStream = resource.getInputStream();
            String content = CharStreams.toString(new InputStreamReader(inputStream, Charset.forName("utf-8")));
            return content;
        } catch (IOException e) {
            log.error("===>Read process json fail: ", e);
        }

        return null;
    }

    /**
     * 控制流程临时变量
     */
    @Data
    public static class ProcessNext {
        private String tag;
        private String group;
        private String next;
        private X.XMessage message;
        private Map<String, State> processes;
        private Map<String, Object> processParams;
    }
}
