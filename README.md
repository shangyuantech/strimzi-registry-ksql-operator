# Strimzi Schema Registry and KsqlDB Operator
---

Run a Schema Registry and KsqlDB Operator based on https://github.com/java-operator-sdk/java-operator-sdk .

# Schema Registry Operator

Try to use the basic configuration of `Strimzi Kafka Operator` and pull `confluentinc/cp-schema-registry` image to run operator.
Currently, only non SSL Kafka clusters are supported to create `Schema Registry` service. The yaml file is as follows for reference.

```yaml
apiVersion: kafka.strimzi.io/v1beta1
kind: SchemaRegistry
metadata:
  namespace: kafka
  name: example-schemaregistry
spec:
  version: 2.6.0
  replicas: 1
  bootstrapServers: 'PLAINTEXT://kafka-brokers:9092'
  # https://docs.confluent.io/5.5.0/schema-registry/installation/config.html#sr-configuration-options
  config:
    kafkastore.group.id: example-schemaregistry
    kafkastore.init.timeout.ms: '12000'
    kafkastore.timeout.ms: '12000'
  otherProps:
    TZ: 'Asia/Shanghai'
  image: confluentinc/cp-schema-registry:6.0.0
  resources:
    requests:
      cpu: 200m
      memory: 64Mi
    limits:
      cpu: 500m
      memory: 256Mi
  external:
    type: nodeport
#    loadbalanceIp: ""
#    loadbalancerSourceRanges: [""]
    nodeport: 30881
  template:
    pod:
      imagePullSecrets:
      - pullsecret
      terminationGracePeriodSeconds: 120
  readinessProbe:
    initialDelaySeconds: 30
    timeoutSeconds: 5
  livenessProbe:
    initialDelaySeconds: 30
    timeoutSeconds: 5
```

# KsqlDB Operator

Not started yet