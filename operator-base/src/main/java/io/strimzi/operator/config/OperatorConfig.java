package io.strimzi.operator.config;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.strimzi.operator.exception.InvalidConfigurationException;
import io.strimzi.operator.model.ImagePullPolicy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class OperatorConfig {

    public static final Logger logger = LoggerFactory.getLogger(OperatorConfig.class);

    public static final String STRIMZI_CLUSTER_OPERATOR_NAME = "strimzi-registry-ksql-operator";

    public static final String STRIMZI_NAMESPACE = "STRIMZI_NAMESPACE";
    public static final String STRIMZI_DEFAULT_NAMESPACE = "*";

    public static final String STRIMZI_IMAGE_PULL_POLICY = "STRIMZI_IMAGE_PULL_POLICY";
    public static final String STRIMZI_IMAGE_PULL_SECRETS = "STRIMZI_IMAGE_PULL_SECRETS";

    public static final String STRIMZI_SCHEMA_REGISTRY_IMAGE = "STRIMZI_SCHEMA_REGISTRY_IMAGE";
    public static final String STRIMZI_DEFAULT_SCHEMA_REGISTRY_IMAGE = "2.5.0=confluentinc/cp-schema-registry:5.5.0\n" +
            "2.6.0=confluentinc/cp-schema-registry:6.0.0";

    public static final String SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS = "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS";


    private final List<String> namespaces;
    private final ImagePullPolicy imagePullPolicy;
    private final List<LocalObjectReference> imagePullSecrets;
    private final HashMap<String, String> schemaRegistryImage;

    public static OperatorConfig fromMap(Map<String, String> map) {
        List<String> namespaces = Arrays.asList(map.getOrDefault(STRIMZI_NAMESPACE, STRIMZI_DEFAULT_NAMESPACE).split(","));
        ImagePullPolicy imagePullPolicy = parseImagePullPolicy(map.get(STRIMZI_IMAGE_PULL_POLICY));
        List<LocalObjectReference> imagePullSecrets = parseImagePullSecrets(map.get(STRIMZI_IMAGE_PULL_SECRETS));
        HashMap<String, String> schemaRegistryImage = parseSchemaRegistryImage(map.get(STRIMZI_SCHEMA_REGISTRY_IMAGE));

        return new OperatorConfig(namespaces, imagePullPolicy, imagePullSecrets, schemaRegistryImage);
    }

    public OperatorConfig(List<String> namespaces, ImagePullPolicy imagePullPolicy,
                          List<LocalObjectReference> imagePullSecrets,
                          HashMap<String, String> schemaRegistryImage) {
        this.namespaces = namespaces;
        this.imagePullPolicy = imagePullPolicy;
        this.imagePullSecrets = imagePullSecrets;
        this.schemaRegistryImage = schemaRegistryImage;
    }

    private static ImagePullPolicy parseImagePullPolicy(String imagePullPolicyEnvVar) {
        ImagePullPolicy imagePullPolicy = null;

        if (StringUtils.isNotEmpty(imagePullPolicyEnvVar)) {
            switch (imagePullPolicyEnvVar.trim().toLowerCase(Locale.ENGLISH)) {
                case "always":
                    imagePullPolicy = ImagePullPolicy.ALWAYS;
                    break;
                case "ifnotpresent":
                    imagePullPolicy = ImagePullPolicy.IFNOTPRESENT;
                    break;
                case "never":
                    imagePullPolicy = ImagePullPolicy.NEVER;
                    break;
                default:
                    throw new InvalidConfigurationException(imagePullPolicyEnvVar
                            + " is not a valid " + STRIMZI_IMAGE_PULL_POLICY + " value. " +
                            STRIMZI_IMAGE_PULL_POLICY + " can have one of the following values: Always, IfNotPresent, Never.");
            }
        } else {
            imagePullPolicy = ImagePullPolicy.IFNOTPRESENT;
        }

        return imagePullPolicy;
    }

    private static List<LocalObjectReference> parseImagePullSecrets(String imagePullSecretList) {
        List<LocalObjectReference> imagePullSecrets = null;

        if (StringUtils.isNoneEmpty(imagePullSecretList)) {
            if (imagePullSecretList.matches("(\\s*[a-z0-9.-]+\\s*,)*\\s*[a-z0-9.-]+\\s*")) {
                imagePullSecrets = Arrays.stream(imagePullSecretList.trim().split("\\s*,+\\s*"))
                        .map(secret -> new LocalObjectReferenceBuilder().withName(secret).build())
                        .collect(Collectors.toList());
            } else {
                throw new InvalidConfigurationException(STRIMZI_IMAGE_PULL_SECRETS
                        + " is not a valid list of secret names");
            }
        } else {
            imagePullSecrets = new ArrayList<>();
        }

        return imagePullSecrets;
    }

    private static HashMap<String, String> parseSchemaRegistryImage(String schemaRegistryImage) {
        if (StringUtils.isEmpty(schemaRegistryImage)) {
            schemaRegistryImage = STRIMZI_DEFAULT_SCHEMA_REGISTRY_IMAGE;
        }
        String[] schemaRegistryImages = schemaRegistryImage.split("\n");

        HashMap<String, String> schemaRegistryImageMap = new HashMap<>();
        for (String sri : schemaRegistryImages) {
            String[] vi = sri.split("=");
            if (vi.length == 2) {
                schemaRegistryImageMap.put(vi[0], vi[1]);
            } else {
                logger.warn("{} can not parse to schema registry images map! need to be like {version}={image}", sri);
            }
        }

        return schemaRegistryImageMap;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public ImagePullPolicy getImagePullPolicy() {
        return imagePullPolicy;
    }

    public List<LocalObjectReference> getImagePullSecrets() {
        return imagePullSecrets;
    }

    public HashMap<String, String> getSchemaRegistryImages() {
        return schemaRegistryImage;
    }

    public String getSchemaRegistryImage(String version) {
        return schemaRegistryImage.get(version);
    }

    @Override
    public String toString() {
        return "OperatorConfig {" + "\n" +
                "   namespaces=" + namespaces + "\n" +
                "   imagePullPolicy=" + imagePullPolicy + "\n" +
                "   imagePullSecrets=" + imagePullSecrets + "\n" +
                "   schemaRegistryImage=" + schemaRegistryImage + "\n" +
                '}';
    }
}
