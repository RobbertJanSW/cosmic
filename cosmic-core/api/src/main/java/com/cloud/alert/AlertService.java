package com.cloud.alert;

import com.cloud.capacity.Capacity;
import com.cloud.legacymodel.exceptions.InvalidParameterValueException;

import java.util.HashSet;
import java.util.Set;

public interface AlertService {
    boolean generateAlert(AlertType alertType, long dataCenterId, Long podId, String msg);

    class AlertType {
        private static final Set<AlertType> defaultAlertTypes = new HashSet<>();

        public static final AlertType ALERT_TYPE_MEMORY = new AlertType(Capacity.CAPACITY_TYPE_MEMORY, "ALERT.MEMORY", true);
        public static final AlertType ALERT_TYPE_CPU = new AlertType(Capacity.CAPACITY_TYPE_CPU, "ALERT.CPU", true);
        public static final AlertType ALERT_TYPE_STORAGE = new AlertType(Capacity.CAPACITY_TYPE_STORAGE, "ALERT.STORAGE", true);
        public static final AlertType ALERT_TYPE_STORAGE_ALLOCATED = new AlertType(Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, "ALERT.STORAGE.ALLOCATED", true);
        public static final AlertType ALERT_TYPE_VIRTUAL_NETWORK_PUBLIC_IP = new AlertType(Capacity.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP, "ALERT.NETWORK.PUBLICIP",
                true);
        public static final AlertType ALERT_TYPE_PRIVATE_IP = new AlertType(Capacity.CAPACITY_TYPE_PRIVATE_IP, "ALERT.NETWORK.PRIVATEIP", true);
        public static final AlertType ALERT_TYPE_SECONDARY_STORAGE = new AlertType(Capacity.CAPACITY_TYPE_SECONDARY_STORAGE, "ALERT.STORAGE.SECONDARY", true);
        public static final AlertType ALERT_TYPE_HOST = new AlertType((short) 7, "ALERT.COMPUTE.HOST", true);
        public static final AlertType ALERT_TYPE_USERVM = new AlertType((short) 8, "ALERT.USERVM", true);
        public static final AlertType ALERT_TYPE_DOMAIN_ROUTER = new AlertType((short) 9, "ALERT.SERVICE.DOMAINROUTER", true);
        public static final AlertType ALERT_TYPE_CONSOLE_PROXY = new AlertType((short) 10, "ALERT.SERVICE.CONSOLEPROXY", true);
        public static final AlertType ALERT_TYPE_ROUTING = new AlertType((short) 11, "ALERT.NETWORK.ROUTING", true);
        public static final AlertType ALERT_TYPE_STORAGE_MISC = new AlertType((short) 12, "ALERT.STORAGE.MISC", true);
        public static final AlertType ALERT_TYPE_USAGE_SERVER = new AlertType((short) 13, "ALERT.USAGE", true);
        public static final AlertType ALERT_TYPE_MANAGMENT_NODE = new AlertType((short) 14, "ALERT.MANAGEMENT", true);
        public static final AlertType ALERT_TYPE_DOMAIN_ROUTER_MIGRATE = new AlertType((short) 15, "ALERT.NETWORK.DOMAINROUTERMIGRATE", true);
        public static final AlertType ALERT_TYPE_CONSOLE_PROXY_MIGRATE = new AlertType((short) 16, "ALERT.SERVICE.CONSOLEPROXYMIGRATE", true);
        public static final AlertType ALERT_TYPE_USERVM_MIGRATE = new AlertType((short) 17, "ALERT.USERVM.MIGRATE", true);
        public static final AlertType ALERT_TYPE_VLAN = new AlertType((short) 18, "ALERT.NETWORK.VLAN", true);
        public static final AlertType ALERT_TYPE_SSVM = new AlertType((short) 19, "ALERT.SERVICE.SSVM", true);
        public static final AlertType ALERT_TYPE_USAGE_SERVER_RESULT = new AlertType((short) 20, "ALERT.USAGE.RESULT", true);
        public static final AlertType ALERT_TYPE_STORAGE_DELETE = new AlertType((short) 21, "ALERT.STORAGE.DELETE", true);
        public static final AlertType ALERT_TYPE_UPDATE_RESOURCE_COUNT = new AlertType((short) 22, "ALERT.RESOURCE.COUNT", true);
        public static final AlertType ALERT_TYPE_USAGE_SANITY_RESULT = new AlertType((short) 23, "ALERT.USAGE.SANITY", true);
        public static final AlertType ALERT_TYPE_DIRECT_ATTACHED_PUBLIC_IP = new AlertType((short) 24, "ALERT.NETWORK.DIRECTPUBLICIP", true);
        public static final AlertType ALERT_TYPE_LOCAL_STORAGE = new AlertType((short) 25, "ALERT.STORAGE.LOCAL", true);
        public static final AlertType ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED = new AlertType((short) 26, "ALERT.RESOURCE.EXCEED", true);
        public static final AlertType ALERT_TYPE_SYNC = new AlertType((short) 27, "ALERT.TYPE.SYNC", true);
        public static final AlertType ALERT_TYPE_UPLOAD_FAILED = new AlertType((short) 28, "ALERT.UPLOAD.FAILED", true);
        private final String name;
        private final short type;

        private AlertType(final short type, final String name, final boolean isDefault) {
            this.name = name;
            this.type = type;
            if (isDefault) {
                defaultAlertTypes.add(this);
            }
        }

        public static AlertType generateAlert(final short type, final String name) {
            final AlertType defaultAlert = getAlertType(type);
            if (defaultAlert != null && !defaultAlert.getName().equalsIgnoreCase(name)) {
                throw new InvalidParameterValueException("There is a default alert having type " + type + " and name " + defaultAlert.getName());
            } else {
                return new AlertType(type, name, false);
            }
        }

        private static AlertType getAlertType(final short type) {
            for (final AlertType alertType : defaultAlertTypes) {
                if (alertType.getType() == type) {
                    return alertType;
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public short getType() {
            return type;
        }

        @Override
        public String toString() {
            return String.valueOf(this.getType());
        }
    }
}
