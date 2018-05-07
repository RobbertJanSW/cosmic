package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.common.request.CommandWrapper;
import com.cloud.common.request.ResourceWrapper;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.legacymodel.communication.answer.Answer;
import com.cloud.legacymodel.communication.answer.GetVncPortAnswer;
import com.cloud.legacymodel.communication.command.GetVncPortCommand;

import org.libvirt.Connect;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles = GetVncPortCommand.class)
public final class LibvirtGetVncPortCommandWrapper
        extends CommandWrapper<GetVncPortCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final GetVncPortCommand command, final LibvirtComputingResource libvirtComputingResource) {
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(command.getName());
            final Integer vncPort = libvirtComputingResource.getVncPort(conn, command.getName());
            return new GetVncPortAnswer(command, libvirtComputingResource.getPrivateIp(), 5900 + vncPort);
        } catch (final LibvirtException e) {
            return new GetVncPortAnswer(command, e.toString());
        }
    }
}
