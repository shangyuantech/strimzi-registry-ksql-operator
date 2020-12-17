package io.strimzi.operator.schemaregistry.controller;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.*;
import io.javaoperatorsdk.operator.api.Context;
import io.strimzi.operator.config.OperatorConfig;
import io.strimzi.operator.model.Labels;
import io.strimzi.operator.model.ServiceType;
import io.strimzi.operator.schemaregistry.crd.*;
import io.strimzi.operator.util.EnvUtils;
import io.strimzi.operator.util.KubeUtils;
import io.strimzi.operator.util.ValidationUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller(crdName = "schemaregistries.kafka.strimzi.io")
public class SchemaRegistryController implements ResourceController<SchemaRegistry> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final KubernetesClient kubernetesClient;

    private final OperatorConfig operatorConfig;

    private final static String COMPONENT = "schema-registry";

    private final static Integer DEFAULT_INITIAL_DELAY_SECONDS = 30;
    private final static Integer DEFAULT_TIMEOUT_SECONDS = 5;
    private final static Integer DEFAULT_PERIOD_SECONDS = 10;
    private final static Integer DEFAULT_SUCCESS_THRESHOLD = 1;
    private final static Integer DEFAULT_FAILURE_THRESHOLD  = 3;

    public SchemaRegistryController(KubernetesClient kubernetesClient, OperatorConfig operatorConfig) {
        this.kubernetesClient = kubernetesClient;
        this.operatorConfig = operatorConfig;
    }

    @Override
    public DeleteControl deleteResource(SchemaRegistry schemaRegistry, Context<SchemaRegistry> context) {

        log.info("Deleting Deployment {}", deploymentName(schemaRegistry));
        RollableScalableResource<Deployment, DoneableDeployment> deployment =
                kubernetesClient
                        .apps()
                        .deployments()
                        .inNamespace(schemaRegistry.getMetadata().getNamespace())
                        .withName(deploymentName(schemaRegistry));
        if (deployment.get() != null) {
            deployment.cascading(true).delete();
        }

        log.info("Deleting Service {}", serviceName(schemaRegistry));
        ServiceResource<Service, DoneableService> service =
                kubernetesClient
                        .services()
                        .inNamespace(schemaRegistry.getMetadata().getNamespace())
                        .withName(serviceName(schemaRegistry));
        if (service.get() != null) {
            service.delete();
        }

        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<SchemaRegistry> createOrUpdateResource(SchemaRegistry schemaRegistry, Context<SchemaRegistry> context) {
        if (operatorConfig.getNamespaces().stream()
                .noneMatch(n -> n.equals(OperatorConfig.STRIMZI_DEFAULT_NAMESPACE)
                        || n.equals(schemaRegistry.getMetadata().getNamespace()))) {
            log.warn("Schema Registry {} is not in operator namespace list {}",
                    schemaRegistry.getMetadata().getName(), operatorConfig.getNamespaces());
            return UpdateControl.noUpdate();
        }

        log.debug("update {}", schemaRegistry);
        String namespace = schemaRegistry.getMetadata().getNamespace();

        try {
            // deployment
            Deployment deployment = createDeployment(schemaRegistry, namespace);
            // service
            Service service = createService(schemaRegistry, deployment, namespace);

            // merge deployment
            KubeUtils.mergeDeploymentResource(kubernetesClient, deployment);
            // merge service
            KubeUtils.mergeServiceResource(kubernetesClient, service);

            // build status
            buildStatus(schemaRegistry);

        } catch (Exception e) {
            e.printStackTrace();

            SchemaRegistryStatus fail = new SchemaRegistryStatus();
            fail.setErrorMsg(ExceptionUtils.getMessage(e));
            fail.setStackTrace(ExceptionUtils.getStackFrames(e));
            schemaRegistry.setStatus(fail);
        }

        return UpdateControl.updateStatusSubResource(schemaRegistry);
    }

    /**
     * create a schema registry status
     */
    private void buildStatus(SchemaRegistry schemaRegistry) {
        SchemaRegistryStatus success = new SchemaRegistryStatus();
        // availableReplicas
        success.setAvailableReplicas(schemaRegistry.getSpec().getReplicas());
        // schemaRegistryService
        success.setSchemaRegistryService(String.format("http://%s.%s:8081", serviceName(schemaRegistry),
                schemaRegistry.getMetadata().getNamespace()));

        schemaRegistry.setStatus(success);
    }

    /**
     * create deployment
     */
    private Deployment createDeployment(SchemaRegistry schemaRegistry, String namespace) {
        String deploymentName = deploymentName(schemaRegistry);
        Labels labels = Labels.generateDefaultLabels(schemaRegistry.getMetadata().getName(), schemaRegistry.getKind(),
                COMPONENT, deploymentName, OperatorConfig.STRIMZI_CLUSTER_OPERATOR_NAME);

        // deployment
        Deployment deployment = loadYaml(Deployment.class, "deployment.yaml");
        deployment.getMetadata().setName(deploymentName);
        deployment.getMetadata().setNamespace(namespace);
        deployment.getMetadata().setLabels(labels.toMap());

        deployment
                .getSpec()
                .getSelector()
                .getMatchLabels()
                .putAll(labels.strimziSelectorLabels().toMap());
        deployment
                .getSpec()
                .getTemplate()
                .getMetadata()
                .getLabels()
                .putAll(labels.toMap());

        // image
        String image = schemaRegistry.getSpec().getImage();
        String version = schemaRegistry.getSpec().getVersion();
        if (StringUtils.isEmpty(image)) {
            image = operatorConfig.getSchemaRegistryImage(version);
        }
        ValidationUtils.checkStringEmpty(image,
                "can not find image, please check your version or image properties !\n"
                        + "support versions: " + operatorConfig.getSchemaRegistryImages().keySet());

        log.debug("create deployment with using kafka schema registry version {} and image {} ", version, image);
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(image);

        // pull policy
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0)
                .setImagePullPolicy(operatorConfig.getImagePullPolicy().getImagePullPolicy());

        // template
        if (schemaRegistry.getSpec().getTemplate() != null) {
            SchemaRegistrySpec.Template template = schemaRegistry.getSpec().getTemplate();

            // pod
            if (template.getPod() != null) {
                // pull secret
                List<String> secrets = template.getPod().getImagePullSecrets();
                if (CollectionUtils.isNotEmpty(secrets)) {
                    List<LocalObjectReference> pullSecrets = operatorConfig.getImagePullSecrets();
                    secrets.forEach(secret -> pullSecrets.add(
                            new LocalObjectReferenceBuilder().withName(secret).build())
                    );
                    deployment.getSpec().getTemplate().getSpec().setImagePullSecrets(pullSecrets);
                }

                // terminationGracePeriodSeconds
                if (schemaRegistry.getSpec().getTemplate().getPod().getTerminationGracePeriodSeconds() != null) {
                    deployment.getSpec().getTemplate().getSpec().setTerminationGracePeriodSeconds(
                            schemaRegistry.getSpec().getTemplate().getPod().getTerminationGracePeriodSeconds());
                }
            }
        }

        // resources
        if (schemaRegistry.getSpec().getResources() != null) {
            SchemaRegistrySpec.Resources resources = schemaRegistry.getSpec().getResources();
            ResourceRequirements requirements = new ResourceRequirements();
            // requests
            if (MapUtils.isNotEmpty(resources.getRequests())) {
                Map<String, Quantity> requestsMap = new HashMap<>();
                resources.getRequests().forEach((k, v) -> requestsMap.put(k, Quantity.parse(v)));
                requirements.setRequests(requestsMap);
            }
            // limits
            if (MapUtils.isNotEmpty(resources.getLimits())) {
                Map<String, Quantity> limitsMap = new HashMap<>();
                resources.getLimits().forEach((k, v) -> limitsMap.put(k, Quantity.parse(v)));
                requirements.setLimits(limitsMap);
            }
            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setResources(requirements);
        }

        List<EnvVar> envVars = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
        // config
        Map<String, String> config = schemaRegistry.getSpec().getConfig();
        if (MapUtils.isNotEmpty(config)) {
            config.forEach((key, value) ->
                    envVars.add(new EnvVarBuilder().withName(EnvUtils.transformEnv(key)).withValue(value).build()));
        }
        // other properties
        Map<String, String> otherProps = schemaRegistry.getSpec().getOtherProps();
        if (MapUtils.isNotEmpty(otherProps)) {
            otherProps.forEach((key, value) ->
                    envVars.add(new EnvVarBuilder().withName(key).withValue(value).build()));
        }

        // bootstrap servers
        String bootstrapServers = schemaRegistry.getSpec().getBootstrapServers();
        ValidationUtils.checkStringEmpty(bootstrapServers, "bootstrap servers is needed !");
        int findIndex = -1;
        for (int i = 0 , row = envVars.size() ; i < row ; i ++) {
            if (envVars.get(i).getName().equals(OperatorConfig.SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS)) {
                findIndex = i;
                break;
            }
        }
        EnvVar bssEnv = new EnvVarBuilder().withName(OperatorConfig.SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS)
                .withValue(bootstrapServers).build();
        if (findIndex >= 0) {
            envVars.set(findIndex, bssEnv);
        } else {
            envVars.add(bssEnv);
        }
        log.debug("create deployment with using envs : \n{}", EnvUtils.toPrintString(envVars));

        // readinessProbe
        if (schemaRegistry.getSpec().getReadinessProbe() != null) {
            SchemaRegistrySpec.ReadinessProbe readinessProbe = schemaRegistry.getSpec().getReadinessProbe();
            Probe probe = new Probe();
            probe.setHttpGet(new HTTPGetAction(null, null, "/", new IntOrString(8081), "HTTP"));
            probe.setInitialDelaySeconds(readinessProbe.getInitialDelaySeconds() == null ? DEFAULT_INITIAL_DELAY_SECONDS : readinessProbe.getInitialDelaySeconds());
            probe.setTimeoutSeconds(readinessProbe.getTimeoutSeconds() == null ? DEFAULT_TIMEOUT_SECONDS : readinessProbe.getTimeoutSeconds());
            probe.setPeriodSeconds(readinessProbe.getPeriodSeconds() == null ? DEFAULT_PERIOD_SECONDS : readinessProbe.getPeriodSeconds());
            probe.setSuccessThreshold(readinessProbe.getSuccessThreshold() == null ? DEFAULT_SUCCESS_THRESHOLD : readinessProbe.getSuccessThreshold());
            probe.setFailureThreshold(readinessProbe.getFailureThreshold() == null ? DEFAULT_FAILURE_THRESHOLD : readinessProbe.getFailureThreshold());

            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setReadinessProbe(probe);
        }

        // livenessProbe
        if (schemaRegistry.getSpec().getLivenessProbe() != null) {
            SchemaRegistrySpec.LivenessProbe livenessProbe = schemaRegistry.getSpec().getLivenessProbe();
            Probe probe = new Probe();
            probe.setHttpGet(new HTTPGetAction(null, null, "/", new IntOrString(8081), "HTTP"));
            probe.setInitialDelaySeconds(livenessProbe.getInitialDelaySeconds() == null ? DEFAULT_INITIAL_DELAY_SECONDS : livenessProbe.getInitialDelaySeconds());
            probe.setTimeoutSeconds(livenessProbe.getTimeoutSeconds() == null ? DEFAULT_TIMEOUT_SECONDS : livenessProbe.getTimeoutSeconds());
            probe.setPeriodSeconds(livenessProbe.getPeriodSeconds() == null ? DEFAULT_PERIOD_SECONDS : livenessProbe.getPeriodSeconds());
            probe.setSuccessThreshold(livenessProbe.getSuccessThreshold() == null ? DEFAULT_SUCCESS_THRESHOLD : livenessProbe.getSuccessThreshold());
            probe.setFailureThreshold(livenessProbe.getFailureThreshold() == null ? DEFAULT_FAILURE_THRESHOLD : livenessProbe.getFailureThreshold());

            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setLivenessProbe(probe);
        }

        return deployment;
    }

    /**
     * create service
     */
    private Service createService(SchemaRegistry schemaRegistry, Deployment deployment, String namespace) {
        String serviceName = serviceName(schemaRegistry);
        Labels labels = Labels.generateDefaultLabels(schemaRegistry.getMetadata().getName(), schemaRegistry.getKind(),
                COMPONENT, serviceName, OperatorConfig.STRIMZI_CLUSTER_OPERATOR_NAME);

        Service service = loadYaml(Service.class, "service.yaml");
        service.getMetadata().setName(serviceName(schemaRegistry));
        service.getMetadata().setNamespace(namespace);
        service.getMetadata().setLabels(labels.toMap());

        service.getSpec().setSelector(
                Labels.fromMap(deployment.getSpec().getTemplate().getMetadata().getLabels())
                        .strimziSelectorLabels().toMap()
        );

        // service type
        SchemaRegistrySpec.External external = schemaRegistry.getSpec().getExternal();
        ServiceType serviceType = external == null ? ServiceType.ClusterIP
                : ServiceType.getTypeByName(external.getType());
        switch (serviceType) {
            case NodePort:
                service.getSpec().getPorts().get(0).setNodePort(external.getNodeport());
            case LoadBalancer:
                if (StringUtils.isNoneEmpty(external.getLoadbalanceIp())) {
                    service.getSpec().setLoadBalancerIP(external.getLoadbalanceIp());
                }
                if (CollectionUtils.isNotEmpty(external.getLoadbalancerSourceRanges())) {
                    service.getSpec().setLoadBalancerSourceRanges(external.getLoadbalancerSourceRanges());
                }
                service.getSpec().getPorts().get(0).setNodePort(external.getNodeport());
            default:
                service.getSpec().setType(serviceType.name());
                break;
        }

        return service;
    }

    private static String deploymentName(SchemaRegistry schemaRegistry) {
        return String.format("%s-%s", schemaRegistry.getMetadata().getName(), COMPONENT);
    }

    private static String serviceName(SchemaRegistry schemaRegistry) {
        return String.format("%s-%s-service", schemaRegistry.getMetadata().getName(), COMPONENT);
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = getClass().getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }
}
