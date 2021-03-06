package com.cloud.network.nicira;

public class NiciraConstants {

    public static final String SEC_PROFILE_URI_PREFIX = "/ws.v1/security-profile";
    public static final String ACL_URI_PREFIX = "/ws.v1/acl";
    public static final String SWITCH_URI_PREFIX = "/ws.v1/lswitch";
    public static final String ROUTER_URI_PREFIX = "/ws.v1/lrouter";
    public static final String LOGIN_URL = "/ws.v1/login";
    public static final String CONTROL_CLUSTER_STATUS_URL = "/ws.v1/control-cluster/status";

    public static final String ATTACHMENT_PATH_SEGMENT = "/attachment";
    public static final String NAT_PATH_SEGMENT = "/nat";
    public static final String LPORT_PATH_SEGMENT = "/lport";

    public static final String ATTACHMENT_VIF_UUID_QUERY_PARAMETER_NAME = "attachment_vif_uuid";
    public static final String ATTACHMENT_VLAN_PARAMETER = "attachment_vlan";
    public static final String ATTACHMENT_GWSVC_UUID_QUERY_PARAMETER = "attachment_gwsvc_uuid";
    public static final String WILDCARD_QUERY_PARAMETER = "*";
    public static final String UUID_QUERY_PARAMETER = "uuid";
    public static final String FIELDS_QUERY_PARAMETER = "fields";
}
