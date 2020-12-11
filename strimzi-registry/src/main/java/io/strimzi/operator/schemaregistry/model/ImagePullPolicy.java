package io.strimzi.operator.schemaregistry.model;

/**
 * Enum for ImagePullPolicy types. Supports the 3 types supported in Kubernetes / OpenShift:
 * - Always
 * - Never
 * - IfNotPresent
 */
public enum ImagePullPolicy {
    ALWAYS("Always"),
    IFNOTPRESENT("IfNotPresent"),
    NEVER("Never");

    private final String imagePullPolicy;

    ImagePullPolicy(String imagePullPolicy) {
        this.imagePullPolicy = imagePullPolicy;
    }

    public String toString()    {
        return imagePullPolicy;
    }
}