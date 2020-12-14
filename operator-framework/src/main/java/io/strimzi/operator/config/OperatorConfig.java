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

    public static final String STRIMZI_NAMESPACE = "STRIMZI_NAMESPACE";
    public static final String STRIMZI_FULL_RECONCILIATION_INTERVAL_MS = "STRIMZI_FULL_RECONCILIATION_INTERVAL_MS";
    public static final String STRIMZI_OPERATION_TIMEOUT_MS = "STRIMZI_OPERATION_TIMEOUT_MS";
    public static final String STRIMZI_IMAGE_PULL_POLICY = "STRIMZI_IMAGE_PULL_POLICY";
    public static final String STRIMZI_IMAGE_PULL_SECRETS = "STRIMZI_IMAGE_PULL_SECRETS";

    public static final String STRIMZI_SCHEMA_REGISTRY_IMAGE = "STRIMZI_SCHEMA_REGISTRY_IMAGE";
    public static final String STRIMZI_DEFAULT_SCHEMA_REGISTRY_IMAGE = "2.5.0=confluentinc/cp-schema-registry:5.5.0\n" +
            "2.6.0=confluentinc/cp-schema-registry:5.6.0";

    public static final long DEFAULT_FULL_RECONCILIATION_INTERVAL_MS = 120_000;
    public static final long DEFAULT_OPERATION_TIMEOUT_MS = 300_000;

    private final Optional<String> namespaces;
    private final long reconciliationInterval;
    private final long operationTimeout;
    private final Optional<ImagePullPolicy> imagePullPolicy;
    private final Optional<List<LocalObjectReference>> imagePullSecrets;
    private final HashMap<String, String> schemaRegistryImage;

    public static OperatorConfig fromMap(Map<String, String> map) {
        Optional<String> namespaces = Optional.ofNullable(map.get(STRIMZI_NAMESPACE));
        long reconciliationInterval = parseReconciliationInerval(map.get(STRIMZI_FULL_RECONCILIATION_INTERVAL_MS));
        long operationTimeout = parseOperationTimeout(map.get(STRIMZI_OPERATION_TIMEOUT_MS));
        Optional<ImagePullPolicy> imagePullPolicy = parseImagePullPolicy(map.get(STRIMZI_IMAGE_PULL_POLICY));
        Optional<List<LocalObjectReference>> imagePullSecrets = parseImagePullSecrets(map.get(STRIMZI_IMAGE_PULL_SECRETS));
        HashMap<String, String> schemaRegistryImage = parseSchemaRegistryImage(map.get(STRIMZI_SCHEMA_REGISTRY_IMAGE));

        return new OperatorConfig(namespaces, reconciliationInterval, operationTimeout,
                imagePullPolicy, imagePullSecrets, schemaRegistryImage);
    }

    public OperatorConfig(Optional<String> namespaces, long reconciliationInterval, long operationTimeout,
                          Optional<ImagePullPolicy> imagePullPolicy, Optional<List<LocalObjectReference>> imagePullSecrets,
                          HashMap<String, String> schemaRegistryImage) {
        this.namespaces = namespaces;
        this.reconciliationInterval = reconciliationInterval;
        this.operationTimeout = operationTimeout;
        this.imagePullPolicy = imagePullPolicy;
        this.imagePullSecrets = imagePullSecrets;
        this.schemaRegistryImage = schemaRegistryImage;
    }

    private static Optional<ImagePullPolicy> parseImagePullPolicy(String imagePullPolicyEnvVar) {
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
        }

        return Optional.ofNullable(imagePullPolicy);
    }

    private static Optional<List<LocalObjectReference>> parseImagePullSecrets(String imagePullSecretList) {
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
        }

        return Optional.ofNullable(imagePullSecrets);
    }

    private static long parseReconciliationInerval(String reconciliationIntervalEnvVar) {
        long reconciliationInterval = DEFAULT_FULL_RECONCILIATION_INTERVAL_MS;

        if (StringUtils.isNoneEmpty(reconciliationIntervalEnvVar)) {
            reconciliationInterval = Long.parseLong(reconciliationIntervalEnvVar);
        }

        return reconciliationInterval;
    }

    private static long parseOperationTimeout(String operationTimeoutEnvVar) {
        long operationTimeout = DEFAULT_OPERATION_TIMEOUT_MS;

        if (StringUtils.isNoneEmpty(operationTimeoutEnvVar)) {
            operationTimeout = Long.parseLong(operationTimeoutEnvVar);
        }

        return operationTimeout;
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
}
