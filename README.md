# Kubernetes Autoscaling

A good intro is [here](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/). 
Another good walkthrough (with pictures) is [here](https://learnk8s.io/autoscaling-apps-kubernetes). 
The complete e-2-e walkthrough for the implementation based on Prometheus adapter is [here](https://hackernoon.com/how-to-use-prometheus-adapter-to-autoscale-custom-metrics-deployments-p1p3tl0). 
An example for OpenShift can be found [here](https://docs.openshift.com/container-platform/4.1/monitoring/exposing-custom-application-metrics-for-autoscaling.html). 

Quick validation based on [this](https://blog.kloia.com/kubernetes-hpa-externalmetrics-prometheus-acb1d8a4ed50)
and leverages HPA

![Grafana](images/hpa.png)

## Install Kind for Autoscaling

Install kind. On Mac this is easy [using brew](https://kind.sigs.k8s.io/docs/user/quick-start/). Do not forget to set
[docker parameters](https://kind.sigs.k8s.io/docs/user/quick-start/#settings-for-docker-desktop) for it.

Start kind cluster  
````
kind create cluster
````

Install metrics server. By default its not on kind. Also be aware that default metric server install is using secure communications, requiring a certificate.
To make it simpler, it supports insecure communications. I was following [instructions](https://computingforgeeks.com/how-to-deploy-metrics-server-to-kubernetes-cluster/)
  
Download manifest file from [here](https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml)

Modify it as [here](deployments/metricserver/components.yaml)

Deploy it 
````
kubectl apply -f <your location>components.yaml
````

verify that deployment is running 
````
kubectl get deployment metrics-server -n kube-system
````
Also verify that top command is working correctly 
````
kubectl top nodes
````
 
## HPA with Default metrics

Create ngnix deployment with this [file](deployments/default/ngnixdeployment.yaml) using 
````
kubectl apply -f <your location>ngnixdeployment.yaml
````

Make sure pods are running.

Create HPA with [this](deployments/default/hpa.yaml) using 
````
kubectl apply -f <your location>hpa.yaml  
````
To validate that it works run 
````
kubectl get hpa
````
***Note*** that the amount of replicas is now 2 (minimal amount of replicas in HPA is 2).

Clean up. Run 
````
kubectl delete deployment nginx-deployment
kubectl delete hpa nginx-cpu-hpa
````  

## HPA with Custom metrics

This follows this great [writeup](https://github.com/kubernetes-sigs/prometheus-adapter/blob/master/docs/walkthrough.md)

Create sample app using [this yaml](deployments/custommetric/sample-app.yaml). 
````
kubectl apply -f <your location>sample-app.yaml
````
Once deployed, access it by running 
````
kubectl port-forward service/sample-app 8080:80
```` 

Now we can look at the metrics at `http://localhost:8080/metric`

Install prometheus operator. I was following this [document](https://grafana.com/docs/grafana-cloud/quickstart/prometheus_operator/). 

First install operator bundle 
````
kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/master/bundle.yaml
````

Configure Prometheus RBAC Permissions using [this yaml](deployments/custommetric/prometheus_rbac.yaml) by running 
````
kubectl apply -f <your location>/prometheus_rbac.yaml
````

Create prometheus using [this yaml](deployments/custommetric/prometheus.yaml) by running 
````
kubectl apply -f <your location>/prometheus.yaml 
````
Make sure that prometheus is created and pods are running:
````
kubectl get prometheus
```` 

Create service monitor using [this yaml](deployments/custommetric/servicemonitor.yaml) by running
````
kubectl apply -f <your location>/servicemonitor.yaml
````

Create a Prometheus Service using [this yaml](deployments/custommetric/prometheus_service.yaml) by running 
````
kubectl apply -f <your location>/prometheus_service.yaml
```` 

To access the service run 
````
kubectl port-forward svc/prometheus 9090
````
and go to `http://localhost:9090/` to see UI.

You should see your metrics `http_requests_total`. You should see something like
````
http_requests_total{container="metrics-provider",endpoint="http",instance="10.244.0.16:8080",job="sample-app",namespace="default",pod="sample-app-7cfb596f98-pkv8t",service="sample-app"}	20
````

Creating Prometheus Adapter done using Helm chart as described [here](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus-adapter). 

````
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install prometheus-adapter prometheus-community/prometheus-adapter
````

To verify that everything is installed correctly run 
````
kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1  | jq .
```` 

and make sure that 
````
 {
      "name": "pods/http_requests",
      "singularName": "",
      "namespaced": true,
      "kind": "MetricValueList",
      "verbs": [
        "get"
      ]
    }
````    
is there.

Check the value of the metric 
````
kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/default/pods/*/http_requests?selector=app%3Dsample-app"  | jq .
````

You should see:

````
{
  "kind": "MetricValueList",
  "apiVersion": "custom.metrics.k8s.io/v1beta1",
  "metadata": {
    "selfLink": "/apis/custom.metrics.k8s.io/v1beta1/namespaces/default/pods/%2A/http_requests"
  },
  "items": [
    {
      "describedObject": {
        "kind": "Pod",
        "namespace": "default",
        "name": "sample-app-7cfb596f98-s74rh",
        "apiVersion": "/v1"
      },
      "metricName": "http_requests",
      "timestamp": "2021-05-11T14:08:21Z",
      "value": "66m",
      "selector": null
    }
  ]
}
````

Create the HorizontalPodAutoscaler using [this yaml](deployments/custommetric/sample-app-hpa.yaml) by running 
````
kubectl apply -f <your location>/sample-app-hpa.yaml
````
To validate that it works run
````
kubectl get hpa
````

Watch scaling

Additionally I tried to do the same on Minikube. The only difference there is that I did not have to install metric server manually.
It is part of Minikube addons and can be installed through [configuration](https://kubernetes.io/docs/tutorials/hello-minikube/)

## Using Kafka

One of the most common use cases for HPA on streaming applications is scaling based on the topic(s) lag.
Let`s install Kafka strimzi with Prometheus and see how put it all together.

Start by kind install, as described above and add metric server to it.

Now follow a great [article](https://snourian.com/kafka-kubernetes-strimzi-part-3-monitoring-strimzi-kafka-with-prometheus-grafana/)

Install Strimzi operator:
* Create ns - `kubectl create ns kafka`
* Add Helm repo -
````  
helm repo add strimzi https://strimzi.io/charts/
helm repo update
````  
* Install Strimzi operator with Helm
````
helm install strimzi strimzi/strimzi-kafka-operator --namespace kafka
````
Create Kafka cluster. [Definition](deployments/kafka/kafka-metrics.yaml) is cloned [from](https://github.com/strimzi/strimzi-kafka-operator/blob/0.21.1/examples/metrics/kafka-metrics.yaml).
Here, because we are running on Kind, we are using 1 zookeeper and 1 broker.
File has metrics key for both kafka and zookeeper, it also need to add [Kafka Exporter](https://strimzi.io/docs/operators/latest/deploying.html#proc-kafka-exporter-configuring-str) configs 
to enable [additional metrics](https://strimzi.io/blog/2019/10/14/improving-prometheus-metrics/). To create cluster run:
````
kubectl apply -f <your location>/kafka-metrics.yaml -n kafka
````
More info on Kafka exporter configuration is [here](https://strimzi.io/docs/operators/latest/full/deploying.html#proc-kafka-exporter-configuring-str)
Detail description (along with installation chart) is [here](https://github.com/danielqsj/kafka_exporter)

Create namespace monitoring:

````
kubectl create ns monitoring
````
Now we can install Prometheus operator. To ensure that it is deployed to monitoring ns
we copied bundle [locally](deployments/kafka/bundle.yaml)
````
kubectl apply -f <your location>/bundle.yaml
````

Use [yaml](deployments/kafka/prometheus-additional.yaml) for additional scraping properties. Cloned from [here](https://github.com/strimzi/strimzi-kafka-operator/blob/0.21.1/examples/metrics/prometheus-additional-properties/prometheus-additional.yaml) to create secret:
````
kubectl apply -f <your location>/prometheus-additional.yaml -n monitoring
````
Create strimzi monitor using this [yaml](deployments/kafka/strimzi-pod-monitor.yaml) cloned from [here](https://github.com/strimzi/strimzi-kafka-operator/blob/0.21.1/examples/metrics/prometheus-install/strimzi-pod-monitor.yaml)
````
kubectl apply -f <your location>/strimzi-pod-monitor.yaml -n monitoring
````
Create prometheus using this [yaml](deployments/kafka/prometheus.yaml) cloned from [here](https://github.com/strimzi/strimzi-kafka-operator/blob/0.21.1/examples/metrics/prometheus-install/prometheus.yaml)
````
kubectl apply -f <your location>/prometheus.yaml -n monitoring
````
Install Grafana using this [yaml](deployments/kafka/grafana.yaml) cloned from [here](https://github.com/strimzi/strimzi-kafka-operator/blob/0.21.1/examples/metrics/grafana-install/grafana.yaml)
````
kubectl apply -f <your location>/grafana.yaml -n monitoring
````
Expose grafana using port-forward:
````
kubectl port-forward svc/grafana 3000:3000 -n monitoring
````
Now go to `localhost:3000` to get to UI. Default credentials are admin/admin.
Add Prometheus as a new Data Source. Inside the Settings tap, you need to enter Prometheus address - `http://prometheus-operated:9090` and validate that its
working. Add dashboards to see the results. We used 4 - cloned from [here](https://github.com/strimzi/strimzi-kafka-operator/tree/0.21.1/examples/metrics/grafana-dashboards)

Deploy Kafka consumer and producer:
````
kubectl apply -f <your location>/consumer.yaml
kubectl apply -f <your location>/producer.yaml
````

***Note*** in order for kafka consumers to be scalable, make sure that you have enough partitions in the kafka deployment

Deploy Kafka consumer and producer:
````
kubectl apply -f <your location>/consumer.yaml
kubectl apply -f <your location>/producer.yaml
````

Once consumer and producer are deployed, you can look at the grafana kafka exporter
dashboard, that shows speed of producing and consuming messages and the lag,
that can be used as an information for HPA

![Grafana](images/consumer_lag.png)

Usage HPA with Kafka is described [here](https://medium.com/@ranrubin/horizontal-pod-autoscaling-hpa-triggered-by-kafka-event-f30fe99f3948)

Install prometheus adapter using [Helm chart](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus-adapter)

````
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install prometheus-adapter prometheus-community/prometheus-adapter -f /Users/boris/Projects/KubernetesAutoscaling/deployments/kafka/custommetric/values.yaml --namespace monitoring
````
To validate the installation run the following command:

````
kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1 | jq .
````
It shold return:

````
{
  "kind": "APIResourceList",
  "apiVersion": "v1",
  "groupVersion": "custom.metrics.k8s.io/v1beta1",
  "resources": [
    {
      "name": "pods/kafka_consumergroup_lag",
      "singularName": "",
      "namespaced": true,
      "kind": "MetricValueList",
      "verbs": [
        "get"
      ]
    }
  ]
}
````
To see the metrics value run:

````
get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/kafka/pods/*/kafka_consumergroup_lag" | jq .
````
Note in the above that we specify namespace kafka here. This is because kafka exporter, that collects 
this information runs in namespace kafka.

Running this command return:

````
{
  "kind": "MetricValueList",
  "apiVersion": "custom.metrics.k8s.io/v1beta1",
  "metadata": {
    "selfLink": "/apis/custom.metrics.k8s.io/v1beta1/namespaces/kafka/pods/%2A/kafka_consumergroup_lag"
  },
  "items": [
    {
      "describedObject": {
        "kind": "Pod",
        "namespace": "kafka",
        "name": "my-cluster-kafka-exporter-9774b9f48-jq5hs",
        "apiVersion": "/v1"
      },
      "metricName": "kafka_consumergroup_lag",
      "timestamp": "2021-05-07T16:05:56Z",
      "value": "2",
      "selector": null
    }
  ]
}
````
When we look at the metric value, we notice that it returns data only for an exporter pod.
This means that in the HPA we need to use a custom metrics not of the object that we are 
scaling (kafka consumer), but rather another object - kafka exporter.
As defined [here](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/#autoscaling-on-multiple-metrics-and-custom-metrics)
The object metrics describe a different object in the same namespace, instead of describing Pods. 
The metrics are not necessarily fetched from the object; they only describe it. 
Based on this, an HPA yaml is defined as [folows](deployments/kafka/custommetric/kafka-consumer-hpa.yaml).
We can apply this using:
````
kubectl apply -f <your-location>kafka-consumer-hpa.yaml -n kafka
````
***Note*** Because object metrics requires that the object is in the namespace as the scaling pod, kafka producers and consumers (and HPA) have to be deployed in the same namespace
as kafka exporter

Now we can verify that everything is working correctly:

````
kubectl get hpa kafka-consumer-hpa -n kafka
````
Which returns:
````
NAME                 REFERENCE                              TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
kafka-consumer-hpa   Deployment/kafka-consumer-deployment   3875m/5 (avg)   1         10        4          90m
````
***Note*** that average value specified in HPA configuration is per pod.

In addition to base configuration presented here, additional features, such as scaling behavior tuning and a stabilization window
can be configured. See [here](https://granulate.io/kubernetes-autoscaling-the-hpa/) for more details.

## Using KEDA

This is based on the [article](https://faun.pub/event-driven-autoscaling-for-kubernetes-with-kafka-keda-d68490200812)

[KEDA](https://keda.sh/) is a Kubernetes-based Event Driven Autoscaler. With KEDA, you can drive the scaling of any 
container in Kubernetes based on the number of events needing to be processed.KEDA works alongside standard Kubernetes 
components like the horizontal pod autoscaler (HPA) and can extend functionality without overwriting or duplication.

![Keda](images/keda.png)

To install Keda run the following set of command:

````
helm repo add kedacore https://kedacore.github.io/charts
helm repo update
kubectl create namespace keda
helm install keda kedacore/keda --namespace keda
````
Install Strimzi and kafka instance, as above:

````
helm repo add strimzi https://strimzi.io/charts/
helm repo update
kubectl create namespace kafka
helm install strimzi strimzi/strimzi-kafka-operator --namespace kafka
kubectl apply -f <your location>/kafka-metrics.yaml -n kafka
````
Install Prometheus and Grafana as above

````
kubectl create ns monitoring
kubectl apply -f <your location>/bundle.yaml
kubectl apply -f <your location>/prometheus-additional.yaml -n monitoring
kubectl apply -f <your location>/strimzi-pod-monitor.yaml -n monitoring
kubectl apply -f <your location>/prometheus.yaml -n monitoring
kubectl apply -f <your location>/grafana.yaml -n monitoring
````
Start consumer and producer as above using:

````
kubectl apply -f <your location>/consumer.yaml
kubectl apply -f <your location>/producer.yaml
````
To start autoscaling, create a keda `ScaledObject` as shown [here](deployments/keda/kafka-scaled-object.yaml)
and deploy it with the following command:

````
kubectl create -f  <your location>/kafka-scaled-object.yaml
````
To make sure that keda is running, execute:

````
kubectl get ScaledObject
````

You should see:
````
NAME                 SCALETARGETKIND      SCALETARGETNAME             MIN   MAX   TRIGGERS   AUTHENTICATION   READY   ACTIVE   AGE
kafka-scaledobject   apps/v1.Deployment   kafka-consumer-deployment   1     10    kafka                       True    True     8m38s
````

Starting from Kubernetes v1.18 the autoscaling API allows scaling behavior to be configured through the 
[HPA behavior](https://keda.sh/docs/2.2/concepts/scaling-deployments/)