package com.cloud.jobs;

import com.cloud.legacymodel.Identity;
import com.cloud.legacymodel.InternalIdentity;

import java.util.Date;

public interface JobInfo extends Identity, InternalIdentity {
    String getType();

    String getDispatcher();

    int getPendingSignals();

    long getUserId();

    long getAccountId();

    String getCmd();

    int getCmdVersion();

    String getCmdInfo();

    Status getStatus();

    int getProcessStatus();

    int getResultCode();

    String getResult();

    Long getInitMsid();

    Long getExecutingMsid();

    Long getCompleteMsid();

    Date getCreated();

    Date getLastUpdated();

    Date getLastPolled();

    String getInstanceType();

    Long getInstanceId();

    public enum Status {
        IN_PROGRESS(false), SUCCEEDED(true), FAILED(true), CANCELLED(true);

        private final boolean done;

        private Status(final boolean done) {
            this.done = done;
        }

        public boolean done() {
            return done;
        }
    }
}
