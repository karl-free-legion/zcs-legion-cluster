package com.legion.process;

import com.google.common.collect.Maps;
import com.legion.common.utils.CacheUtils;
import com.legion.core.api.X;
import com.legion.core.exception.LegionException;
import com.legion.node.handler.ProcessSendMessageHandler;
import com.legion.process.conditions.Condition;
import com.legion.process.info.Choice;
import com.legion.process.state.ChoiceState;
import com.legion.process.state.State;
import com.legion.process.state.TaskState;
import com.legion.process.transitions.NextStateTransition;
import com.legion.process.transitions.Transition;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 任务开始执行
 * 1.修改process中流程控制(主要条件表达式控制流程)
 *
 * @author lance
 * 8/8/2019 10:48
 */
@Slf4j
public class StateRunner {
    /**
     * 缓存流程定义
     */
    public static final Map<String, String> CACHE_PROCESS_DEFINE = Maps.newConcurrentMap();

    private static class StateRunnerHolder {
        public static StateRunner instance = new StateRunner();
    }

    private StateRunner() {
    }

    public static StateRunner newInstance() {
        return StateRunnerHolder.instance;
    }

    /**
     * process.run
     */
    public Single<X.XMessage> run(String nextState, Map<String, State> processes, Map<String, Object> map, Single<X.XMessage> single, ProcessSendMessageHandler processor) {
        State state = processes.get(nextState);
        switch (state.getType()) {
            case State.CHOICE:
                return handlerChoice(state, processes, map, single, processor);
            case State.TASK:
                single = handlerTask(state, processes, map, single, processor);
                return single;
            case State.PASS:
            case State.SUCCEED:
            case State.FAIL:
            default:
                break;
        }

        return single;
    }

    /**
     * 条件表达式
     */
    private Single<X.XMessage> handlerChoice(State state, Map<String, State> processes, Map<String, Object> map, Single<X.XMessage> single, ProcessSendMessageHandler processor) {
        Single<ProcessNext> result = single.flatMap(message -> Single.create(emitter -> {
            //处理异常代码
            if (isReturn(message, emitter)) {
                return;
            }

            if (!message.getRpl().getStatusMap().isEmpty()) {
                map.putAll(message.getRpl().getStatusMap());
            }

            if (message.getHeader().getRequest() != null && message.getHeader().getRequest().getHeadersCount() > 0) {
                map.putAll(message.getHeader().getRequest().getHeadersMap());
            }

            ChoiceState choiceState = (ChoiceState) state;
            final List<Choice> choices = choiceState.getChoices();

            for (Choice choice : choices) {
                Condition condition = choice.getCondition();
                if (condition.check(map)) {
                    Transition transition = choice.getTransition();
                    NextStateTransition nextStateTransition = (NextStateTransition) transition;
                    map.put("processNextStep", nextStateTransition.getNextStateName());
                    emitter.onSuccess(new ProcessNext(nextStateTransition.getNextStateName(), message));
                    return;
                }
            }

            emitter.onSuccess(new ProcessNext(choiceState.getDefaultStateName(), message));
        }));

        return result.flatMap(next -> run(map.get("processNextStep").toString(), processes, map, Single.just(next.getMessage()), processor));
    }

    /**
     * 流程中Task
     */
    private Single<X.XMessage> handlerTask(State state, Map<String, State> processes, Map<String, Object> map, Single<X.XMessage> single, ProcessSendMessageHandler processor) {
        single = single.flatMap(s -> Single.create(singleEmitter -> {
            TaskState taskState = (TaskState) state;
            Transition transition = taskState.getTransition();
            NextStateTransition next = (NextStateTransition) transition;

            map.put("processNextStep", next.getNextStateName());
            X.XMessage.Builder builder = s.toBuilder();
            if (!builder.getRpl().getStatusMap().isEmpty()) {
                map.putAll(builder.getRpl().getStatusMap());
            }

            if (builder.getHeader().getRequest() != null && builder.getHeader().getRequest().getHeadersCount() > 0) {
                map.putAll(builder.getHeader().getRequest().getHeadersMap());
            }

            //处理异常代码
            if (isReturn(s, singleEmitter)) {
                return;
            }

            builder.setIsReply(false);
            builder.setHeader(builder.getHeaderBuilder().setTag(taskState.getTag()).setUri("M://" + taskState.getGroup()));

            //缓存流程singleEmitter
            CacheUtils.putEmitter(builder.getHeader().getTrackId() + "_" + taskState.getTag().hashCode(), singleEmitter);
            processor.send(builder.build(), taskState.getGroup());
        }));

        return single.flatMap(message -> run(map.get("processNextStep").toString(), processes, map, Single.just(message), processor));
    }

    private boolean isReturn(X.XMessage builder, SingleEmitter emitter) {
        X.XReplyHeader header = builder.getRpl();
        if (header.getRplCode() != 0 || StringUtils.isNotBlank(header.getErrorCode())) {
            String code = StringUtils.isNoneBlank(header.getErrorCode()) ? header.getErrorCode() : header.getRplCode() + "";
            emitter.tryOnError(new LegionException(header.getRplCode(), code, header.getRplMessage()));
            return true;
        }

        return false;
    }

    /**
     * 控制流程临时变量
     */
    @Data
    @AllArgsConstructor
    private static class ProcessNext {
        private String next;
        private X.XMessage message;
    }
}
