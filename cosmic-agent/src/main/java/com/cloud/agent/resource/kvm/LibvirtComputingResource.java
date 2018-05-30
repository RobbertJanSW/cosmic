package com.cloud.agent.resource.kvm;

import static com.cloud.agent.resource.kvm.LibvirtComputingResource.BridgeType.OPENVSWITCH;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.FORMAT_NETWORK_SPEED;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.PATH_PATCH_DIR;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.PATH_SCRIPTS_NETWORK_DOMR;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_CREATE_TEMPLATE;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_KVM_HEART_BEAT;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_LOCAL_GATEWAY;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_MANAGE_SNAPSHOT;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_OVS_PVLAN_DHCP_HOST;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_OVS_PVLAN_VM;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_PING_TEST;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_RESIZE_VOLUME;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_ROUTER_PROXY;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_SEND_CONFIG_PROPERTIES;
import static com.cloud.agent.resource.kvm.LibvirtComputingResourceProperties.Constants.SCRIPT_VERSIONS;

import static java.util.UUID.randomUUID;

import com.cloud.agent.resource.AgentResource;
import com.cloud.agent.resource.AgentResourceBase;
import com.cloud.agent.resource.kvm.LibvirtVmDef.ClockDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.ConsoleDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.CpuModeDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.CpuTuneDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.DevicesDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.FeaturesDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.GraphicDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.GuestDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.GuestResourceDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.InputDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.InterfaceDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.MetadataDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.QemuGuestAgentDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.HyperVEnlightenmentFeatureDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.RngDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.ScsiDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.SerialDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.TermPolicy;
import com.cloud.agent.resource.kvm.LibvirtVmDef.VideoDef;
import com.cloud.agent.resource.kvm.LibvirtVmDef.WatchDogDef;
import com.cloud.agent.resource.kvm.storage.KvmPhysicalDisk;
import com.cloud.agent.resource.kvm.storage.KvmStoragePool;
import com.cloud.agent.resource.kvm.storage.KvmStoragePoolManager;
import com.cloud.agent.resource.kvm.storage.KvmStorageProcessor;
import com.cloud.agent.resource.kvm.wrapper.LibvirtRequestWrapper;
import com.cloud.agent.resource.kvm.wrapper.LibvirtUtilitiesHelper;
import com.cloud.agent.resource.kvm.xml.LibvirtDiskDef;
import com.cloud.agent.resource.kvm.xml.LibvirtDiskDef.DeviceType;
import com.cloud.agent.resource.kvm.xml.LibvirtDiskDef.DiscardType;
import com.cloud.agent.resource.kvm.xml.LibvirtDiskDef.DiskProtocol;
import com.cloud.common.resource.ServerResource;
import com.cloud.common.storageprocessor.resource.StorageSubsystemCommandHandler;
import com.cloud.common.storageprocessor.resource.StorageSubsystemCommandHandlerBase;
import com.cloud.common.virtualnetwork.VirtualRoutingResource;
import com.cloud.legacymodel.ExecutionResult;
import com.cloud.legacymodel.communication.answer.Answer;
import com.cloud.legacymodel.communication.command.Command;
import com.cloud.legacymodel.communication.command.NetworkElementCommand;
import com.cloud.legacymodel.communication.command.PingCommand;
import com.cloud.legacymodel.communication.command.PingRoutingCommand;
import com.cloud.legacymodel.communication.command.StartupCommand;
import com.cloud.legacymodel.communication.command.StartupRoutingCommand;
import com.cloud.legacymodel.communication.command.StartupStorageCommand;
import com.cloud.legacymodel.communication.command.UpdateNetworkOverviewCommand;
import com.cloud.legacymodel.exceptions.CloudRuntimeException;
import com.cloud.legacymodel.exceptions.InternalErrorException;
import com.cloud.legacymodel.network.VRScripts;
import com.cloud.legacymodel.network.VirtualRouterDeployer;
import com.cloud.legacymodel.storage.StoragePoolInfo;
import com.cloud.legacymodel.storage.VmDiskStatsEntry;
import com.cloud.legacymodel.to.DataStoreTO;
import com.cloud.legacymodel.to.DataTO;
import com.cloud.legacymodel.to.DiskTO;
import com.cloud.legacymodel.to.MetadataTO;
import com.cloud.legacymodel.to.NfsTO;
import com.cloud.legacymodel.to.NicTO;
import com.cloud.legacymodel.to.PrimaryDataStoreTO;
import com.cloud.legacymodel.to.VirtualMachineTO;
import com.cloud.legacymodel.to.VolumeObjectTO;
import com.cloud.legacymodel.utils.Pair;
import com.cloud.legacymodel.utils.Ternary;
import com.cloud.legacymodel.vm.HostVmStateReportEntry;
import com.cloud.legacymodel.vm.VirtualMachine.PowerState;
import com.cloud.legacymodel.vm.VmStatsEntry;
import com.cloud.model.enumeration.BroadcastDomainType;
import com.cloud.model.enumeration.DiskCacheMode;
import com.cloud.model.enumeration.DiskControllerType;
import com.cloud.model.enumeration.GuestNetType;
import com.cloud.model.enumeration.HostType;
import com.cloud.model.enumeration.HypervisorType;
import com.cloud.model.enumeration.OptimiseFor;
import com.cloud.model.enumeration.RngBackendModel;
import com.cloud.model.enumeration.RouterPrivateIpStrategy;
import com.cloud.model.enumeration.StoragePoolType;
import com.cloud.model.enumeration.StorageResourceType;
import com.cloud.model.enumeration.TrafficType;
import com.cloud.model.enumeration.VirtualMachineType;
import com.cloud.model.enumeration.VolumeType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.hypervisor.HypervisorUtils;
import com.cloud.utils.linux.CpuStat;
import com.cloud.utils.linux.MemStat;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.qemu.QemuImg.PhysicalDiskFormat;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.OutputInterpreter.AllLinesParser;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.utils.storage.JavaStorageLayer;
import com.cloud.utils.storage.StorageLayer;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.DomainSnapshot;
import org.libvirt.Library;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * LibvirtComputingResource execute requests on the computing/routing host using the libvirt API
 *
 * @config {@table || Param Name | Description | Values | Default || || hypervisor.type | type of local hypervisor |
 * string | kvm || || hypervisor.uri | local hypervisor to connect to | URI | qemu:///system || || domr.arch |
 * instruction set for domr template | string | i686 || || private.bridge.name | private bridge where the domrs
 * have their private interface | string | vmops0 || || public.bridge.name | public bridge where the domrs have
 * their public interface | string | br0 || || private.network.name | name of the network where the domrs have
 * their private interface | string | vmops-private || || private.ipaddr.start | start of the range of private
 * ip addresses for domrs | ip address | 192.168.166.128 || || private.ipaddr.end | end of the range of private
 * ip addresses for domrs | ip address | start + 126 || || private.macaddr.start | start of the range of private
 * mac addresses for domrs | mac address | 00:16:3e:77:e2:a0 || || private.macaddr.end | end of the range of
 * private mac addresses for domrs | mac address | start + 126 || || pool | the parent of the storage pool
 * hierarchy * }
 **/
@Local(value = {ServerResource.class})
public class LibvirtComputingResource extends AgentResourceBase implements AgentResource, VirtualRouterDeployer {

    public static final String SSHKEYSPATH = "/root/.ssh";
    public static final String SSHPRVKEYPATH = SSHKEYSPATH + File.separator + "id_rsa.cloud";
    public static final String SSHPUBKEYPATH = SSHKEYSPATH + File.separator + "id_rsa.pub.cloud";
    public static final String BASH_SCRIPT_PATH = "/bin/bash";

    protected static final String DEFAULT_OVS_VIF_DRIVER_CLASS = "com.cloud.agent.resource.kvm.OvsVifDriver";
    protected static final String DEFAULT_BRIDGE_VIF_DRIVER_CLASS = "com.cloud.agent.resource.kvm.BridgeVifDriver";
    protected static final HashMap<DomainState, PowerState> s_powerStatesTable;
    private static final Logger logger = LoggerFactory.getLogger(LibvirtComputingResource.class);

