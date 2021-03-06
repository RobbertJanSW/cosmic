package com.cloud.hypervisor.xenserver.resource;

import com.cloud.common.resource.ServerResource;

import javax.ejb.Local;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Local(value = ServerResource.class)
public class XcpServerResource extends CitrixResourceBase {

    private final static Logger s_logger = LoggerFactory.getLogger(XcpServerResource.class);
    private final static long mem_32m = 33554432L;

    @Override
    protected String getPatchFilePath() {
        return "scripts/vm/hypervisor/xenserver/xcpserver/patch";
    }

    @Override
    public boolean isDmcEnabled(final Connection conn, final Host host) {
        //Dynamic Memory Control (DMC) is a technology provided by Xen Cloud Platform (XCP), starting from the 0.5 release
        //For the supported XCPs dmc is default enabled, XCP 1.0.0, 1.1.0, 1.4.x, 1.5 beta, 1.6.x;
        return true;
    }

    /**
     * XCP provides four memory configuration fields through which
     * administrators can control this behaviour:
     * <p>
     * static-min
     * dynamic-min
     * dynamic-max
     * static-max
     * <p>
     * The fields static-{min,max} act as *hard* lower and upper
     * bounds for a guest's memory. For a running guest:
     * it's not possible to assign the guest more memory than
     * static-max without first shutting down the guest.
     * it's not possible to assign the guest less memory than
     * static-min without first shutting down the guest.
     * <p>
     * The fields dynamic-{min,max} act as *soft* lower and upper
     * bounds for a guest's memory. It's possible to change these
     * fields even when a guest is running.
     * <p>
     * The dynamic range must lie wholly within the static range. To
     * put it another way, XCP at all times ensures that:
     * <p>
     * static-min <= dynamic-min <= dynamic-max <= static-max
     * <p>
     * At all times, XCP will attempt to keep a guest's memory usage
     * between dynamic-min and dynamic-max.
     * <p>
     * If dynamic-min = dynamic-max, then XCP will attempt to keep
     * a guest's memory allocation at a constant size.
     * <p>
     * If dynamic-min < dynamic-max, then XCP will attempt to give
     * the guest as much memory as possible, while keeping the guest
     * within dynamic-min and dynamic-max.
     * <p>
     * If there is enough memory on a given host to give all resident
     * guests dynamic-max, then XCP will attempt do so.
     * <p>
     * If there is not enough memory to give all guests dynamic-max,
     * then XCP will ask each of the guests (on that host) to use
     * an amount of memory that is the same *proportional* distance
     * between dynamic-min and dynamic-max.
     * <p>
     * XCP will refuse to start guests if starting those guests would
     * cause the sum of all the dynamic-min values to exceed the total
     * host memory (taking into account various memory overheads).
     * <p>
     * cf: https://wiki.xenserver.org/index.php?title=XCP_FAQ_Dynamic_Memory_Control
     */
    @Override
    protected void setMemory(final Connection conn, final VM vm, final long minMemsize, final long maxMemsize) throws XmlRpcException, XenAPIException {
        //setMemoryLimits(staticMin, staticMax, dynamicMin, dynamicMax)
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Memory Limits for VM [" + vm.getNameLabel(conn) + "[staticMin:" + mem_32m + ", staticMax:" + maxMemsize + ", dynamicMin: " + minMemsize +
                    ", dynamicMax:" + maxMemsize + "]]");
        }
        vm.setMemoryLimits(conn, mem_32m, maxMemsize, minMemsize, maxMemsize);
    }
}
