package com.cloud.api.command.admin.vm;

import com.cloud.api.APICommand;
import com.cloud.api.APICommandGroup;
import com.cloud.api.ApiErrorCode;
import com.cloud.api.ResponseObject.ResponseView;
import com.cloud.api.ServerApiException;
import com.cloud.api.command.user.vm.StopVMCmd;
import com.cloud.api.response.UserVmResponse;
import com.cloud.context.CallContext;
import com.cloud.legacymodel.exceptions.ConcurrentOperationException;
import com.cloud.legacymodel.vm.VirtualMachine;
import com.cloud.uservm.UserVm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@APICommand(name = "stopVirtualMachine", group = APICommandGroup.VirtualMachineService, responseObject = UserVmResponse.class, description = "Stops a virtual machine.", responseView = ResponseView
        .Full, entityType =
        {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class StopVMCmdByAdmin extends StopVMCmd {
    public static final Logger s_logger = LoggerFactory.getLogger(StopVMCmdByAdmin.class.getName());

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        CallContext.current().setEventDetails("Vm Id: " + getId());
        final UserVm result;

        result = _userVmService.stopVirtualMachine(getId(), isForced());

        if (result != null) {
            final UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Full, "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to stop vm");
        }
    }
}