    static {
        s_powerStatesTable = new HashMap<>();
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_SHUTOFF, PowerState.PowerOff);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_PAUSED, PowerState.PowerPaused);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_RUNNING, PowerState.PowerOn);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_BLOCKED, PowerState.PowerOn);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_NOSTATE, PowerState.PowerUnknown);
        s_powerStatesTable.put(DomainState.VIR_DOMAIN_SHUTDOWN, PowerState.PowerOff);
    }

    private static final Thread LIBVIRT_EVENT_LOOP;

    // Start the Libvirt event loop
    static {
        try {
            logger.debug("Starting Libvirt event loop");
            Library.initEventLoop();
        } catch (final LibvirtException e) {
            throw new CloudRuntimeException(e);
        }

        LIBVIRT_EVENT_LOOP = new Thread(() -> {
            try {
                Library.runEventLoop();
            } catch (final LibvirtException e) {
                throw new RuntimeException(e);
            } catch (final InterruptedException ignored) {
            }
        });

        LIBVIRT_EVENT_LOOP.setName("Libvirt-Event-Loop");
        LIBVIRT_EVENT_LOOP.setDaemon(true);
        LIBVIRT_EVENT_LOOP.start();
    }

    private final LibvirtComputingResourceProperties libvirtComputingResourceProperties = new LibvirtComputingResourceProperties();

    private final Map<String, String> pifs = new HashMap<>();
    private final Map<String, VmStats> vmStats = new ConcurrentHashMap<>();
    private final LibvirtUtilitiesHelper libvirtUtilitiesHelper = new LibvirtUtilitiesHelper();
    private long hypervisorLibvirtVersion;
    private long hypervisorQemuVersion;
    private String hypervisorPath;
    private String privateIp;
    private String localGateway;
    private boolean diskActivityCheckEnabled;
    private final long diskActivityCheckFileSizeMin = 10485760; // 10MB
    private int diskActivityCheckTimeoutSeconds = 120; // 120s
    private long diskActivityInactiveThresholdMilliseconds = 30000; // 30s
    private final RngBackendModel rngBackendModel = RngBackendModel.RANDOM;
    private final String rngPath = "/dev/random";
    private final CpuStat cpuStat = new CpuStat();
    private final MemStat memStat = new MemStat();
    private StorageSubsystemCommandHandler storageHandler;
    private final String[] ifNamePatterns = {
            "^eth",
            "^bond",
            "^vlan",
            "^vx",
            "^em",
            "^ens",
            "^eno",
            "^enp",
            "^team",
            "^enx",
            "^p\\d+p\\d+"
    };
    private String versionstringpath;
    private String sendConfigPropertiesPath;
    private String manageSnapshotPath;
    private String resizeVolumePath;
    private String createTmplPath;
    private String heartBeatPath;
    private String ovsPvlanDhcpHostPath;
    private String ovsPvlanVmPath;
    private String routerProxyPath;
    private long hvVersion;
    private VirtualRoutingResource virtRouterResource;
    private String pingTestPath;
    private String updateHostPasswdPath;
    private KvmHaMonitor monitor;
    private StorageLayer storage;
    private KvmStoragePoolManager storagePoolMgr;
    private VifDriver defaultVifDriver;
    private Map<TrafficType, VifDriver> trafficTypeVifDrivers;
    private long totalMemory;

    @Override
    public ExecutionResult executeInVR(final String routerIp, final String script, final String args) {
        return executeInVR(routerIp, script, args, getScriptsTimeout() / 1000);
    }

    @Override
    public ExecutionResult executeInVR(final String routerIp, final String script, final String args, final int timeout) {
        final Script command = new Script(this.routerProxyPath, timeout * 1000, logger);
        final AllLinesParser parser = new AllLinesParser();
        command.add(script);
        command.add(routerIp);
        if (args != null) {
            command.add(args);
        }
        String details = command.execute(parser);
        if (details == null) {
            details = parser.getLines().trim();
        }

        logger.debug("Executing script in VR " + script);

        return new ExecutionResult(command.getExitValue() == 0, details);
    }

    @Override
    public ExecutionResult createFileInVR(final String routerIp, final String path, final String filename,
                                          final String content) {
        final File permKey = new File("/root/.ssh/id_rsa.cloud");
        String error = null;

        logger.debug("Creating file in VR " + filename);

        try {
            SshHelper.scpTo(routerIp, 3922, "root", permKey, null, path, content.getBytes(), filename, null);
        } catch (final Exception e) {
            logger.warn("Fail to create file " + path + filename + " in VR " + routerIp, e);
            error = e.getMessage();
        }
        return new ExecutionResult(error == null, error);
    }

    @Override
    public ExecutionResult prepareCommand(final NetworkElementCommand cmd) {
        // Update IP used to access router
        cmd.setRouterAccessIp(cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP));
        assert cmd.getRouterAccessIp() != null;

        if (cmd instanceof UpdateNetworkOverviewCommand && ((UpdateNetworkOverviewCommand) cmd).isPlugNics()) {
            return prepareNetworkElementCommand((UpdateNetworkOverviewCommand) cmd);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(final NetworkElementCommand cmd) {
        return new ExecutionResult(true, null);
    }

    public List<InterfaceDef> getInterfaces(final Connect conn, final String vmName, final String interfaceToExclude) {
        final List<InterfaceDef> interfaces = getInterfaces(conn, vmName);
        final List<InterfaceDef> interfacesToReturn = new ArrayList<>();
        for (final InterfaceDef interfaceDef : interfaces) {
            if (!interfaceDef.getBrName().equalsIgnoreCase(interfaceToExclude)) {
                interfacesToReturn.add(interfaceDef);
            }
        }

        return interfacesToReturn;
    }

    public List<VifDriver> getAllVifDrivers() {
        final Set<VifDriver> vifDrivers = new HashSet<>();

        vifDrivers.add(this.defaultVifDriver);
        vifDrivers.addAll(this.trafficTypeVifDrivers.values());

        return new ArrayList<>(vifDrivers);
    }

    protected ExecutionResult prepareNetworkElementCommand(final UpdateNetworkOverviewCommand cmd) {
        final String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

        try {
            final Connect conn = LibvirtConnection.getConnectionByVmName(routerName);
            final Map<String, Integer> bridgeToNicNum = new HashMap<>();
            final List<InterfaceDef> pluggedNics = getInterfaces(conn, routerName);

            buildBridgeToNicNumHashMap(bridgeToNicNum, pluggedNics);

            return new ExecutionResult(true, null);
        } catch (final LibvirtException e) {
            logger.error("Ip Assoc failure on applying one ip due to exception:  ", e);
            return new ExecutionResult(false, e.getMessage());
        }
    }

    public List<InterfaceDef> getInterfaces(final Connect conn, final String vmName) {
        final LibvirtDomainXmlParser parser = new LibvirtDomainXmlParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
            parser.parseDomainXml(dm.getXMLDesc(0));
            return parser.getInterfaces();
        } catch (final LibvirtException e) {
            logger.debug("Failed to get dom xml: " + e.toString());
            return new ArrayList<>();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (final LibvirtException e) {
                logger.trace("Ignoring libvirt error.", e);
            }
        }
    }

    private Integer buildBridgeToNicNumHashMap(final Map<String, Integer> bridgeToNicNum, final List<InterfaceDef> pluggedNics) {
        Integer devNum = 0;
        for (final InterfaceDef pluggedNic : pluggedNics) {
            final String pluggedVlan = pluggedNic.getBrName();

            if (pluggedVlan.equalsIgnoreCase(getLinkLocalBridgeName()) || pluggedVlan.equalsIgnoreCase(getPublicBridgeName())
                    || pluggedVlan.equalsIgnoreCase(getPrivBridgeName()) || pluggedVlan.equalsIgnoreCase(getGuestBridgeName())) {
                bridgeToNicNum.put(pluggedVlan, devNum);
            }
            devNum++;
        }
        return devNum;
    }

    private String getPrivBridgeName() {
        return this.libvirtComputingResourceProperties.getPrivateNetworkDevice();
    }

    private String getLinkLocalBridgeName() {
        return this.libvirtComputingResourceProperties.getPrivateBridgeName();
    }

    private String getBridgeNameFromTrafficType(final TrafficType trafficType) {
        final String bridgeName;
        switch (trafficType) {
            case Public:
                bridgeName = getPublicBridgeName();
                break;
            case Management:
                bridgeName = getPrivBridgeName();
                break;
            case Guest:
                bridgeName = getGuestBridgeName();
                break;
            case Control:
                bridgeName = getLinkLocalBridgeName();
                break;
            default:
                bridgeName = "";
        }
        return bridgeName;
    }

    private void vifHotPlug(final Connect conn, final String vmName, final String broadcastUri, final String macAddr)
            throws InternalErrorException, LibvirtException {
        final NicTO nicTo = new NicTO();
        nicTo.setMac(macAddr);
        nicTo.setType(TrafficType.Public);
        if (broadcastUri == null) {
            nicTo.setBroadcastType(BroadcastDomainType.Native);
        } else {
            final URI uri = BroadcastDomainType.fromString(broadcastUri);
            nicTo.setBroadcastType(BroadcastDomainType.getSchemeValue(uri));
            nicTo.setBroadcastUri(uri);
        }

        final Domain vm = getDomain(conn, vmName);
        vm.attachDevice(getVifDriver(nicTo.getType()).plug(nicTo, "Default - VirtIO capable OS (64-bit)", "").toString());
    }

    public String networkUsage(final String privateIpAddress, final String option, final String vif) {
        final Script getUsage = new Script(this.routerProxyPath, logger);
        getUsage.add("netusage.sh");
        getUsage.add(privateIpAddress);
        if (option.equals("get")) {
            getUsage.add("-g");
        } else if (option.equals("create")) {
            getUsage.add("-c");
        } else if (option.equals("reset")) {
            getUsage.add("-r");
        } else if (option.equals("addVif")) {
            getUsage.add("-a", vif);
        } else if (option.equals("deleteVif")) {
            getUsage.add("-d", vif);
        }

        final OutputInterpreter.OneLineParser usageParser = new OutputInterpreter.OneLineParser();
        final String result = getUsage.execute(usageParser);
        if (result != null) {
            logger.debug("Failed to execute networkUsage:" + result);
            return null;
        }
        return usageParser.getLine();
    }

    private String getBroadcastUriFromBridge(final String brName) {
        final String pif = matchPifFileInDirectory(brName);
        final Pattern pattern = Pattern.compile("(\\D+)(\\d+)(\\D*)(\\d*)");
        final Matcher matcher = pattern.matcher(pif);
        logger.debug("getting broadcast uri for pif " + pif + " and bridge " + brName);
        if (matcher.find()) {
            if (brName.startsWith("brvx")) {
                return BroadcastDomainType.Vxlan.toUri(matcher.group(2)).toString();
            } else {
                if (!matcher.group(4).isEmpty()) {
                    return BroadcastDomainType.Vlan.toUri(matcher.group(4)).toString();
                } else {
                    // untagged or not matching (eth|bond|team)#.#
                    logger.debug("failed to get vNet id from bridge " + brName
                            + "attached to physical interface" + pif + ", perhaps untagged interface");
                    return "";
                }
            }
        } else {
            logger.debug("failed to get vNet id from bridge " + brName + "attached to physical interface" + pif);
            return "";
        }
    }

    public Domain getDomain(final Connect conn, final String vmName) throws LibvirtException {
        return conn.domainLookupByName(vmName);
    }

    public VifDriver getVifDriver(final TrafficType trafficType) {
        VifDriver vifDriver = this.trafficTypeVifDrivers.get(trafficType);

        if (vifDriver == null) {
            vifDriver = this.defaultVifDriver;
        }

        return vifDriver;
    }

    private String matchPifFileInDirectory(final String bridgeName) {
        final File brif = new File("/sys/devices/virtual/net/" + bridgeName + "/brif");

        if (!brif.isDirectory()) {
            final File pif = new File("/sys/class/net/" + bridgeName);
            if (pif.isDirectory()) {
                // if bridgeName already refers to a pif, return it as-is
                return bridgeName;
            }
            logger.debug("failing to get physical interface from bridge " + bridgeName + ", does " + brif.getAbsolutePath() + "exist?");
            return "";
        }

        final File[] interfaces = brif.listFiles();

        for (final File interface1 : interfaces) {
            final String fname = interface1.getName();
            logger.debug("matchPifFileInDirectory: file name '" + fname + "'");
            if (isInterface(fname)) {
                return fname;
            }
        }

        logger.debug("failing to get physical interface from bridge " + bridgeName
                + ", did not find an eth*, bond*, team*, vlan*, em*, p*p*, ens*, eno*, enp*, or enx* in "
                + brif.getAbsolutePath());
        return "";
    }

    public boolean isInterface(final String fname) {
        final StringBuffer commonPattern = new StringBuffer();
        for (final String ifNamePattern : this.ifNamePatterns) {
            commonPattern.append("|(").append(ifNamePattern).append(".*)");
        }
        if (fname.matches(commonPattern.toString())) {
            return true;
        }
        return false;
    }

    public LibvirtUtilitiesHelper getLibvirtUtilitiesHelper() {
        return this.libvirtUtilitiesHelper;
    }

    public CpuStat getCpuStat() {
        return this.cpuStat;
    }

    public MemStat getMemStat() {
        return this.memStat;
    }

    public VirtualRoutingResource getVirtRouterResource() {
        return this.virtRouterResource;
    }

    public String getPublicBridgeName() {
        return this.libvirtComputingResourceProperties.getPublicNetworkDevice();
    }

    public KvmStoragePoolManager getStoragePoolMgr() {
        return this.storagePoolMgr;
    }

    public String getPrivateIp() {
        return this.privateIp;
    }

    public int getMigrateDowntime() {
        return this.libvirtComputingResourceProperties.getVmMigrateDowntime();
    }

    public int getMigratePauseAfter() {
        return this.libvirtComputingResourceProperties.getVmMigratePauseafter();
    }

    public int getMigrateSpeed() {
        return this.libvirtComputingResourceProperties.getVmMigrateSpeed();
    }

    public String getPingTestPath() {
        return this.pingTestPath;
    }

    public String getUpdateHostPasswdPath() {
        return this.updateHostPasswdPath;
    }

    public int getScriptsTimeout() {
        return this.libvirtComputingResourceProperties.getScriptsTimeout();
    }

    public KvmHaMonitor getMonitor() {
        return this.monitor;
    }

    public StorageLayer getStorage() {
        return this.storage;
    }

    public String createTmplPath() {
        return this.createTmplPath;
    }

    public int getCmdsTimeout() {
        return this.libvirtComputingResourceProperties.getCmdsTimeout();
    }

    public String manageSnapshotPath() {
        return this.manageSnapshotPath;
    }

    public String getGuestBridgeName() {
        return this.libvirtComputingResourceProperties.getGuestNetworkDevice();
    }

    public String getOvsPvlanDhcpHostPath() {
        return this.ovsPvlanDhcpHostPath;
    }

    public String getOvsPvlanVmPath() {
        return this.ovsPvlanVmPath;
    }

    public String getResizeVolumePath() {
        return this.resizeVolumePath;
    }

    public StorageSubsystemCommandHandler getStorageHandler() {
        return this.storageHandler;
    }

    @Override
    public boolean configure(final Map<String, Object> params) throws ConfigurationException {
        this.libvirtComputingResourceProperties.load(params);

        // TODO: Do not use params anymore, it should eventually be removed from the interface definition
        //       For now we pay the penalty of generating the map every time we need to use it, in the future all methods should accept the pojo

        Map<String, Object> propertiesMap = this.libvirtComputingResourceProperties.buildPropertiesMap();

        final boolean success = super.configure(propertiesMap);
        if (!success) {
            return false;
        }

        initStorage(propertiesMap);

        if (initVirtualRoutingResource(propertiesMap)) {
            return false;
        }

        initScripts(this.libvirtComputingResourceProperties);

        initConnectionToLibvirtDaemon(this.libvirtComputingResourceProperties);
        initMonitorThread();

        initStoragePoolManager();
        if (checkSystemvmIso(this.libvirtComputingResourceProperties)) {
            return false;
        }

        initBridges();
        checkPhysicalInterfaces();
        initLocalGateway();

        checkVmMigrationSpeed(this.libvirtComputingResourceProperties);

        // TODO: the next few keys added to the map are not added to the POJO because they are only commuted here and are not needed in the POJO
        propertiesMap = this.libvirtComputingResourceProperties.buildPropertiesMap();

        final Map<String, String> bridges = new HashMap<>();
        bridges.put("linklocal", getLinkLocalBridgeName());
        bridges.put("public", getPublicBridgeName());
        bridges.put("private", getPrivBridgeName());
        bridges.put("guest", getGuestBridgeName());

        propertiesMap.put("libvirt.host.bridges", bridges);
        propertiesMap.put("libvirt.host.pifs", this.pifs);
        propertiesMap.put("libvirt.computing.resource", this);
        propertiesMap.put("libvirtVersion", this.hypervisorLibvirtVersion);

        configureVifDrivers(propertiesMap);
        configureDiskActivityChecks(propertiesMap);

        final KvmStorageProcessor storageProcessor = new KvmStorageProcessor(this.storagePoolMgr, this);
        storageProcessor.configure(name, propertiesMap);
        this.storageHandler = new StorageSubsystemCommandHandlerBase(storageProcessor);

        return true;
    }

    private void initScripts(final LibvirtComputingResourceProperties libvirtComputingResourceProperties) throws ConfigurationException {
        final String kvmScriptsDir = libvirtComputingResourceProperties.getHypervisorScriptsDir();
        final String networkScriptsDir = libvirtComputingResourceProperties.getNetworkScriptsDir();
        final String storageScriptsDir = libvirtComputingResourceProperties.getStorageScriptsDir();
        this.updateHostPasswdPath = findScriptPath(kvmScriptsDir, VRScripts.UPDATE_HOST_PASSWD);
        this.versionstringpath = findScriptPath(kvmScriptsDir, SCRIPT_VERSIONS);
        this.sendConfigPropertiesPath = findScriptPath(kvmScriptsDir, PATH_PATCH_DIR, SCRIPT_SEND_CONFIG_PROPERTIES);
        this.heartBeatPath = findScriptPath(kvmScriptsDir, SCRIPT_KVM_HEART_BEAT);
        this.manageSnapshotPath = findScriptPath(storageScriptsDir, SCRIPT_MANAGE_SNAPSHOT);
        this.resizeVolumePath = findScriptPath(storageScriptsDir, SCRIPT_RESIZE_VOLUME);
        this.createTmplPath = findScriptPath(storageScriptsDir, SCRIPT_CREATE_TEMPLATE);
        this.routerProxyPath = findScriptPath(PATH_SCRIPTS_NETWORK_DOMR, SCRIPT_ROUTER_PROXY);
        this.ovsPvlanDhcpHostPath = findScriptPath(networkScriptsDir, SCRIPT_OVS_PVLAN_DHCP_HOST);
        this.ovsPvlanVmPath = findScriptPath(networkScriptsDir, SCRIPT_OVS_PVLAN_VM);
        this.pingTestPath = findScriptPath(kvmScriptsDir, SCRIPT_PING_TEST);
    }

    private void checkVmMigrationSpeed(final LibvirtComputingResourceProperties libvirtComputingResourceProperties) {
        int migrateSpeed = libvirtComputingResourceProperties.getVmMigrateSpeed();
        if (migrateSpeed == -1) {
            // get guest network device speed
            migrateSpeed = 0;
            final String speed = Script.runSimpleBashScript(String.format(FORMAT_NETWORK_SPEED, this.pifs.get("public")));
            if (speed != null) {
                final String[] tokens = speed.split("M");
                if (tokens.length == 2) {
                    try {
                        migrateSpeed = Integer.parseInt(tokens[0]);
                    } catch (final NumberFormatException e) {
                        logger.trace("Ignoring migrateSpeed extraction error.", e);
                    }
                    logger.debug("device " + this.pifs.get("public") + " has speed: " + String.valueOf(migrateSpeed));
                }
            }
            libvirtComputingResourceProperties.setVmMigrateSpeed(migrateSpeed);
        }
    }

    private void initLocalGateway() {
        this.localGateway = Script.runSimpleBashScript(SCRIPT_LOCAL_GATEWAY);
        if (this.localGateway == null) {
            logger.debug("Failed to found the local gateway");
        }
    }

    private void checkPhysicalInterfaces() throws ConfigurationException {
        if (this.pifs.get("private") == null) {
            logger.debug("Failed to get private nic name");
            throw new ConfigurationException("Failed to get private nic name");
        }

        if (this.pifs.get("public") == null) {
            logger.debug("Failed to get public nic name");
            throw new ConfigurationException("Failed to get public nic name");
        }
        logger.debug("Found pif: " + this.pifs.get("private") + " on " + getPrivBridgeName() + ", pif: " + this.pifs.get("public") + " on " + getPublicBridgeName());
    }

    private void initBridges() {
        logger.debug("Initializing bridges");
        switch (getBridgeType()) {
            case OPENVSWITCH:
                getOvsPifs();
                break;
            case NATIVE:
            default:
                getPifs();
                break;
        }
    }

    private BridgeType getBridgeType() {
        return this.libvirtComputingResourceProperties.getNetworkBridgeType();
    }

    private boolean checkSystemvmIso(final LibvirtComputingResourceProperties libvirtComputingResourceProperties) {
        if (!this.storage.exists(libvirtComputingResourceProperties.getSystemvmIsoPath())) {
            logger.debug("Can't find system vm ISO");
            return true;
        }
        return false;
    }

    private void initStoragePoolManager() {
        this.storagePoolMgr = new KvmStoragePoolManager(this.storage, this.monitor);
    }

    private void initMonitorThread() {
        final String[] info = NetUtils.getNetworkParams(this._privateNic);
        this.monitor = new KvmHaMonitor(null, info[0], this.heartBeatPath);
        final Thread ha = new Thread(this.monitor);
        ha.start();
    }

    private void initConnectionToLibvirtDaemon(final LibvirtComputingResourceProperties libvirtComputingResourceProperties) throws ConfigurationException {
        LibvirtConnection.initialize(getHypervisorUri());
        final Connect conn = connectToHypervisor();

        checkIsHvmEnabled(conn);

        this.hypervisorPath = getHypervisorPath(conn);
        try {
            this.hvVersion = conn.getVersion();
            this.hvVersion = this.hvVersion % 1000000 / 1000;
            this.hypervisorLibvirtVersion = conn.getLibVirVersion();
            this.hypervisorQemuVersion = conn.getVersion();
        } catch (final LibvirtException e) {
            logger.trace("Ignoring libvirt error.", e);
        }

        if (libvirtComputingResourceProperties.hasGuestCpuMode()) {
            if (this.hypervisorLibvirtVersion < 9 * 1000 + 10) {
                logger.warn("Libvirt version 0.9.10 required for guest cpu mode, but version {} detected, so it will be disabled", prettyVersion(this.hypervisorLibvirtVersion));
                libvirtComputingResourceProperties.unsetGuestCpuMode();
                libvirtComputingResourceProperties.unsetGuestCpuModel();
            }
        }
    }

    private String getHypervisorUri() {
        return this.libvirtComputingResourceProperties.getHypervisorUri();
    }

    private boolean initVirtualRoutingResource(final Map<String, Object> propertiesMap) throws ConfigurationException {
        this.virtRouterResource = new VirtualRoutingResource(this);

        return !this.virtRouterResource.configure(propertiesMap);
    }

    private void initStorage(final Map<String, Object> propertiesMap) throws ConfigurationException {
        this.storage = new JavaStorageLayer();
        this.storage.configure("StorageLayer", propertiesMap);
    }

    private static String findScriptPath(final String baseDir, final String path, final String scriptName) throws ConfigurationException {
        final String scriptPath = Script.findScript(baseDir + path, scriptName);
        if (scriptPath == null) {
            throw new ConfigurationException("Unable to find " + scriptName);
        } else {
            return scriptPath;
        }
    }

    private static String findScriptPath(final String baseDir, final String scriptName) throws ConfigurationException {
        return findScriptPath(baseDir, "", scriptName);
    }

    private void checkIsHvmEnabled(final Connect conn) throws ConfigurationException {
        if (HypervisorType.KVM == getHypervisorType()) {
            if (!isHvmEnabled(conn)) {
                throw new ConfigurationException("NO HVM support on this machine, please make sure: "
                        + "1. VT/SVM is supported by your CPU, or is enabled in BIOS. "
                        + "2. kvm modules are loaded (kvm, kvm_amd|kvm_intel)");
            }
        }
    }

    private Connect connectToHypervisor() throws ConfigurationException {
        Connect conn = null;
        try {
            conn = LibvirtConnection.getConnection();

            if (getBridgeType() == OPENVSWITCH) {
                if (conn.getLibVirVersion() < 10 * 1000 + 0) {
                    throw new ConfigurationException("Libvirt version 0.10.0 required for openvswitch support, but version "
                            + conn.getLibVirVersion() + " detected");
                }
            }
        } catch (final LibvirtException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
        return conn;
    }

    private boolean isHvmEnabled(final Connect conn) {
        final LibvirtCapXmlParser parser = new LibvirtCapXmlParser();
        try {
            parser.parseCapabilitiesXml(conn.getCapabilities());
            final ArrayList<String> osTypes = parser.getGuestOsType();
            for (final String o : osTypes) {
                if (o.equalsIgnoreCase("hvm")) {
                    return true;
                }
            }
        } catch (final LibvirtException e) {
            logger.trace("Ignoring libvirt error.", e);
        }
        return false;
    }

    private String getHypervisorPath(final Connect conn) {
        final LibvirtCapXmlParser parser = new LibvirtCapXmlParser();
        try {
            parser.parseCapabilitiesXml(conn.getCapabilities());
        } catch (final LibvirtException e) {
            logger.debug(e.getMessage());
        }
        return parser.getEmulator();
    }

    @Override
    public Answer executeRequest(final Command cmd) {
        logger.debug("Processing cmd " + cmd.toString());

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        try {
            return wrapper.execute(cmd, this);
        } catch (final Exception e) {
            logger.debug("Exception was " + e.getMessage());
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private String prettyVersion(final long version) {
        final long major = version / 1000000;
        final long minor = version % 1000000 / 1000;
        final long release = version % 1000000 % 1000;
        return major + "." + minor + "." + release;
    }

    private void getOvsPifs() {
        logger.debug("Looking for OpenVSwitch bridges");
        final String cmdout = Script.runSimpleBashScript("ovs-vsctl list-br | sed '{:q;N;s/\\n/%/g;t q}'");
        logger.debug("cmdout was " + cmdout);
        final List<String> bridges = Arrays.asList(cmdout.split("%"));
        for (final String bridge : bridges) {
            logger.debug("looking for pif for bridge " + bridge);
            // String pif = getOvsPif(bridge);
            // Not really interested in the pif name at this point for ovs
            // bridges
            final String pif = bridge;
            if (getPublicBridgeName() != null && bridge.equals(getPublicBridgeName())) {
                this.pifs.put("public", pif);
            }
            if (getGuestBridgeName() != null && bridge.equals(getGuestBridgeName())) {
                this.pifs.put("private", pif);
            }
            this.pifs.put(bridge, pif);
        }
        logger.debug("done looking for pifs, no more bridges");
    }

    private void getPifs() {
        logger.debug("Looking for Linux bridges");
        final File dir = new File("/sys/devices/virtual/net");
        final File[] netdevs = dir.listFiles();
        final List<String> bridges = new ArrayList<>();
        for (final File netdev : netdevs) {
            final File isbridge = new File(netdev.getAbsolutePath() + "/bridge");
            final String netdevName = netdev.getName();
            logger.debug("looking in file " + netdev.getAbsolutePath() + "/bridge");
            if (isbridge.exists()) {
                logger.debug("Found bridge " + netdevName);
                bridges.add(netdevName);
            }
        }

        for (final String bridge : bridges) {
            logger.debug("looking for pif for bridge " + bridge);
            final String pif = getPif(bridge);
            if (getPublicBridgeName() != null && bridge.equals(getPublicBridgeName())) {
                this.pifs.put("public", pif);
            }
            if (getGuestBridgeName() != null && bridge.equals(getGuestBridgeName())) {
                this.pifs.put("private", pif);
            }
            this.pifs.put(bridge, pif);
        }

        // guest(private) creates bridges on a pif, if private bridge not found try pif direct
        // This addresses the unnecessary requirement of someone to create an unused bridge just for traffic label
        if (this.pifs.get("private") == null) {
            logger.debug("guest(private) traffic label '" + getGuestBridgeName()
                    + "' not found as bridge, looking for physical interface");
            final File dev = new File("/sys/class/net/" + getGuestBridgeName());
            if (dev.exists()) {
                logger.debug("guest(private) traffic label '" + getGuestBridgeName() + "' found as a physical device");
                this.pifs.put("private", getGuestBridgeName());
            }
        }

        // public creates bridges on a pif, if private bridge not found try pif direct
        // This addresses the unnecessary requirement of someone to create an unused bridge just for traffic label
        if (this.pifs.get("public") == null) {
            logger.debug(
                    "public traffic label '" + getPublicBridgeName() + "' not found as bridge, looking for physical interface");
            final File dev = new File("/sys/class/net/" + getPublicBridgeName());
            if (dev.exists()) {
                logger.debug("public traffic label '" + getPublicBridgeName() + "' found as a physical device");
                this.pifs.put("public", getPublicBridgeName());
            }
        }

        logger.debug("done looking for pifs, no more bridges");
    }

    protected void configureVifDrivers(final Map<String, Object> params) throws ConfigurationException {
        final String libvirtVifDriver = "libvirt.vif.driver";

        this.trafficTypeVifDrivers = new HashMap<>();

        // Load the default vif driver
        String defaultVifDriverName = (String) params.get(libvirtVifDriver);
        if (defaultVifDriverName == null) {
            if (getBridgeType() == OPENVSWITCH) {
                logger.info("No libvirt.vif.driver specified. Defaults to OvsVifDriver.");
                defaultVifDriverName = DEFAULT_OVS_VIF_DRIVER_CLASS;
            } else {
                logger.info("No libvirt.vif.driver specified. Defaults to BridgeVifDriver.");
                defaultVifDriverName = DEFAULT_BRIDGE_VIF_DRIVER_CLASS;
            }
        }
        this.defaultVifDriver = getVifDriverClass(defaultVifDriverName, params);

        // Load any per-traffic-type vif drivers
        for (final Map.Entry<String, Object> entry : params.entrySet()) {
            final String k = entry.getKey();
            final String vifDriverPrefix = libvirtVifDriver + ".";

            if (k.startsWith(vifDriverPrefix)) {
                // Get trafficType
                final String trafficTypeSuffix = k.substring(vifDriverPrefix.length());

                // Does this suffix match a real traffic type?
                final TrafficType trafficType = TrafficType.getTrafficType(trafficTypeSuffix);
                if (!trafficType.equals(TrafficType.None)) {
                    // Get vif driver class name
                    final String vifDriverClassName = (String) entry.getValue();
                    // if value is null, ignore
                    if (vifDriverClassName != null) {
                        // add traffic type to vif driver mapping to Map
                        this.trafficTypeVifDrivers.put(trafficType, getVifDriverClass(vifDriverClassName, params));
                    }
                }
            }
        }
    }

    protected void configureDiskActivityChecks(final Map<String, Object> params) {
        this.diskActivityCheckEnabled = Boolean.parseBoolean((String) params.get("vm.diskactivity.checkenabled"));
        if (this.diskActivityCheckEnabled) {
            final int timeout = NumbersUtil.parseInt((String) params.get("vm.diskactivity.checktimeout_s"), 0);
            if (timeout > 0) {
                this.diskActivityCheckTimeoutSeconds = timeout;
            }
            final long inactiveTime = NumbersUtil.parseLong((String) params.get("vm.diskactivity.inactivetime_ms"), 0L);
            if (inactiveTime > 0) {
                this.diskActivityInactiveThresholdMilliseconds = inactiveTime;
            }
        }
    }

    private String getPif(final String bridge) {
        String pif = matchPifFileInDirectory(bridge);
        final File vlanfile = new File("/proc/net/vlan/" + pif);

        if (vlanfile.isFile()) {
            pif = Script.runSimpleBashScript("grep ^Device\\: /proc/net/vlan/" + pif + " | awk {'print $2'}");
        }

        return pif;
    }

    protected VifDriver getVifDriverClass(final String vifDriverClassName, final Map<String, Object> params) throws ConfigurationException {
        final VifDriver vifDriver;

        try {
            final Class<?> clazz = Class.forName(vifDriverClassName);
            vifDriver = (VifDriver) clazz.newInstance();
            vifDriver.configure(params);
        } catch (final ClassNotFoundException e) {
            throw new ConfigurationException("Unable to find class for libvirt.vif.driver " + e);
        } catch (final InstantiationException e) {
            throw new ConfigurationException("Unable to instantiate class for libvirt.vif.driver " + e);
        } catch (final IllegalAccessException e) {
            throw new ConfigurationException("Unable to instantiate class for libvirt.vif.driver " + e);
        }
        return vifDriver;
    }

    @Override
    public boolean stop() {
        try {
            final Connect conn = LibvirtConnection.getConnection();
            conn.close();

            Library.stopEventLoop();
        } catch (final LibvirtException e) {
            logger.trace("Ignoring libvirt error.", e);
        }

        return true;
    }

    public boolean passCmdLine(final String vmName, final String cmdLine) throws InternalErrorException {
        final Script command = new Script(this.sendConfigPropertiesPath, 5 * 1000, logger);
        Integer commandretries = 0;
        command.add("-n", vmName);
        command.add("-p", cmdLine.replaceAll(" ", "%"));

        // Since we return null on success (really!) we will just wait for that to happen
        while (command.execute() != null) {
            logger.debug("Got a timeout or an error, will try again.");
            commandretries++;
            if (commandretries > 100) {
                logger.debug("Giving up on cmdline passing after 100 times, aborting.");
                return false;
            }
            // Give it some time to get ready
            try {
                Thread.sleep(15000);
            } catch (final InterruptedException e) {
                logger.debug("Interrupted while awaiting next try: ", e);
            }
        }
        return true;
    }

    public boolean checkNetwork(final String networkName) {
        if (networkName == null) {
            return true;
        }

        if (getBridgeType() == OPENVSWITCH) {
            return checkOvsNetwork(networkName);
        } else {
            return checkBridgeNetwork(networkName);
        }
    }

    private boolean checkOvsNetwork(final String networkName) {
        logger.debug("Checking if network " + networkName + " exists as openvswitch bridge");
        if (networkName == null) {
            return true;
        }

        final Script command = new Script("/bin/sh", getScriptsTimeout());
        command.add("-c");
        command.add("ovs-vsctl br-exists " + networkName);
        return "0".equals(command.execute(null));
    }

    private boolean checkBridgeNetwork(final String networkName) {
        if (networkName == null) {
            return true;
        }

        final String name = matchPifFileInDirectory(networkName);

        if (name == null || name.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    // this is much like PrimaryStorageDownloadCommand, but keeping it separate
    public KvmPhysicalDisk templateToPrimaryDownload(final String templateUrl, final KvmStoragePool primaryPool,
                                                     final String volUuid) {
        final int index = templateUrl.lastIndexOf("/");
        final String mountpoint = templateUrl.substring(0, index);
        String templateName = null;
        if (index < templateUrl.length() - 1) {
            templateName = templateUrl.substring(index + 1);
        }

        KvmPhysicalDisk templateVol = null;
        KvmStoragePool secondaryPool = null;
        try {
            secondaryPool = this.storagePoolMgr.getStoragePoolByUri(mountpoint);
            /* Get template vol */
            if (templateName == null) {
                secondaryPool.refresh();
                final List<KvmPhysicalDisk> disks = secondaryPool.listPhysicalDisks();
                if (disks == null || disks.isEmpty()) {
                    logger.error("Failed to get volumes from pool: " + secondaryPool.getUuid());
                    return null;
                }
                for (final KvmPhysicalDisk disk : disks) {
                    if (disk.getName().endsWith("qcow2")) {
                        templateVol = disk;
                        break;
                    }
                }
                if (templateVol == null) {
                    logger.error("Failed to get template from pool: " + secondaryPool.getUuid());
                    return null;
                }
            } else {
                templateVol = secondaryPool.getPhysicalDisk(templateName);
            }

            /* Copy volume to primary storage */

            final KvmPhysicalDisk primaryVol = this.storagePoolMgr.copyPhysicalDisk(templateVol, volUuid, primaryPool, 0);
            return primaryVol;
        } catch (final CloudRuntimeException e) {
            logger.error("Failed to download template to primary storage", e);
            return null;
        } finally {
            if (secondaryPool != null) {
                this.storagePoolMgr.deleteStoragePool(secondaryPool.getType(), secondaryPool.getUuid());
            }
        }
    }

    public String getResizeScriptType(final KvmStoragePool pool, final KvmPhysicalDisk vol) {
        final StoragePoolType poolType = pool.getType();
        final PhysicalDiskFormat volFormat = vol.getFormat();

        if (pool.getType() == StoragePoolType.CLVM && volFormat == PhysicalDiskFormat.RAW) {
            return "CLVM";
        } else if ((poolType == StoragePoolType.NetworkFilesystem
                || poolType == StoragePoolType.SharedMountPoint
                || poolType == StoragePoolType.Filesystem
                || poolType == StoragePoolType.Gluster)
                && volFormat == PhysicalDiskFormat.QCOW2) {
            return "QCOW2";
        }
        throw new CloudRuntimeException("Cannot determine resize type from pool type " + pool.getType());
    }

    public PowerState getVmState(final Connect conn, final String vmName) {
        int retry = 3;
        Domain vms = null;
        while (retry-- > 0) {
            try {
                vms = conn.domainLookupByName(vmName);
                final PowerState s = convertToPowerState(vms.getInfo().state);
                return s;
            } catch (final LibvirtException e) {
                logger.warn("Can't get vm state " + vmName + e.getMessage() + "retry:" + retry);
            } finally {
                try {
                    if (vms != null) {
                        vms.free();
                    }
                } catch (final LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
        return PowerState.PowerOff;
    }

    protected PowerState convertToPowerState(final DomainState ps) {
        final PowerState state = s_powerStatesTable.get(ps);
        return state == null ? PowerState.PowerUnknown : state;
    }

    public long[] getNetworkStats(final String privateIp) {
        final String result = networkUsage(privateIp, "get", null);
        final long[] stats = new long[2];
        if (result != null) {
            final String[] splitResult = result.split(":");
            int index = 0;
            while (index < splitResult.length - 1) {
                stats[0] += Long.parseLong(splitResult[index++]);
                stats[1] += Long.parseLong(splitResult[index++]);
            }
        }
        return stats;
    }

    public long[] getVpcNetworkStats(final String privateIp, final String publicIp, final String option) {
        final String result = configureVpcNetworkUsage(privateIp, publicIp, option, null);
        final long[] stats = new long[2];
        if (result != null) {
            final String[] splitResult = result.split(":");
            int index = 0;
            while (index < splitResult.length - 1) {
                stats[0] += Long.parseLong(splitResult[index++]);
                stats[1] += Long.parseLong(splitResult[index++]);
            }
        }
        return stats;
    }

    public String configureVpcNetworkUsage(final String privateIpAddress, final String publicIp, final String option,
                                           final String vpcCidr) {
        final Script getUsage = new Script(this.routerProxyPath, logger);
        getUsage.add(VRScripts.VPC_NETUSAGE);
        getUsage.add(privateIpAddress);
        getUsage.add("-l", publicIp);

        if (option.equals("get")) {
            getUsage.add("-g");
        } else if (option.equals("create")) {
            getUsage.add("-c");
            getUsage.add("-v", vpcCidr);
        } else if (option.equals("reset")) {
            getUsage.add("-r");
        } else if (option.equals("vpn")) {
            getUsage.add("-n");
        } else if (option.equals("remove")) {
            getUsage.add("-d");
        }

        final OutputInterpreter.OneLineParser usageParser = new OutputInterpreter.OneLineParser();
        final String result = getUsage.execute(usageParser);
        if (result != null) {
            logger.debug("Failed to execute VPCNetworkUsage:" + result);
            return null;
        }
        return usageParser.getLine();
    }

    public void handleVmStartFailure(final LibvirtVmDef vm) {
        if (vm != null && vm.getDevices() != null) {
            cleanupVmNetworks(vm.getDevices().getInterfaces());
        }
    }

    private void cleanupVmNetworks(final List<InterfaceDef> nics) {
        if (nics != null) {
            for (final InterfaceDef nic : nics) {
                for (final VifDriver vifDriver : getAllVifDrivers()) {
                    vifDriver.unplug(nic);
                }
            }
        }
    }

    public LibvirtVmDef createVmFromSpec(final VirtualMachineTO vmTo) {
        final LibvirtVmDef vm = new LibvirtVmDef();
        vm.setDomainName(vmTo.getName());
        String uuid = vmTo.getUuid();
        uuid = getUuid(uuid);
        vm.setDomUuid(uuid);
        vm.setDomDescription("This VM is optimised for: " + vmTo.getOptimiseFor().toString());
        vm.setPlatformEmulator(vmTo.getPlatformEmulator());

        final MetadataTO metadataTo = vmTo.getMetadata();
        if (metadataTo != null) {
            final MetadataDef metadata = new MetadataDef();
            metadata.getNodes().put("domainUuid", metadataTo.getDomainUuid());
            vm.addComponent(metadata);
        }

        final GuestDef guest = new GuestDef();

        guest.setGuestType(LibvirtVmDef.GuestType.KVM);
        vm.setHvsType(HypervisorType.KVM.toString().toLowerCase());
        vm.setLibvirtVersion(this.hypervisorLibvirtVersion);
        vm.setQemuVersion(this.hypervisorQemuVersion);

        guest.setGuestArch(vmTo.getArch());
        guest.setMachineType("pc");
        guest.setUuid(uuid);
        guest.setManufacturer(vmTo.getManufacturer());
        guest.setBootOrder(GuestDef.BootOrder.CDROM);
        guest.setBootOrder(GuestDef.BootOrder.HARDISK);

        vm.addComponent(guest);

        final GuestResourceDef grd = new GuestResourceDef();

        if (vmTo.getMinRam() != vmTo.getMaxRam() && !this.libvirtComputingResourceProperties.getVmMemballoonDisable()) {
            grd.setMemBalloning(true);
            grd.setCurrentMem(vmTo.getMinRam() / 1024);
            grd.setMemorySize(vmTo.getMaxRam() / 1024);
        } else {
            grd.setMemorySize(vmTo.getMaxRam() / 1024);
        }
        final int vcpus = vmTo.getCpus();
        grd.setVcpuNum(vcpus);
        vm.addComponent(grd);

        final CpuModeDef cmd = new CpuModeDef();
        cmd.setMode(getGuestCpuMode());
        cmd.setModel(getGuestCpuModel());
        cmd.setCpuflags(vmTo.getCpuflags());
        if (vmTo.getType() == VirtualMachineType.User) {
            cmd.setFeatures(getCpuFeatures());
        }
        // multi cores per socket, for larger core configs
        if (vcpus % 6 == 0) {
            final int sockets = vcpus / 6;
            cmd.setTopology(6, sockets);
        } else if (vcpus % 4 == 0) {
            final int sockets = vcpus / 4;
            cmd.setTopology(4, sockets);
        }
        vm.addComponent(cmd);

        final CpuTuneDef ctd = new CpuTuneDef();
        if (VirtualMachineType.DomainRouter.equals(vmTo.getType())) {
            ctd.setShares(vmTo.getCpus() * this.libvirtComputingResourceProperties.getGuestCpuSharesRouter());
        } else {
            ctd.setShares(vmTo.getCpus() * this.libvirtComputingResourceProperties.getGuestCpuShares());
        }
        vm.addComponent(ctd);

        final FeaturesDef features = new FeaturesDef();
        features.addFeature("pae");
        features.addFeature("apic");
        features.addFeature("acpi");
        vm.addComponent(features);

        if (vmTo.getOptimiseFor() == OptimiseFor.Windows) {
            final HyperVEnlightenmentFeatureDef hyperVFeatures = new HyperVEnlightenmentFeatureDef();
            hyperVFeatures.addFeature("relaxed", true);
            hyperVFeatures.addFeature("vapic", true);
            hyperVFeatures.addFeature("spinlocks", true);
            hyperVFeatures.setRetries(8191);
            features.addHyperVFeature(hyperVFeatures);
        }

        final TermPolicy term = new TermPolicy();
        if (VirtualMachineType.DomainRouter.equals(vmTo.getType())) {
            term.setCrashPolicy(getRouterTermpolicyCrash());
            term.setPowerOffPolicy(getRouterTermpolicyPowerOff());
            term.setRebootPolicy(getRouterTermpolicyReboot());
        } else {
            term.setCrashPolicy(getVmTermpolicyCrash());
            term.setPowerOffPolicy(getVmTermpolicyPowerOff());
            term.setRebootPolicy(getVmTermpolicyReboot());
        }
        vm.addComponent(term);

        final ClockDef clock = new ClockDef();
        Boolean kvmClockEnabled = !isKvmClockDisabled();
        Boolean hypervClockEnabled = false;
        if (vmTo.getOptimiseFor() == OptimiseFor.Windows) {
            clock.setClockOffset(ClockDef.ClockOffset.LOCALTIME);
            kvmClockEnabled = false;
            hypervClockEnabled = true;
        }
        clock.addTimer("kvmclock", null, kvmClockEnabled);
        clock.addTimer("hypervclock", null, hypervClockEnabled);

        // Recommended default clock/timer settings - https://bugzilla.redhat.com/show_bug.cgi?id=1053847
        clock.addTimer("rtc", "catchup");
        clock.addTimer("pit", "delay");
        clock.addTimer("hpet", null, false);
        vm.addComponent(clock);

        final DevicesDef devices = new DevicesDef();
        devices.setEmulatorPath(this.hypervisorPath);
        devices.setGuestType(guest.getGuestType());

        final SerialDef serial = new SerialDef("pty", null, (short) 0);
        devices.addDevice(serial);

        final QemuGuestAgentDef guestagent = new QemuGuestAgentDef();
        devices.addDevice(guestagent);

        if (this.libvirtComputingResourceProperties.getVmRngEnable()) {
            final RngDef rngDevice = new RngDef(this.rngPath, this.rngBackendModel);
            devices.addDevice(rngDevice);
        }

        final WatchDogDef watchDog = new WatchDogDef(this.libvirtComputingResourceProperties.getVmWatchdogAction(), this.libvirtComputingResourceProperties.getVmWatchdogModel());
        devices.addDevice(watchDog);

        final VideoDef videoCard = new VideoDef(this.libvirtComputingResourceProperties.getVmVideoHardware(), this.libvirtComputingResourceProperties.getVmVideoRam());
        devices.addDevice(videoCard);

        final ConsoleDef console = new ConsoleDef("pty", null, null, (short) 0);
        devices.addDevice(console);

        // add the VNC port passwd here, get the passwd from the vmInstance.
        final String passwd = vmTo.getVncPassword();
        final GraphicDef grap = new GraphicDef("vnc", (short) 0, true, passwd, null);
        devices.addDevice(grap);

        final InputDef input = new InputDef("tablet", "usb");
        devices.addDevice(input);

        // Always add a virtio scsi controller
        vmTo.getName();
        final ScsiDef sd = new ScsiDef((short) 0, 0, 0, 9, 0);
        devices.addDevice(sd);
        logger.debug("Adding SCSI definition for " + vmTo.getName() + ":\n" + sd.toString());

        vm.addComponent(devices);

        return vm;
    }

    private boolean isKvmClockDisabled() {
        return this.libvirtComputingResourceProperties.isKvmClockDisable();
    }

    private String getGuestCpuModel() {
        return this.libvirtComputingResourceProperties.getGuestCpuModel();
    }

    private String getGuestCpuMode() {
        return this.libvirtComputingResourceProperties.getGuestCpuMode();
    }

    protected String getUuid(String uuid) {
        if (uuid == null) {
            uuid = randomUUID().toString();
        } else {
            try {
                final UUID uuid2 = UUID.fromString(uuid);
                final String uuid3 = uuid2.toString();
                if (!uuid3.equals(uuid)) {
                    uuid = randomUUID().toString();
                }
            } catch (final IllegalArgumentException e) {
                uuid = randomUUID().toString();
            }
        }
        return uuid;
    }

    boolean isGuestVirtIoCapable(final String guestOsName) {
        final DiskControllerType db = DiskControllerType.getGuestDiskModel(guestOsName);
        return db != DiskControllerType.IDE;
    }

    public void createVifs(final VirtualMachineTO vmSpec, final LibvirtVmDef vm)
            throws InternalErrorException, LibvirtException {
        final Map<String, String> params = vmSpec.getDetails();
        String nicAdapter = "";
        if (params != null && params.get("nicAdapter") != null && !params.get("nicAdapter").isEmpty()) {
            nicAdapter = params.get("nicAdapter");
        }
        for (final NicTO nic : vmSpec.getNics()) {
            createVif(vm, nic, nicAdapter);
        }
    }

    private void createVif(final LibvirtVmDef vm, final NicTO nic, final String nicAdapter)
            throws InternalErrorException, LibvirtException {

        if (nic.getType().equals(TrafficType.Guest) && nic.getBroadcastType().equals(BroadcastDomainType.Vsp)) {
            final String vrIp = nic.getBroadcastUri().getPath().substring(1);

            if (logger.isDebugEnabled()) {
                logger.debug("NIC with MAC " + nic.getMac() + " and BroadcastDomainType " + nic.getBroadcastType()
                        + " in network(" + nic.getGateway() + "/" + nic.getNetmask() + ") is " + nic.getType()
                        + " traffic type. So, vsp-vr-ip " + vrIp + " is set in the metadata");
            }
        }

        vm.getDevices().addDevice(
                getVifDriver(nic.getType()).plug(nic, vm.getPlatformEmulator().toString(), nicAdapter).toString());
    }

    public String getVolumePath(final Connect conn, final DiskTO volume) throws LibvirtException, URISyntaxException {
        final DataTO data = volume.getData();
        final DataStoreTO store = data.getDataStore();

        if (volume.getType() == VolumeType.ISO && data.getPath() != null) {
            final NfsTO nfsStore = (NfsTO) store;
            final String isoPath = nfsStore.getUrl() + File.separator + data.getPath();
            final int index = isoPath.lastIndexOf("/");
            final String path = isoPath.substring(0, index);
            final String name = isoPath.substring(index + 1);
            final KvmStoragePool secondaryPool = this.storagePoolMgr.getStoragePoolByUri(path);
            final KvmPhysicalDisk isoVol = secondaryPool.getPhysicalDisk(name);
            return isoVol.getPath();
        } else {
            return data.getPath();
        }
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingRoutingCommand(HostType.Routing, id, this.getHostVmStateReport());
    }

    public void createVbd(final Connect conn, final VirtualMachineTO vmSpec, final String vmName, final LibvirtVmDef vm)
            throws InternalErrorException, LibvirtException, URISyntaxException {
        final List<DiskTO> disks = Arrays.asList(vmSpec.getDisks());
        Collections.sort(disks, new Comparator<DiskTO>() {
            @Override
            public int compare(final DiskTO arg0, final DiskTO arg1) {
                return arg0.getDiskSeq() > arg1.getDiskSeq() ? 1 : -1;
            }
        });

        for (final DiskTO volume : disks) {
            KvmPhysicalDisk physicalDisk = null;
            KvmStoragePool pool = null;
            final DataTO data = volume.getData();
            if (volume.getType() == VolumeType.ISO && data.getPath() != null) {
                final NfsTO nfsStore = (NfsTO) data.getDataStore();
                final String volPath = nfsStore.getUrl() + File.separator + data.getPath();
                final int index = volPath.lastIndexOf("/");
                final String volDir = volPath.substring(0, index);
                final String volName = volPath.substring(index + 1);
                final KvmStoragePool secondaryStorage = this.storagePoolMgr.getStoragePoolByUri(volDir);
                physicalDisk = secondaryStorage.getPhysicalDisk(volName);
            } else if (volume.getType() != VolumeType.ISO) {
                final PrimaryDataStoreTO store = (PrimaryDataStoreTO) data.getDataStore();
                physicalDisk = this.storagePoolMgr.getPhysicalDisk(store.getPoolType(), store.getUuid(), data.getPath());
                pool = physicalDisk.getPool();
            }

            String volPath = null;
            if (physicalDisk != null) {
                volPath = physicalDisk.getPath();
            }

            // check for disk activity, if detected we should exit because vm is running elsewhere
            if (this.diskActivityCheckEnabled && physicalDisk != null && physicalDisk.getFormat() == PhysicalDiskFormat.QCOW2) {
                logger.debug("Checking physical disk file at path " + volPath
                        + " for disk activity to ensure vm is not running elsewhere");
                try {
                    HypervisorUtils.checkVolumeFileForActivity(volPath, this.diskActivityCheckTimeoutSeconds,
                            this.diskActivityInactiveThresholdMilliseconds, this.diskActivityCheckFileSizeMin);
                } catch (final IOException ex) {
                    throw new CloudRuntimeException("Unable to check physical disk file for activity", ex);
                }
                logger.debug("Disk activity check cleared");
            }

            final LibvirtDiskDef disk = new LibvirtDiskDef();
            if (volume.getType() == VolumeType.ISO) {
                if (volPath == null) {
                    /* Add iso as placeholder */
                    disk.defIsoDisk(null);
                } else {
                    disk.defIsoDisk(volPath);
                }
            } else {
                final int devId = volume.getDiskSeq().intValue();

                if (volume.getDiskController() == DiskControllerType.SCSI) {
                    disk.setQemuDriver(true);
                    disk.setDiscard(DiscardType.UNMAP);
                }

                if (pool.getType() == StoragePoolType.RBD) {
                    /*
                     * For RBD pools we use the secret mechanism in libvirt. We store the secret under the UUID of the pool,
                     * that's why we pass the pool's UUID as the authSecret
                     */
                    disk.defNetworkBasedDisk(physicalDisk.getPath().replace("rbd:", ""), pool.getSourceHost(),
                            pool.getSourcePort(), pool.getAuthUserName(),
                            pool.getUuid(), devId, volume.getDiskController(), DiskProtocol.RBD, LibvirtDiskDef.DiskFmtType.RAW);
                } else if (pool.getType() == StoragePoolType.Gluster) {
                    final String mountpoint = pool.getLocalPath();
                    final String path = physicalDisk.getPath();
                    final String glusterVolume = pool.getSourceDir().replace("/", "");
                    disk.defNetworkBasedDisk(glusterVolume + path.replace(mountpoint, ""), pool.getSourceHost(),
                            pool.getSourcePort(), null,
                            null, devId, volume.getDiskController(), DiskProtocol.GLUSTER, LibvirtDiskDef.DiskFmtType.QCOW2);
                } else if (pool.getType() == StoragePoolType.CLVM || physicalDisk.getFormat() == PhysicalDiskFormat.RAW) {
                    disk.defBlockBasedDisk(physicalDisk.getPath(), devId, volume.getDiskController());
                } else {
                    disk.defFileBasedDisk(physicalDisk.getPath(), devId, volume.getDiskController(), LibvirtDiskDef.DiskFmtType.QCOW2);
                }
            }

            if (data instanceof VolumeObjectTO) {
                final VolumeObjectTO volumeObjectTo = (VolumeObjectTO) data;
                disk.setSerial(volumeObjectTo.getDeviceId() + "-" + diskUuidToSerial(volumeObjectTo.getUuid()));
                disk.setDeviceId(volumeObjectTo.getDeviceId().intValue());
                if (volumeObjectTo.getBytesReadRate() != null && volumeObjectTo.getBytesReadRate() > 0) {
                    disk.setBytesReadRate(volumeObjectTo.getBytesReadRate());
                }
                if (volumeObjectTo.getBytesWriteRate() != null && volumeObjectTo.getBytesWriteRate() > 0) {
                    disk.setBytesWriteRate(volumeObjectTo.getBytesWriteRate());
                }
                if (volumeObjectTo.getIopsReadRate() != null && volumeObjectTo.getIopsReadRate() > 0) {
                    disk.setIopsReadRate(volumeObjectTo.getIopsReadRate());
                }
                if (volumeObjectTo.getIopsWriteRate() != null && volumeObjectTo.getIopsWriteRate() > 0) {
                    disk.setIopsWriteRate(volumeObjectTo.getIopsWriteRate());
                }
                if (volumeObjectTo.getCacheMode() != null) {
                    disk.setCacheMode(DiskCacheMode.valueOf(volumeObjectTo.getCacheMode().toString().toUpperCase()));
                }
            }
            logger.debug("Adding disk: " + disk.toString());
            vm.getDevices().addDevice(disk);
        }

        if (vmSpec.getType() != VirtualMachineType.User) {
            final String sysvmIsoPath = getSysvmIsoPath();
            if (sysvmIsoPath != null) {
                final LibvirtDiskDef iso = new LibvirtDiskDef();
                iso.defIsoDisk(sysvmIsoPath);
                vm.getDevices().addDevice(iso);
            }
        }
    }

    private String getSysvmIsoPath() {
        return this.libvirtComputingResourceProperties.getSystemvmIsoPath();
    }

    @Override
    public HostType getType() {
        return HostType.Routing;
    }

    private Map<String, String> getVersionStrings() {
        final Script command = new Script(this.versionstringpath, getScriptsTimeout(), logger);
        final KeyValueInterpreter kvi = new KeyValueInterpreter();
        final String result = command.execute(kvi);
        if (result == null) {
            return kvi.getKeyValues();
        } else {
            return new HashMap<>(1);
        }
    }

    public String diskUuidToSerial(final String uuid) {
        final String uuidWithoutHyphen = uuid.replace("-", "");
        return uuidWithoutHyphen.substring(0, Math.min(uuidWithoutHyphen.length(), 20));
    }

    @Override
    public StartupCommand[] initialize() {

        final List<Object> info = getHostInfo();
        this.totalMemory = (Long) info.get(1);

        final StartupRoutingCommand cmd = new StartupRoutingCommand((Integer) info.get(0), (Long) info.get(1), (Long) info.get(3), (String) info.get(2), getHypervisorType(),
                RouterPrivateIpStrategy.HostLocal);
        cmd.setCpuSockets((Integer) info.get(4));
        fillNetworkInformation(cmd);
        this.privateIp = cmd.getPrivateIpAddress();
        cmd.getHostDetails().putAll(getVersionStrings());
        cmd.setPool(getPool());
        cmd.setCluster(getCluster());
        cmd.setGatewayIpAddress(this.localGateway);
        cmd.setIqn(getIqn());
        cmd.setVersion(LibvirtComputingResource.class.getPackage().getImplementationVersion());

        StartupStorageCommand sscmd = null;
        try {

            final String localStoragePath = getLocalStoragePath();
            final KvmStoragePool localStoragePool = this.storagePoolMgr.createStoragePool(getLocalStorageUuid(), "localhost", -1,
                    localStoragePath, "", StoragePoolType.Filesystem);
            final StoragePoolInfo pi = new StoragePoolInfo(localStoragePool.getUuid(),
                    cmd.getPrivateIpAddress(), localStoragePath, localStoragePath,
                    StoragePoolType.Filesystem, localStoragePool.getCapacity(), localStoragePool.getAvailable());

            sscmd = new StartupStorageCommand();
            sscmd.setPoolInfo(pi);
            sscmd.setGuid(pi.getUuid());
            sscmd.setDataCenter(getZone());
            sscmd.setResourceType(StorageResourceType.STORAGE_POOL);
        } catch (final CloudRuntimeException e) {
            logger.debug("Unable to initialize local storage pool: " + e);
        }

        if (sscmd != null) {
            return new StartupCommand[]{cmd, sscmd};
        } else {
            return new StartupCommand[]{cmd};
        }
    }

    private String getZone() {
        return this.libvirtComputingResourceProperties.getZone();
    }

    private String getLocalStoragePath() {
        return this.libvirtComputingResourceProperties.getLocalStoragePath();
    }

    private String getLocalStorageUuid() {
        return this.libvirtComputingResourceProperties.getLocalStorageUuid();
    }

    private String getCluster() {
        return this.libvirtComputingResourceProperties.getCluster();
    }

    private String getPool() {
        return this.libvirtComputingResourceProperties.getPool();
    }

    public synchronized String attachOrDetachIso(final Connect conn, final String vmName, String isoPath,
                                                 final boolean isAttach) throws LibvirtException, URISyntaxException,
            InternalErrorException {
        String isoXml = null;
        if (isoPath != null && isAttach) {
            final int index = isoPath.lastIndexOf("/");
            final String path = isoPath.substring(0, index);
            final String name = isoPath.substring(index + 1);
            final KvmStoragePool secondaryPool = this.storagePoolMgr.getStoragePoolByUri(path);
            final KvmPhysicalDisk isoVol = secondaryPool.getPhysicalDisk(name);
            isoPath = isoVol.getPath();

            final LibvirtDiskDef iso = new LibvirtDiskDef();
            iso.defIsoDisk(isoPath);
            isoXml = iso.toString();
        } else {
            final LibvirtDiskDef iso = new LibvirtDiskDef();
            iso.defIsoDisk(null);
            isoXml = iso.toString();
        }

        final List<LibvirtDiskDef> disks = getDisks(conn, vmName);
        final String result = attachOrDetachDevice(conn, true, vmName, isoXml);
        if (result == null && !isAttach) {
            for (final LibvirtDiskDef disk : disks) {
                if (disk.getDeviceType() == LibvirtDiskDef.DeviceType.CDROM) {
                    cleanupDisk(disk);
                }
            }
        }
        return result;
    }

    private String getIqn() {
        try {
            final String textToFind = "InitiatorName=";

            final Script iScsiAdmCmd = new Script(true, "grep", 0, logger);

            iScsiAdmCmd.add(textToFind);
            iScsiAdmCmd.add("/etc/iscsi/initiatorname.iscsi");

            final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();

            final String result = iScsiAdmCmd.execute(parser);

            if (result != null) {
                return null;
            }

            final String textFound = parser.getLine().trim();

            return textFound.substring(textToFind.length());
        } catch (final Exception ex) {
            return null;
        }
    }

    public List<LibvirtDiskDef> getDisks(final Connect conn, final String vmName) {
        final LibvirtDomainXmlParser parser = new LibvirtDomainXmlParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
            parser.parseDomainXml(dm.getXMLDesc(0));
            return parser.getDisks();
        } catch (final LibvirtException e) {
            logger.debug("Failed to get dom xml: " + e.toString());
            return new ArrayList<>();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (final LibvirtException e) {
                logger.trace("Ignoring libvirt error.", e);
            }
        }
    }

    protected synchronized String attachOrDetachDevice(final Connect conn, final boolean attach, final String vmName,
                                                       final String xml) throws LibvirtException, InternalErrorException {
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
            if (attach) {
                logger.debug("Attaching device: " + xml);
                dm.attachDevice(xml);
            } else {
                logger.debug("Detaching device: " + xml);
                dm.detachDevice(xml);
            }
        } catch (final LibvirtException e) {
            if (attach) {
                logger.warn("Failed to attach device to " + vmName + ": " + e.getMessage());
            } else {
                logger.warn("Failed to detach device from " + vmName + ": " + e.getMessage());
            }
            throw e;
        } finally {
            if (dm != null) {
                try {
                    dm.free();
                } catch (final LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                }
            }
        }

        return null;
    }

    private HashMap<String, HostVmStateReportEntry> getHostVmStateReport() {
        final HashMap<String, HostVmStateReportEntry> vmStates = new HashMap<>();
        Connect conn = null;

        if (getHypervisorType() == HypervisorType.KVM) {
            try {
                conn = LibvirtConnection.getConnectionByType(HypervisorType.KVM.toString());
                vmStates.putAll(getHostVmStateReport(conn));
            } catch (final LibvirtException e) {
                logger.debug("Failed to get connection: " + e.getMessage());
            }
        }

        return vmStates;
    }

    public boolean cleanupDisk(final LibvirtDiskDef disk) {
        final String path = disk.getDiskPath();

        if (path == null) {
            logger.debug("Unable to clean up disk with null path (perhaps empty cdrom drive):" + disk);
            return false;
        }

        if (path.endsWith("systemvm.iso")) {
            // don't need to clean up system vm ISO as it's stored in local
            return true;
        }

        return this.storagePoolMgr.disconnectPhysicalDiskByPath(path);
    }

    private HashMap<String, HostVmStateReportEntry> getHostVmStateReport(final Connect conn) {
        final HashMap<String, HostVmStateReportEntry> vmStates = new HashMap<>();

        String[] vms = null;
        int[] ids = null;

        try {
            ids = conn.listDomains();
        } catch (final LibvirtException e) {
            logger.warn("Unable to listDomains", e);
            return null;
        }
        try {
            vms = conn.listDefinedDomains();
        } catch (final LibvirtException e) {
            logger.warn("Unable to listDomains", e);
            return null;
        }

        Domain dm = null;
        for (final int id : ids) {
            try {
                dm = conn.domainLookupByID(id);

                final DomainState ps = dm.getInfo().state;

                final PowerState state = convertToPowerState(ps);

                logger.trace("VM " + dm.getName() + ": powerstate = " + ps + "; vm state=" + state.toString());
                final String vmName = dm.getName();
                vmStates.put(vmName, new HostVmStateReportEntry(state, conn.getHostName()));
            } catch (final LibvirtException e) {
                logger.warn("Unable to get vms", e);
            } finally {
                try {
                    if (dm != null) {
                        dm.free();
                    }
                } catch (final LibvirtException e) {
                    logger.trace("Ignoring libvirt error.", e);
                }
            }
        }

        for (final String vm : vms) {
            try {

                dm = conn.domainLookupByName(vm);

                final DomainState ps = dm.getInfo().state;
                final PowerState state = convertToPowerState(ps);
                final String vmName = dm.getName();
                logger.trace("VM " + vmName + ": powerstate = " + ps + "; vm state=" + state.toString());
                vmStates.put(vmName, new HostVmStateReportEntry(state, conn.getHostName()));
            } catch (final LibvirtException e) {
                logger.warn("Unable to get vms", e);
            } finally {
                try {
                    if (dm != null) {
                        dm.free();
                    }
                } catch (final LibvirtException e) {
                    logger.trace("Ignoring libvirt error.", e);
                }
            }
        }

        return vmStates;
    }

    protected List<Object> getHostInfo() {
        final ArrayList<Object> info = new ArrayList<>();
        long cpus = 0;
        long ram = 0;
        int cpuSockets = 0;
        String cap = null;
        try {
            final Connect conn = LibvirtConnection.getConnection();
            final NodeInfo hosts = conn.nodeInfo();

            /*
             * Some CPUs report a single socket and multiple NUMA cells.
             * We need to multiply them to get the correct socket count.
             */
            cpuSockets = hosts.sockets;
            if (hosts.nodes > 0) {
                cpuSockets = hosts.sockets * hosts.nodes;
            }
            cpus = hosts.cpus;
            ram = hosts.memory * 1024L;
            final LibvirtCapXmlParser parser = new LibvirtCapXmlParser();
            parser.parseCapabilitiesXml(conn.getCapabilities());
            final ArrayList<String> oss = parser.getGuestOsType();
            for (final String s : oss) {
                /*
                 * Even host supports guest os type more than hvm, we only report hvm to management server
                 */
                if (s.equalsIgnoreCase("hvm")) {
                    cap = "hvm";
                }
            }
        } catch (final LibvirtException e) {
            logger.trace("Ignoring libvirt error.", e);
        }

        if (isSnapshotSupported()) {
            cap = cap + ",snapshot";
        }

        info.add((int) cpus);
        // Report system's RAM as actual RAM minus host OS reserved RAM
        final long dom0MinMem = getHostReservedMemMb();
        ram = ram - dom0MinMem;
        info.add(ram);
        info.add(cap);
        info.add(dom0MinMem);
        info.add(cpuSockets);
        logger.debug("cpus=" + cpus + ", ram=" + ram + ", _dom0MinMem=" + dom0MinMem + ", cpu sockets=" + cpuSockets);

        return info;
    }

    private long getHostReservedMemMb() {
        return this.libvirtComputingResourceProperties.getHostReservedMemMb();
    }

    protected static long getCpuSpeed(final NodeInfo nodeInfo) {
        try (final Reader reader = new FileReader(
                "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")) {
            return Long.parseLong(IOUtils.toString(reader).trim()) / 1000;
        } catch (final IOException | NumberFormatException e) {
            logger.warn("Could not read cpuinfo_max_freq");
            return nodeInfo.mhz;
        }
    }

    public String rebootVm(final Connect conn, final String vmName) {
        Domain dm = null;
        String msg = null;
        try {
            dm = conn.domainLookupByName(vmName);
            // Get XML Dump including the secure information such as VNC password
            // By passing 1, or VIR_DOMAIN_XML_SECURE flag
            // https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainXMLFlags
            String vmDef = dm.getXMLDesc(1);
            final LibvirtDomainXmlParser parser = new LibvirtDomainXmlParser();
            parser.parseDomainXml(vmDef);
            for (final InterfaceDef nic : parser.getInterfaces()) {
                if (nic.getNetType() == GuestNetType.BRIDGE && nic.getBrName().startsWith("cloudVirBr")) {
                    try {
                        final int vnetId = Integer.parseInt(nic.getBrName().replaceFirst("cloudVirBr", ""));
                        final String pifName = getPif(getGuestBridgeName());
                        final String newBrName = "br" + pifName + "-" + vnetId;
                        vmDef = vmDef.replaceAll("'" + nic.getBrName() + "'", "'" + newBrName + "'");
                        logger.debug("VM bridge name is changed from " + nic.getBrName() + " to " + newBrName);
                    } catch (final NumberFormatException e) {
                        continue;
                    }
                }
            }
            logger.debug(vmDef);
            msg = stopVm(conn, vmName, false);
            msg = startVm(conn, vmName, vmDef);
            return null;
        } catch (final LibvirtException | InternalErrorException e) {
            logger.warn("Failed to create vm", e);
            msg = e.getMessage();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (final LibvirtException e) {
                logger.trace("Ignoring libvirt error.", e);
            }
        }

        return msg;
    }

    public String stopVm(final Connect conn, final String vmName, final boolean forceStop) {
        DomainState state = null;
        Domain dm = null;

        // delete the metadata of vm snapshots before stopping
        try {
            dm = conn.domainLookupByName(vmName);
            cleanVMSnapshotMetadata(dm);
        } catch (final LibvirtException e) {
            logger.debug("Failed to get vm :" + e.getMessage());
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (final LibvirtException l) {
                logger.trace("Ignoring libvirt error.", l);
            }
        }

        logger.debug("Try to stop the vm at first");
        String ret = stopVmInternal(conn, vmName, forceStop);
        if (ret == Script.ERR_TIMEOUT) {
            ret = stopVmInternal(conn, vmName, true);
        } else if (ret != null) {
            /*
             * There is a race condition between libvirt and qemu: libvirt listens on qemu's monitor fd. If qemu is shutdown,
             * while libvirt is reading on the fd, then libvirt will report an error.
             */
            /* Retry 3 times, to make sure we can get the vm's status */
            for (int i = 0; i < 3; i++) {
                try {
                    dm = conn.domainLookupByName(vmName);
                    state = dm.getInfo().state;
                    break;
                } catch (final LibvirtException e) {
                    logger.debug("Failed to get vm status:" + e.getMessage());
                } finally {
                    try {
                        if (dm != null) {
                            dm.free();
                        }
                    } catch (final LibvirtException l) {
                        logger.trace("Ignoring libvirt error.", l);
                    }
                }
            }

            if (state == null) {
                logger.debug("Can't get vm's status, assume it's dead already");
                return null;
            }

            if (state != DomainState.VIR_DOMAIN_SHUTOFF) {
                logger.debug("Try to destroy the vm");
                ret = stopVmInternal(conn, vmName, forceStop);
                if (ret != null) {
                    return ret;
                }
            }
        }

        return null;
    }

    public String startVm(final Connect conn, final String vmName, final String domainXml)
            throws LibvirtException, InternalErrorException {
        try {
            /*
             * We create a transient domain here. When this method gets called we receive a full XML specification of the
             * guest, so no need to define it persistent.
             *
             * This also makes sure we never have any old "garbage" defined in libvirt which might haunt us.
             */

            // check for existing inactive vm definition and remove it
            // this can sometimes happen during crashes, etc
            Domain dm = null;
            try {
                dm = conn.domainLookupByName(vmName);
                if (dm != null && dm.isPersistent() == 1) {
                    // this is safe because it doesn't stop running VMs
                    dm.undefine();
                }
            } catch (final LibvirtException e) {
                // this is what we want, no domain found
            } finally {
                if (dm != null) {
                    dm.free();
                }
            }

            conn.domainCreateXML(domainXml, 0);
        } catch (final LibvirtException e) {
            throw e;
        }
        return null;
    }

    protected String stopVmInternal(final Connect conn, final String vmName, final boolean force) {
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
            final int persist = dm.isPersistent();
            if (force) {
                if (dm.isActive() == 1) {
                    dm.destroy();
                    if (persist == 1) {
                        dm.undefine();
                    }
                }
            } else {
                if (dm.isActive() == 0) {
                    return null;
                }
                dm.shutdown();
                int retry = getStopScriptTimeout() / 2000;
                /*
                 * Wait for the domain gets into shutoff state. When it does the dm object will no longer work, so we need to
                 * catch it.
                 */
                try {
                    while (dm.isActive() == 1 && retry >= 0) {
                        Thread.sleep(2000);
                        retry--;
                    }
                } catch (final LibvirtException e) {
                    final String error = e.toString();
                    if (error.contains("Domain not found")) {
                        logger.debug("successfully shut down vm " + vmName);
                    } else {
                        logger.debug("Error in waiting for vm shutdown:" + error);
                    }
                }
                if (retry < 0) {
                    logger.warn("Timed out waiting for domain " + vmName + " to shutdown gracefully");
                    return Script.ERR_TIMEOUT;
                } else {
                    if (persist == 1) {
                        dm.undefine();
                    }
                }
            }
        } catch (final LibvirtException e) {
            if (e.getMessage().contains("Domain not found")) {
                logger.debug("VM " + vmName + " doesn't exist, no need to stop it");
                return null;
            }
            logger.debug("Failed to stop VM :" + vmName + " :", e);
            return e.getMessage();
        } catch (final InterruptedException ie) {
            logger.debug("Interrupted sleep");
            return ie.getMessage();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (final LibvirtException e) {
                logger.trace("Ignoring libvirt error.", e);
            }
        }

        return null;
    }

    public List<Ternary<String, Boolean, String>> cleanVMSnapshotMetadata(final Domain dm) throws LibvirtException {
        logger.debug("Cleaning the metadata of vm snapshots of vm " + dm.getName());
        final List<Ternary<String, Boolean, String>> vmsnapshots = new ArrayList<>();
        if (dm.snapshotNum() == 0) {
            return vmsnapshots;
        }
        String currentSnapshotName = null;
        try {
            final DomainSnapshot snapshotCurrent = dm.snapshotCurrent();
            final String snapshotXML = snapshotCurrent.getXMLDesc();
            snapshotCurrent.free();
            try {
                final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                final InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(snapshotXML));
                final Document doc = builder.parse(is);
                final Element rootElement = doc.getDocumentElement();

                currentSnapshotName = getTagValue("name", rootElement);
            } catch (final ParserConfigurationException | SAXException | IOException e) {
                logger.debug(e.toString());
            }
        } catch (final LibvirtException e) {
            logger.debug("Fail to get the current vm snapshot for vm: " + dm.getName() + ", continue");
        }
        final int flags = 2; // VIR_DOMAIN_SNAPSHOT_DELETE_METADATA_ONLY = 2
        final String[] snapshotNames = dm.snapshotListNames();
        Arrays.sort(snapshotNames);
        for (final String snapshotName : snapshotNames) {
            final DomainSnapshot snapshot = dm.snapshotLookupByName(snapshotName);
            final Boolean isCurrent = currentSnapshotName != null && currentSnapshotName.equals(snapshotName);
            vmsnapshots.add(new Ternary<>(snapshotName, isCurrent, snapshot.getXMLDesc()));
        }
        for (final String snapshotName : snapshotNames) {
            final DomainSnapshot snapshot = dm.snapshotLookupByName(snapshotName);
            snapshot.delete(flags); // clean metadata of vm snapshot
        }
        return vmsnapshots;
    }

    private static String getTagValue(final String tag, final Element eElement) {
        final NodeList nlList = eElement.getElementsByTagName(tag).item(0).getChildNodes();
        final Node nValue = nlList.item(0);

        return nValue.getNodeValue();
    }

    public void restoreVMSnapshotMetadata(final Domain dm, final String vmName, final List<Ternary<String, Boolean, String>> vmsnapshots) {
        logger.debug("Restoring the metadata of vm snapshots of vm " + vmName);
        for (final Ternary<String, Boolean, String> vmsnapshot : vmsnapshots) {
            final String snapshotName = vmsnapshot.first();
            final Boolean isCurrent = vmsnapshot.second();
            final String snapshotXML = vmsnapshot.third();
            logger.debug("Restoring vm snapshot " + snapshotName + " on " + vmName + " with XML:\n " + snapshotXML);
            try {
                int flags = 1; // VIR_DOMAIN_SNAPSHOT_CREATE_REDEFINE = 1
                if (isCurrent) {
                    flags += 2; // VIR_DOMAIN_SNAPSHOT_CREATE_CURRENT = 2
                }
                dm.snapshotCreateXML(snapshotXML, flags);
            } catch (final LibvirtException e) {
                logger.debug("Failed to restore vm snapshot " + snapshotName + ", continue");
            }
        }
    }

    private int getStopScriptTimeout() {
        return this.libvirtComputingResourceProperties.getStopScriptTimeout();
    }

    public Integer getVncPort(final Connect conn, final String vmName) throws LibvirtException {
        final LibvirtDomainXmlParser parser = new LibvirtDomainXmlParser();
        Domain dm = null;
        try {
            dm = conn.domainLookupByName(vmName);
            final String xmlDesc = dm.getXMLDesc(0);
            parser.parseDomainXml(xmlDesc);
            return parser.getVncPort();
        } finally {
            try {
                if (dm != null) {
                    dm.free();
                }
            } catch (final LibvirtException l) {
                logger.trace("Ignoring libvirt error.", l);
            }
        }
    }

    public List<VmDiskStatsEntry> getVmDiskStat(final Connect conn, final String vmName) throws LibvirtException {
        Domain dm = null;
        try {
            dm = getDomain(conn, vmName);

            final List<VmDiskStatsEntry> stats = new ArrayList<>();

            final List<LibvirtDiskDef> disks = getDisks(conn, vmName);

            for (final LibvirtDiskDef disk : disks) {
                if (disk.getDeviceType() != DeviceType.DISK) {
                    break;
                }
                final DomainBlockStats blockStats = dm.blockStats(disk.getDiskLabel());
                final String path = disk.getDiskPath(); // for example, path = /mnt/pool_uuid/disk_path/
                String diskPath = null;
                if (path != null) {
                    final String[] token = path.split("/");
                    if (token.length > 3) {
                        diskPath = token[3];
                        final VmDiskStatsEntry stat = new VmDiskStatsEntry(vmName, diskPath, blockStats.wr_req, blockStats.rd_req,
                                blockStats.wr_bytes, blockStats.rd_bytes);
                        stats.add(stat);
                    }
                }
            }

            return stats;
        } finally {
            if (dm != null) {
                dm.free();
            }
        }
    }

    public VmStatsEntry getVmStat(final Connect conn, final String vmName) throws LibvirtException {
        Domain dm = null;
        try {
            dm = getDomain(conn, vmName);
            final DomainInfo info = dm.getInfo();

            final VmStatsEntry stats = new VmStatsEntry();
            stats.setNumCPUs(info.nrVirtCpu);
            stats.setEntityType("vm");

            /* get cpu utilization */
            VmStats oldStats = null;

            final Calendar now = Calendar.getInstance();

            oldStats = this.vmStats.get(vmName);

            long elapsedTime = 0;
            if (oldStats != null) {
                elapsedTime = now.getTimeInMillis() - oldStats.timestamp.getTimeInMillis();
                double utilization = (info.cpuTime - oldStats.usedTime) / ((double) elapsedTime * 1000000);

                final NodeInfo node = conn.nodeInfo();
                utilization = utilization / node.cpus;
                if (utilization > 0) {
                    stats.setCPUUtilization(utilization * 100);
                }
            }

            /* get network stats */

            final List<InterfaceDef> vifs = getInterfaces(conn, vmName);
            long rx = 0;
            long tx = 0;
            for (final InterfaceDef vif : vifs) {
                final DomainInterfaceStats ifStats = dm.interfaceStats(vif.getDevName());
                rx += ifStats.rx_bytes;
                tx += ifStats.tx_bytes;
            }

            if (oldStats != null) {
                final double deltarx = rx - oldStats.rx;
                if (deltarx > 0) {
                    stats.setNetworkReadKBs(deltarx / 1024);
                }
                final double deltatx = tx - oldStats.tx;
                if (deltatx > 0) {
                    stats.setNetworkWriteKBs(deltatx / 1024);
                }
            }

            /* get disk stats */
            final List<LibvirtDiskDef> disks = getDisks(conn, vmName);
            long ioRd = 0;
            long ioWr = 0;
            long bytesRd = 0;
            long bytesWr = 0;
            for (final LibvirtDiskDef disk : disks) {
                if (disk.getDeviceType() == DeviceType.CDROM || disk.getDeviceType() == DeviceType.FLOPPY) {
                    continue;
                }
                final DomainBlockStats blockStats = dm.blockStats(disk.getDiskLabel());
                ioRd += blockStats.rd_req;
                ioWr += blockStats.wr_req;
                bytesRd += blockStats.rd_bytes;
                bytesWr += blockStats.wr_bytes;
            }

            if (oldStats != null) {
                final long deltaiord = ioRd - oldStats.ioRead;
                if (deltaiord > 0) {
                    stats.setDiskReadIOs(deltaiord);
                }
                final long deltaiowr = ioWr - oldStats.ioWrote;
                if (deltaiowr > 0) {
                    stats.setDiskWriteIOs(deltaiowr);
                }
                final double deltabytesrd = bytesRd - oldStats.bytesRead;
                if (deltabytesrd > 0) {
                    stats.setDiskReadKBs(deltabytesrd / 1024);
                }
                final double deltabyteswr = bytesWr - oldStats.bytesWrote;
                if (deltabyteswr > 0) {
                    stats.setDiskWriteKBs(deltabyteswr / 1024);
                }
            }

            /* save to Hashmap */
            final VmStats newStat = new VmStats();
            newStat.usedTime = info.cpuTime;
            newStat.rx = rx;
            newStat.tx = tx;
            newStat.ioRead = ioRd;
            newStat.ioWrote = ioWr;
            newStat.bytesRead = bytesRd;
            newStat.bytesWrote = bytesWr;
            newStat.timestamp = now;
            this.vmStats.put(vmName, newStat);
            return stats;
        } finally {
            if (dm != null) {
                dm.free();
            }
        }
    }

    private String executeBashScript(final String script) {
        final Script command = new Script("/bin/bash", getScriptsTimeout(), logger);
        command.add("-c");
        command.add(script);
        return command.execute();
    }

    public Pair<Double, Double> getNicStats(final String nicName) {
        return new Pair<>(readDouble(nicName, "rx_bytes"), readDouble(nicName, "tx_bytes"));
    }

    static double readDouble(final String nicName, final String fileName) {
        final String path = "/sys/class/net/" + nicName + "/statistics/" + fileName;
        try {
            return Double.parseDouble(FileUtils.readFileToString(new File(path)));
        } catch (final IOException ioe) {
            logger.warn("Failed to read the " + fileName + " for " + nicName + " from " + path, ioe);
            return 0.0;
        }
    }

    @Override
    public void setName(final String name) {
    }

    private HypervisorType getHypervisorType() {
        return this.libvirtComputingResourceProperties.getHypervisorType();
    }

    public List<String> getCpuFeatures() {
        return this.libvirtComputingResourceProperties.getGuestCpuFeatures();
    }

    public String getRouterTermpolicyCrash() {
        return this.libvirtComputingResourceProperties.getRouterTermPolicyCrash();
    }

    public String getRouterTermpolicyPowerOff() {
        return this.libvirtComputingResourceProperties.getRouterTermPolicyPowerOff();
    }

    public String getRouterTermpolicyReboot() {
        return this.libvirtComputingResourceProperties.getRouterTermPolicyReboot();
    }

    public String getVmTermpolicyCrash() {
        return this.libvirtComputingResourceProperties.getVmTermPolicyCrash();
    }

    public String getVmTermpolicyPowerOff() {
        return this.libvirtComputingResourceProperties.getVmTermPolicyPowerOff();
    }

    public String getVmTermpolicyReboot() {
        return this.libvirtComputingResourceProperties.getVmTermPolicyReboot();
    }

    public String[] getIfNamePatterns() {
        return this.ifNamePatterns;
    }

    protected void setBridgeType(final BridgeType bridgeType) {
        this.libvirtComputingResourceProperties.setNetworkBridgeType(bridgeType);
    }

    protected enum BridgeType {
        NATIVE, OPENVSWITCH
    }

    private static final class KeyValueInterpreter extends OutputInterpreter {
        private final Map<String, String> map = new HashMap<>();

        @Override
        public String interpret(final BufferedReader reader) throws IOException {
            String line = null;
            int numLines = 0;
            while ((line = reader.readLine()) != null) {
                final String[] toks = line.trim().split("=");
                if (toks.length < 2) {
                    logger.warn("Failed to parse Script output: " + line);
                } else {
                    this.map.put(toks[0].trim(), toks[1].trim());
                }
                numLines++;
            }
            if (numLines == 0) {
                logger.warn("KeyValueInterpreter: no output lines?");
            }
            return null;
        }

        public Map<String, String> getKeyValues() {
            return this.map;
        }
    }

    private class VmStats {
        long usedTime;
        long tx;
        long rx;
        long ioRead;
        long ioWrote;
        long bytesRead;
        long bytesWrote;
        Calendar timestamp;
    }

    /* online snapshot supported by enhanced qemu-kvm */
    private boolean isSnapshotSupported() {
        final String result = executeBashScript("qemu-img --help|grep convert");
        if (result != null) {
            return false;
        } else {
            return true;
        }
    }

    public long getTotalMemory() {
        return this.totalMemory;
    }
}
