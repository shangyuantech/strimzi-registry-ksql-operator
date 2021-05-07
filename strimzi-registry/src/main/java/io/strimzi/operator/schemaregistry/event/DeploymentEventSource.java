package io.strimzi.operator.schemaregistry.event;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;
import static io.strimzi.operator.config.OperatorConfig.STRIMZI_CLUSTER_OPERATOR_NAME;
import static io.strimzi.operator.model.Labels.KUBERNETES_MANAGED_BY_LABEL;
import static io.strimzi.operator.model.Labels.STRIMZI_KIND_LABEL;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.strimzi.operator.schemaregistry.controller.SchemaRegistryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentEventSource extends AbstractEventSource implements Watcher<Deployment> {

    private static final Logger log = LoggerFactory.getLogger(DeploymentEventSource.class);

    private final KubernetesClient client;

    public static DeploymentEventSource createAndRegisterWatch(KubernetesClient client) {
        DeploymentEventSource deploymentEventSource = new DeploymentEventSource(client);
        deploymentEventSource.registerWatch();
        return deploymentEventSource;
    }

    private DeploymentEventSource(KubernetesClient client) {
        this.client = client;
    }

    private void registerWatch() {
        client
                .apps()
                .deployments()
                .inAnyNamespace()
                .withLabel(KUBERNETES_MANAGED_BY_LABEL, STRIMZI_CLUSTER_OPERATOR_NAME)
                .withLabel(STRIMZI_KIND_LABEL, "SchemaRegistry")
                .watch(this);
    }

    @Override
    public boolean reconnecting() {
        return true;
    }

    @Override
    public void eventReceived(Action action, Deployment deployment) {
        log.debug(
                "Event received for action: {}, Deployment: {}",
                action.name(),
                deployment.getMetadata().getName());

        if (action == Action.ERROR) {
            log.warn(
                    "Skipping {} event for custom resource uid: {}, version: {}",
                    action,
                    getUID(deployment),
                    getVersion(deployment));
            return;
        }

        eventHandler.handleEvent(new DeploymentEvent(action, deployment, this));
    }

    @Override
    public void onClose(WatcherException e) {
        if (e == null) {
            log.debug("Close deployment event source.");
            return;
        }
        if (e.isHttpGone()) {
            log.warn("Received error for watch, will try to reconnect.", e);
            registerWatch();
        } else {
            // Note that this should not happen normally, since fabric8 client handles reconnect.
            // In case it tries to reconnect this method is not called.
            log.error("Unexpected error happened with watch. Will exit.", e);
            System.exit(1);
        }
    }
}

