package io.strimzi.operator.schemaregistry.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.List;
import java.util.Map;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class SchemaRegistrySpec implements KubernetesResource {

    private String version;

    private int replicas;

    private String bootstrapServers;

    private Map<String, String> config;

    private Map<String, String> otherProps;

    private String image;

    public class External {

        private String type;

        private Integer nodeport;

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

        @Override
        public String toString() {
            return "External{" +
                    "type='" + type + '\'' +
                    ", nodeport=" + nodeport +
                    '}';
        }
    }

    private External external;

    public class Template {

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

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
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
        return "SchemaRegistrySpec{" +
                "version='" + version + '\'' +
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
