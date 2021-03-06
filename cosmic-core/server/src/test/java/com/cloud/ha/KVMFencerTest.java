package com.cloud.ha;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.legacymodel.communication.answer.FenceAnswer;
import com.cloud.legacymodel.communication.command.FenceCommand;
import com.cloud.legacymodel.dc.HostStatus;
import com.cloud.legacymodel.exceptions.AgentUnavailableException;
import com.cloud.legacymodel.exceptions.OperationTimedoutException;
import com.cloud.legacymodel.vm.VirtualMachine;
import com.cloud.model.enumeration.HypervisorType;
import com.cloud.resource.ResourceManager;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KVMFencerTest {

    @Mock
    HostDao hostDao;
    @Mock
    AgentManager agentManager;
    @Mock
    AlertManager alertMgr;
    @Mock
    ResourceManager resourceManager;

    KVMFencer fencer;

    @Before
    public void setup() {
        fencer = new KVMFencer();
        fencer._agentMgr = agentManager;
        fencer._alertMgr = alertMgr;
        fencer._hostDao = hostDao;
        fencer._resourceMgr = resourceManager;
    }

    @Test
    public void testWithSingleHost() {
        final HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getStatus()).thenReturn(HostStatus.Up);
        Mockito.when(host.getId()).thenReturn(1l);
        final VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Collections.singletonList(host));
        Assert.assertFalse(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithSingleHostDown() {
        final HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getStatus()).thenReturn(HostStatus.Down);
        Mockito.when(host.getId()).thenReturn(1l);
        final VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Collections.singletonList(host));
        Assert.assertFalse(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithHosts() throws AgentUnavailableException, OperationTimedoutException {
        final HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getStatus()).thenReturn(HostStatus.Up);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(1l);

        final HostVO secondHost = Mockito.mock(HostVO.class);
        Mockito.when(secondHost.getClusterId()).thenReturn(1l);
        Mockito.when(secondHost.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(secondHost.getStatus()).thenReturn(HostStatus.Up);
        Mockito.when(secondHost.getDataCenterId()).thenReturn(1l);
        Mockito.when(secondHost.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(2l);

        final VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Arrays.asList(host, secondHost));

        final FenceAnswer answer = new FenceAnswer(null, true, "ok");
        Mockito.when(agentManager.send(Matchers.anyLong(), Matchers.any(FenceCommand.class))).thenReturn(answer);

        Assert.assertTrue(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithFailingFence() throws AgentUnavailableException, OperationTimedoutException {
        final HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getStatus()).thenReturn(HostStatus.Up);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(1l);

        final HostVO secondHost = Mockito.mock(HostVO.class);
        Mockito.when(secondHost.getClusterId()).thenReturn(1l);
        Mockito.when(secondHost.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(secondHost.getStatus()).thenReturn(HostStatus.Up);
        Mockito.when(secondHost.getDataCenterId()).thenReturn(1l);
        Mockito.when(secondHost.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(2l);

        final VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Arrays.asList(host, secondHost));

        Mockito.when(agentManager.send(Matchers.anyLong(), Matchers.any(FenceCommand.class))).thenThrow(new AgentUnavailableException(2l));

        Assert.assertFalse(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithTimeoutingFence() throws AgentUnavailableException, OperationTimedoutException {
        final HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(host.getStatus()).thenReturn(HostStatus.Up);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(1l);

        final HostVO secondHost = Mockito.mock(HostVO.class);
        Mockito.when(secondHost.getClusterId()).thenReturn(1l);
        Mockito.when(secondHost.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(secondHost.getStatus()).thenReturn(HostStatus.Up);
        Mockito.when(secondHost.getDataCenterId()).thenReturn(1l);
        Mockito.when(secondHost.getPodId()).thenReturn(1l);
        Mockito.when(host.getId()).thenReturn(2l);

        final VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Arrays.asList(host, secondHost));

        Mockito.when(agentManager.send(Matchers.anyLong(), Matchers.any(FenceCommand.class))).thenThrow(new OperationTimedoutException(null, 2l, 0l, 0, false));

        Assert.assertFalse(fencer.fenceOff(virtualMachine, host));
    }

    @Test
    public void testWithSingleNotKVM() {
        final HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1l);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.Any);
        Mockito.when(host.getStatus()).thenReturn(HostStatus.Down);
        Mockito.when(host.getId()).thenReturn(1l);
        Mockito.when(host.getDataCenterId()).thenReturn(1l);
        Mockito.when(host.getPodId()).thenReturn(1l);
        final VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

        Mockito.when(resourceManager.listAllHostsInCluster(1l)).thenReturn(Collections.singletonList(host));
        Assert.assertNull(fencer.fenceOff(virtualMachine, host));
    }
}
