/*
 * Copyright (C) 2017. The UAPI Authors
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at the LICENSE file.
 *
 * You must gained the permission from the authors if you want to
 * use the project into a commercial product
 */

package uapi.common;

import uapi.GeneralException;

/**
 * The watcher watch (block current thread) on a specific condition until the condition is satisfied or timed out.
 */
public final class Watcher {

    private static final IntervalTime DEFAULT_TIMEOUT           = IntervalTime.parse("5s");
    private static final IntervalTime DEFAULT_POLLING_INTERVAL  = IntervalTime.parse("100ms");

    public static Watcher on(WatcherCondition condition) {
        return new Watcher(condition);
    }

    private final WatcherCondition _condition;

    private IntervalTime _timeout           = DEFAULT_TIMEOUT;
    private IntervalTime _pollingInterval   = DEFAULT_POLLING_INTERVAL;

    private Watcher(WatcherCondition condition) {
        ArgumentChecker.required(condition, "condition");
        this._condition = condition;
    }

    public Watcher timeout(String timeout) {
        this._timeout = IntervalTime.parse(timeout);
        return this;
    }

    public Watcher timeout(IntervalTime timeout) {
        ArgumentChecker.required(timeout, "timeout");
        this._timeout = timeout;
        return this;
    }

    public Watcher pollingTime(String pollingInterval) {
        this._pollingInterval = IntervalTime.parse(pollingInterval);
        return this;
    }

    public Watcher pollingTime(IntervalTime pollingInterval) {
        ArgumentChecker.required(pollingInterval, "pollingInterval");
        this._pollingInterval = pollingInterval;
        return this;
    }

    public void start() {
        long startTime = System.currentTimeMillis();
        ConditionResult result = this._condition.accept();
        if (! result.isDenied()) {
            return;
        }
        if (result.awaiting() != null) {
            doAwait(startTime, result.awaiting());
        } else {
            doPolling(startTime);
        }
    }

    private void doAwait(long startTime, IAwaiting notifier) {
        long restTime = this._timeout.milliseconds() - (System.currentTimeMillis() - startTime);
        boolean notified = notifier.await(restTime);
        ConditionResult result = null;
        if (notified) {
            result = this._condition.accept();
            if (!result.isDenied()) {
                return;
            }
        }
        check(startTime);
        if (result != null && result.awaiting() != null) {
            doAwait(startTime, result.awaiting());
        } else {
            doPolling(startTime);
        }
    }

    private void doPolling(long startTime) {
        check(startTime);
        if (this._pollingInterval.milliseconds() > 0) {
            try {
                Thread.sleep(this._pollingInterval.milliseconds());
            } catch (InterruptedException ex) {
                throw new GeneralException(ex);
            }
        }
        ConditionResult result = this._condition.accept();
        if (! result.isDenied()) {
            return;
        }
        check(startTime);
        if (result.awaiting() != null) {
            doAwait(startTime, result.awaiting());
        } else {
            doPolling(startTime);
        }
    }

    private void check(long startTime) {
        long timeout = this._timeout.milliseconds();
        if (System.currentTimeMillis() - startTime >= timeout) {
            throw new GeneralException("The watcher is timed out");
        }
    }

    public interface WatcherCondition {

        ConditionResult accept();
    }

    public static class ConditionResult {

        private final boolean _denied;
        private final IAwaiting _awaiting;

        public ConditionResult(boolean denied) {
            this._denied = denied;
            this._awaiting = null;
        }

        public ConditionResult(IAwaiting notifier) {
            ArgumentChecker.required(notifier, "notified");
            this._awaiting = notifier;
            this._denied = true;
        }

        public boolean isDenied() {
            return this._denied;
        }

        public IAwaiting awaiting() {
            return this._awaiting;
        }
    }
}
