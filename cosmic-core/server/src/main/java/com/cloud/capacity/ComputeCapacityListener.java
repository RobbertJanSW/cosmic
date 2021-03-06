package com.cloud.capacity;

import com.cloud.agent.Listener;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.legacymodel.communication.answer.AgentControlAnswer;
import com.cloud.legacymodel.communication.answer.Answer;
import com.cloud.legacymodel.communication.command.Command;
import com.cloud.legacymodel.communication.command.agentcontrol.AgentControlCommand;
import com.cloud.legacymodel.communication.command.startup.StartupCommand;
import com.cloud.legacymodel.communication.command.startup.StartupRoutingCommand;
import com.cloud.legacymodel.dc.Host;
import com.cloud.legacymodel.dc.HostStatus;

public class ComputeCapacityListener implements Listener {
    CapacityDao _capacityDao;
    CapacityManager _capacityMgr;
    float _cpuOverProvisioningFactor = 1.0f;

    public ComputeCapacityListener(final CapacityDao capacityDao, final CapacityManager capacityMgr) {
        super();
        this._capacityDao = capacityDao;
        this._capacityMgr = capacityMgr;
    }

    @Override
    public boolean processAnswers(final long agentId, final long seq, final Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(final long agentId, final long seq, final Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(final long agentId, final AgentControlCommand cmd) {

        return null;
    }

    @Override
    public void processConnect(final Host server, final StartupCommand[] startupCommands, final boolean forRebalance) {
        for (final StartupCommand startupCommand : startupCommands) {
            if (!(startupCommand instanceof StartupRoutingCommand)) {
                return;
            }
            _capacityMgr.updateCapacityForHost(server);
        }
    }

    @Override
    public boolean processDisconnect(final long agentId, final HostStatus state) {
        return false;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean processTimeout(final long agentId, final long seq) {
        return false;
    }
}
