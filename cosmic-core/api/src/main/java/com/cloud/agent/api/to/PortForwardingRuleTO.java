package com.cloud.agent.api.to;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.utils.net.NetUtils;

/**
 * PortForwardingRuleTO specifies one port forwarding rule.
 */
public class PortForwardingRuleTO extends FirewallRuleTO {
    String dstIp;
    int[] dstPortRange;

    protected PortForwardingRuleTO() {
        super();
    }

    public PortForwardingRuleTO(final PortForwardingRule rule, final String srcVlanTag, final String srcIp) {
        super(rule, srcVlanTag, srcIp);
        this.dstIp = rule.getDestinationIpAddress().addr();
        this.dstPortRange = new int[]{rule.getDestinationPortStart()};
    }

    public PortForwardingRuleTO(final long id, final String srcIp, final int srcPortStart, final String dstIp, final int dstPortStart, final String protocol,
                                final boolean revoked, final boolean alreadyAdded) {
        super(id, null, srcIp, protocol, srcPortStart, revoked, alreadyAdded, FirewallRule.Purpose.PortForwarding, null, 0, 0);
        this.dstIp = dstIp;
        this.dstPortRange = new int[]{dstPortStart};
    }

    public String getDstIp() {
        return dstIp;
    }

    public int[] getDstPortRange() {
        return dstPortRange;
    }

    public String getStringDstPortRange() {
        return NetUtils.portRangeToString(dstPortRange);
    }
}
