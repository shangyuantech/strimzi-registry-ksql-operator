package io.strimzi.operator.schemaregistry.crd;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

import static io.strimzi.operator.model.ServiceType.ClusterIP;

public class SchemaRegistrySpec {

    private String version = "2.6.0";

    private Integer replicas = 1;

    private String bootstrapServers;

    private Map<String, String> config;

    private Map<String, String> otherProps;

    private String image;

    public static class External {

        private String type = ClusterIP.getName();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer nodeport;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String loadbalanceIp;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> loadbalancerSourceRanges;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getNodeport() {
            return nodeport;
        }

        public void setNodeport(Integer nodeport) {
            this.nodeport = nodeport;
        }

        public String getLoadbalanceIp() {
            return loadbalanceIp;
        }

        public void setLoadbalanceIp(String loadbalanceIp) {
            this.loadbalanceIp = loadbalanceIp;
        }

        public List<String> getLoadbalancerSourceRanges() {
            return loadbalancerSourceRanges;
        }

        public void setLoadbalancerSourceRanges(List<String> loadbalancerSourceRanges) {
            this.loadbalancerSourceRanges = loadbalancerSourceRanges;
        }

        @Override
        public String toString() {
            return "External{" +
                    "type='" + type + '\'' +
                    ", nodeport=" + nodeport +
                    ", loadbalanceIp='" + loadbalanceIp + '\'' +
                    ", loadbalancerSourceRanges='" + loadbalancerSourceRanges + '\'' +
                    '}';
        }
    }

    private External external;

    public static class Template {

        public class Pod {

            private List<String> imagePullSecrets;

            public List<String> getImagePullSecrets() {
                return imagePullSecrets;
            }

            public void setImagePullSecrets(List<String> imagePullSecrets) {
                this.imagePullSecrets = imagePullSecrets;
            }

            @Override
            public String toString() {
                return "Pod{" +
                        "imagePullSecrets=" + imagePullSecrets +
                        '}';
            }
        }

        private Pod pod;

        public Pod getPod() {
            return pod;
        }

        public void setPod(Pod pod) {
            this.pod = pod;
        }

        @Override
        public String toString() {
            return "Template{" +
                    "pod=" + pod +
                    '}';
        }
    }

    private Template template;

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public Map<String, String> getOtherProps() {
        return otherProps;
    }

    public void setOtherProps(Map<String, String> otherProps) {
        this.otherProps = otherProps;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public External getExternal() {
        return external;
    }

    public void setExternal(External external) {
        this.external = external;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    @Override
    public String toString() {
        return "SchemaRegistrySpec{" + "\n" +
                "   version='" + version + '\'' +
                ", replicas=" + replicas +
                ", bootstrapServers='" + bootstrapServers + '\'' +
                ", config=" + config +
                ", otherProps=" + otherProps +
                ", image='" + image + '\'' +
                ", external=" + external +
                ", template=" + template +
                '}';
    }
}
