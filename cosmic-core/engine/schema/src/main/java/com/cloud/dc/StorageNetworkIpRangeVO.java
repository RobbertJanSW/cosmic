package com.cloud.dc;

import com.cloud.legacymodel.dc.StorageNetworkIpRange;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "dc_storage_network_ip_range")
@SecondaryTables({@SecondaryTable(name = "networks", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "network_id", referencedColumnName = "id")}),
        @SecondaryTable(name = "host_pod_ref", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "pod_id", referencedColumnName = "id")}),
        @SecondaryTable(name = "data_center", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "data_center_id", referencedColumnName = "id")})})
public class StorageNetworkIpRangeVO implements StorageNetworkIpRange {
    @Column(name = "uuid")
    String uuid;
    @Column(name = "uuid", table = "networks", insertable = false, updatable = false)
    String networkUuid;
    @Column(name = "uuid", table = "host_pod_ref", insertable = false, updatable = false)
    String podUuid;
    @Column(name = "uuid", table = "data_center", insertable = false, updatable = false)
    String zoneUuid;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Column(name = "vlan")
    private Integer vlan;
    @Column(name = "data_center_id")
    private long dataCenterId;
    @Column(name = "pod_id")
    private long podId;
    @Column(name = "start_ip")
    private String startIp;
    @Column(name = "end_ip")
    private String endIp;
    @Column(name = "gateway")
    private String gateway;
    @Column(name = "network_id")
    private long networkId;
    @Column(name = "netmask")
    private String netmask;

    public StorageNetworkIpRangeVO(final long dcId, final long podId, final long networkId, final String startIp, final String endIp, final Integer vlan, final String netmask,
                                   final String gateway) {
        this();
        this.dataCenterId = dcId;
        this.podId = podId;
        this.networkId = networkId;
        this.startIp = startIp;
        this.endIp = endIp;
        this.vlan = vlan;
        this.netmask = netmask;
        this.gateway = gateway;
    }

    protected StorageNetworkIpRangeVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(final long dcId) {
        this.dataCenterId = dcId;
    }

    public long getPodId() {
        return podId;
    }

    public void setPodId(final long podId) {
        this.podId = podId;
    }

    public long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(final long nwId) {
        this.networkId = nwId;
    }

    @Override
    public Integer getVlan() {
        return vlan;
    }

    public void setVlan(final int vlan) {
        this.vlan = vlan;
    }

    @Override
    public String getPodUuid() {
        return podUuid;
    }

    @Override
    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(final String start) {
        this.startIp = start;
    }

    @Override
    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(final String end) {
        this.endIp = end;
    }

    @Override
    public String getNetworkUuid() {
        return networkUuid;
    }

    @Override
    public String getZoneUuid() {
        return zoneUuid;
    }

    @Override
    public String getNetmask() {
        return netmask;
    }

    @Override
    public String getGateway() {
        return this.gateway;
    }

    public void setGateway(final String gateway) {
        this.gateway = gateway;
    }

    public void setNetmask(final String netmask) {
        this.netmask = netmask;
    }

    @Override
    public String getUuid() {
        return uuid;
    }
}
