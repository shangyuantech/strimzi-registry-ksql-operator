package io.strimzi.operator.schemaregistry.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;

import static io.strimzi.operator.model.ServiceType.ClusterIP;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaRegistrySpec {

    public SchemaRegistrySpec() {
    }

    private String version = "2.6.0";

    private Integer replicas = 1;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String bootstrapServers;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> config;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> otherProps;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String image;

    public static class External {

        public External() {
        }

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
            return "External {" +
                    "type = '" + type + '\'' +
                    ", nodeport=" + nodeport +
                    ", loadbalanceIp='" + loadbalanceIp + '\'' +
                    ", loadbalancerSourceRanges='" + loadbalancerSourceRanges + '\'' +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private External external;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Template {

        public Template() {
        }

        public static class Pod {

            public Pod() {
            }

            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            private List<String> imagePullSecrets;

            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            private Long terminationGracePeriodSeconds = 120L;

            public List<String> getImagePullSecrets() {
                return imagePullSecrets;
            }

            public void setImagePullSecrets(List<String> imagePullSecrets) {
                this.imagePullSecrets = imagePullSecrets;
            }

            public Long getTerminationGracePeriodSeconds() {
                return terminationGracePeriodSeconds;
            }

            public void setTerminationGracePeriodSeconds(Long terminationGracePeriodSeconds) {
                this.terminationGracePeriodSeconds = terminationGracePeriodSeconds;
            }

            @Override
            public String toString() {
                return "Pod {" +
                        "imagePullSecrets = " + imagePullSecrets +
                        ", terminationGracePeriodSeconds = " + terminationGracePeriodSeconds +
                        '}';
            }
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Pod pod;

        public Pod getPod() {
            return pod;
        }

        public void setPod(Pod pod) {
            this.pod = pod;
        }

        @Override
        public String toString() {
            return "Template {" +
                    "pod = " + pod +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
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
        return external == null ? new External() : external;
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

    public static class Resources {

        public Resources() {
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, String> requests;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, String> limits;

        public Map<String, String> getRequests() {
            return requests;
        }

        public void setRequests(Map<String, String> requests) {
            this.requests = requests;
        }

        public Map<String, String> getLimits() {
            return limits;
        }

        public void setLimits(Map<String, String> limits) {
            this.limits = limits;
        }

        @Override
        public String toString() {
            return "Resources {" +
                    "requests = " + requests +
                    ", limits = " + limits +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Resources resources;

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public static class ReadinessProbe {

        public ReadinessProbe() {
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer initialDelaySeconds;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer timeoutSeconds;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer periodSeconds;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer successThreshold;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer failureThreshold;

        public Integer getInitialDelaySeconds() {
            return initialDelaySeconds;
        }

        public void setInitialDelaySeconds(Integer initialDelaySeconds) {
            this.initialDelaySeconds = initialDelaySeconds;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Integer getPeriodSeconds() {
            return periodSeconds;
        }

        public void setPeriodSeconds(Integer periodSeconds) {
            this.periodSeconds = periodSeconds;
        }

        public Integer getSuccessThreshold() {
            return successThreshold;
        }

        public void setSuccessThreshold(Integer successThreshold) {
            this.successThreshold = successThreshold;
        }

        public Integer getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(Integer failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        @Override
        public String toString() {
            return "ReadinessProbe {" +
                    "initialDelaySeconds = " + initialDelaySeconds +
                    ", timeoutSeconds = " + timeoutSeconds +
                    ", periodSeconds = " + timeoutSeconds +
                    ", successThreshold = " + timeoutSeconds +
                    ", failureThreshold = " + timeoutSeconds +
                    '}';
        }
    }

    public static class LivenessProbe {

        public LivenessProbe() {
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer initialDelaySeconds;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer timeoutSeconds;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer periodSeconds;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer successThreshold;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Integer failureThreshold;

        public Integer getInitialDelaySeconds() {
            return initialDelaySeconds;
        }

        public void setInitialDelaySeconds(Integer initialDelaySeconds) {
            this.initialDelaySeconds = initialDelaySeconds;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Integer getPeriodSeconds() {
            return periodSeconds;
        }

        public void setPeriodSeconds(Integer periodSeconds) {
            this.periodSeconds = periodSeconds;
        }

        public Integer getSuccessThreshold() {
            return successThreshold;
        }

        public void setSuccessThreshold(Integer successThreshold) {
            this.successThreshold = successThreshold;
        }

        public Integer getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(Integer failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        @Override
        public String toString() {
            return "LivenessProbe {" +
                    "initialDelaySeconds = " + initialDelaySeconds +
                    ", timeoutSeconds = " + timeoutSeconds +
                    ", periodSeconds = " + timeoutSeconds +
                    ", successThreshold = " + timeoutSeconds +
                    ", failureThreshold = " + timeoutSeconds +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private ReadinessProbe readinessProbe;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private LivenessProbe livenessProbe;

    public ReadinessProbe getReadinessProbe() {
        return readinessProbe;
    }

    public void setReadinessProbe(ReadinessProbe readinessProbe) {
        this.readinessProbe = readinessProbe;
    }

    public LivenessProbe getLivenessProbe() {
        return livenessProbe;
    }

    public void setLivenessProbe(LivenessProbe livenessProbe) {
        this.livenessProbe = livenessProbe;
    }

    @Override
    public String toString() {
        return "SchemaRegistrySpec {" + "\n" +
                "   version = '" + version + '\'' +
                ", replicas = " + replicas +
                ", bootstrapServers = '" + bootstrapServers + '\'' +
                ", config = " + config +
                ", otherProps = " + otherProps +
                ", image = '" + image + '\'' +
                ", external = " + external +
                ", template = " + template +
                ", resources = " + resources +
                ", readinessProbe = " + readinessProbe +
                ", livenessProbe = " + livenessProbe +
                '}';
    }
}
