package com.cloud.storage.motion;

import com.cloud.engine.subsystem.api.storage.DataStore;
import com.cloud.engine.subsystem.api.storage.StrategyPriority;
import com.cloud.engine.subsystem.api.storage.VolumeInfo;
import com.cloud.legacymodel.communication.answer.Answer;
import com.cloud.legacymodel.communication.answer.MigrateWithStorageAcrossClustersAnswer;
import com.cloud.legacymodel.communication.command.MigrateWithStorageAcrossClustersCommand;
import com.cloud.legacymodel.dc.Host;
import com.cloud.legacymodel.exceptions.AgentUnavailableException;
import com.cloud.legacymodel.exceptions.CloudRuntimeException;
import com.cloud.legacymodel.exceptions.OperationTimedoutException;
import com.cloud.legacymodel.to.StorageFilerTO;
import com.cloud.legacymodel.to.VirtualMachineTO;
import com.cloud.legacymodel.to.VolumeTO;
import com.cloud.legacymodel.utils.Pair;
import com.cloud.model.enumeration.HypervisorType;
import com.cloud.vm.VMInstanceVO;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KVMStorageMotionStrategy extends AbstractHyperVisorStorageMotionStrategy {

    private static final Logger s_logger = LoggerFactory.getLogger(KVMStorageMotionStrategy.class);

    @Override
    public StrategyPriority canHandle(final Map<VolumeInfo, DataStore> volumeMap, final Host srcHost, final Host destHost) {
        if (HypervisorType.KVM.equals(srcHost.getHypervisorType()) && HypervisorType.KVM.equals(destHost.getHypervisorType())) {
            return StrategyPriority.HYPERVISOR;
        }

        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    protected Answer migrateVmWithVolumesAcrossCluster(
            final VMInstanceVO instanceTo,
            final VirtualMachineTO vmTo,
            final Host srcHost,
            final Host destHost,
            final Map<VolumeInfo, DataStore> volumeToPoolMap
    ) throws AgentUnavailableException {
        try {
            final List<Pair<VolumeTO, StorageFilerTO>> volumeMapping = buildVolumeMapping(volumeToPoolMap);

            final MigrateWithStorageAcrossClustersCommand command = new MigrateWithStorageAcrossClustersCommand(vmTo, volumeMapping, destHost.getPrivateIpAddress());
            final MigrateWithStorageAcrossClustersAnswer answer = (MigrateWithStorageAcrossClustersAnswer) agentMgr.send(srcHost.getId(), command);
            if (answer == null) {
                final String errorMessage = String.format(
                        "Error when trying to migrate VM %s (with storage) to host %s",
                        instanceTo,
                        destHost
                );
                s_logger.error(errorMessage);
                throw new CloudRuntimeException(errorMessage);
            } else if (!answer.getResult()) {
                final String errorMessage = String.format(
                        "Error when trying to migrate VM %s (with storage) to host %s => Details: %s",
                        instanceTo,
                        destHost,
                        answer.getDetails()
                );
                s_logger.error(errorMessage);
                throw new CloudRuntimeException(errorMessage);
            } else {
                // Update the volume details after migration.
                updateVolumePathsAfterMigration(volumeToPoolMap, answer.getVolumes());
            }

            return answer;
        } catch (final OperationTimedoutException e) {
            final String errorMessage = String.format(
                    "Operation timed out when attempting to migrate VM %s to host %s",
                    instanceTo,
                    destHost
            );
            s_logger.error(errorMessage, e);
            throw new AgentUnavailableException(errorMessage, srcHost.getId());
        }
    }
}
