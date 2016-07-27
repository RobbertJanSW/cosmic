package com.cloud.api;

import com.cloud.dao.EntityManager;
import com.cloud.exception.CloudException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.jobs.JobInfo;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiAsyncJobDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger = LoggerFactory.getLogger(ApiAsyncJobDispatcher.class);

    @Inject
    private ApiDispatcher _dispatcher;

    @Inject
    private AsyncJobManager _asyncJobMgr;
    @Inject
    private EntityManager _entityMgr;

    public ApiAsyncJobDispatcher() {
    }

    @Override
    public void runJob(final AsyncJob job) {
        BaseAsyncCmd cmdObj = null;
        try {
            final Class<?> cmdClass = Class.forName(job.getCmd());
            cmdObj = (BaseAsyncCmd) cmdClass.newInstance();
            cmdObj = ComponentContext.inject(cmdObj);
            cmdObj.configure();
            cmdObj.setJob(job);

            final Type mapType = new TypeToken<Map<String, String>>() {
            }.getType();
            final Gson gson = ApiGsonHelper.getBuilder().create();
            final Map<String, String> params = gson.fromJson(job.getCmdInfo(), mapType);

            // whenever we deserialize, the UserContext needs to be updated
            final String userIdStr = params.get("ctxUserId");
            final String acctIdStr = params.get("ctxAccountId");
            final String contextDetails = params.get("ctxDetails");

            Long userId = null;
            Account accountObject = null;

            if (cmdObj instanceof BaseAsyncCreateCmd) {
                final BaseAsyncCreateCmd create = (BaseAsyncCreateCmd) cmdObj;
                create.setEntityId(Long.parseLong(params.get("id")));
                create.setEntityUuid(params.get("uuid"));
            }

            User user = null;
            if (userIdStr != null) {
                userId = Long.parseLong(userIdStr);
                user = _entityMgr.findById(User.class, userId);
            }

            if (acctIdStr != null) {
                accountObject = _entityMgr.findById(Account.class, Long.parseLong(acctIdStr));
            }

            final CallContext ctx = CallContext.register(user, accountObject);
            if (contextDetails != null) {
                final Type objectMapType = new TypeToken<Map<Object, Object>>() {
                }.getType();
                ctx.putContextParameters((Map<Object, Object>) gson.fromJson(contextDetails, objectMapType));
            }

            try {
                // dispatch could ultimately queue the job
                _dispatcher.dispatch(cmdObj, params, true);

                // serialize this to the async job table
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, ApiSerializerHelper.toSerializedString(cmdObj.getResponseObject()));
            } catch (final InvalidParameterValueException | CloudException e) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
            } finally {
                CallContext.unregister();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ServerApiException e) {
            String errorMsg = null;
            int errorCode = ApiErrorCode.INTERNAL_ERROR.getHttpCode();
            if (!(e instanceof ServerApiException)) {
                s_logger.error("Unexpected exception while executing " + job.getCmd(), e);
                errorMsg = e.getMessage();
            } else {
                final ServerApiException sApiEx = (ServerApiException) e;
                errorMsg = sApiEx.getDescription();
                errorCode = sApiEx.getErrorCode().getHttpCode();
            }

            final ExceptionResponse response = new ExceptionResponse();
            response.setErrorCode(errorCode);
            response.setErrorText(errorMsg);
            response.setResponseName((cmdObj == null) ? "unknowncommandresponse" : cmdObj.getCommandName());

            // FIXME:  setting resultCode to ApiErrorCode.INTERNAL_ERROR is not right, usually executors have their exception handling
            //         and we need to preserve that as much as possible here
            _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, ApiErrorCode.INTERNAL_ERROR.getHttpCode(), ApiSerializerHelper.toSerializedString(response));
        }
    }
}
