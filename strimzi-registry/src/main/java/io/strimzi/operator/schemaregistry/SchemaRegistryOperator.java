package io.strimzi.operator.schemaregistry;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import java.io.IOException;

import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import io.strimzi.operator.config.OperatorConfig;
import io.strimzi.operator.schemaregistry.controller.SchemaRegistryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.http.Exit;
import org.takes.http.FtBasic;

public class SchemaRegistryOperator {

    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryOperator.class);

    public static void main(String[] args) throws IOException {
        log.info("SchemaRegistry Operator starting!");

        OperatorConfig operatorConfig = OperatorConfig.fromMap(System.getenv());
        log.info("init Operator Config = \n {}", operatorConfig);

        Config config = new ConfigBuilder().withNamespace(null).build();
        KubernetesClient client = new DefaultKubernetesClient(config);
        Operator operator = new Operator(client, DefaultConfigurationService.instance());
        operator.register(new SchemaRegistryController(client, operatorConfig));

        new FtBasic(new TkFork(new FkRegex("/health", "ALL GOOD!")), 8080).start(Exit.NEVER);
    }
}
