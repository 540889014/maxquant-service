package com.example.crypto.events;

import com.example.crypto.enums.BacktestStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BacktestCompletionEvent extends ApplicationEvent {

    private final String backtestInstanceId;
    private final BacktestStatus finalStatus;

    public BacktestCompletionEvent(Object source, String backtestInstanceId, BacktestStatus finalStatus) {
        super(source);
        this.backtestInstanceId = backtestInstanceId;
        this.finalStatus = finalStatus;
    }
} 