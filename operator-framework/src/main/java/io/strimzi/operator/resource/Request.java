package io.strimzi.operator.resource;

import io.fabric8.kubernetes.client.CustomResource;

public class Request extends CustomResource {

    private String name;
    private String namespace;

    public Request(String name) {
        this(null, name);
    }

    public Request(String namespace, String name) {
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
