package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.common.request.CommandWrapper;
import com.cloud.common.request.ResourceWrapper;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.legacymodel.communication.answer.Answer;
import com.cloud.legacymodel.communication.command.UpgradeSnapshotCommand;

@ResourceWrapper(handles = UpgradeSnapshotCommand.class)
public final class LibvirtUpgradeSnapshotCommandWrapper
        extends CommandWrapper<UpgradeSnapshotCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final UpgradeSnapshotCommand command, final LibvirtComputingResource libvirtComputingResource) {
        return new Answer(command, true, "success");
    }
}
