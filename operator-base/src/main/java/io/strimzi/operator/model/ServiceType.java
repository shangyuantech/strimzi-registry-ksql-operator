package io.strimzi.operator.model;

public enum ServiceType {

    ClusterIP("clusterip"),
    NodePort("nodeport"),
    LoadBalancer("loadbalancer");

    private final String name;

    ServiceType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ServiceType getTypeByName(String name) {
        for (ServiceType type : ServiceType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return ClusterIP;
    }
}
