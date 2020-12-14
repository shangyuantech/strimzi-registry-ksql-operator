package io.strimzi.operator.resource;

public class CRDDef {

    private String version = "v1beta1";

    private String scope = "Namespaced";

    private String kind;

    private String group = "kafka.strimzi.io";

    private String plural;

    public CRDDef(String kind, String plural) {
        this.kind = kind;
        this.plural = plural;
    }

    public CRDDef(String version, String scope, String group, String kind, String plural) {
        this.version = version;
        this.scope = scope;
        this.group = group;
        this.kind = kind;
        this.plural = plural;
    }

    public String getPlural() {
        return plural;
    }

    public void setPlural(String plural) {
        this.plural = plural;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        return "CRDDef{" +
                "version='" + version + '\'' +
                ", scope='" + scope + '\'' +
                ", group='" + group + '\'' +
                ", kind='" + kind + '\'' +
                ", plural='" + plural + '\'' +
                '}';
    }
}
