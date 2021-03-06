package com.cloud.utils.concurrency;

import java.util.Date;

public interface Scheduler {

    /**
     * This is called from the TimerTask thread periodically about every one minute.
     */
    void poll(Date currentTimestamp);
}
