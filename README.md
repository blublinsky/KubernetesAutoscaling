#Kubernetes Autoscaling

A good intro is [here](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/). 
Another good walkthrough (with pictures) is [here](https://learnk8s.io/autoscaling-apps-kubernetes). 
The complete e-2-e walkthrough for the implementation based on Prometheus adapter is [here](https://hackernoon.com/how-to-use-prometheus-adapter-to-autoscale-custom-metrics-deployments-p1p3tl0). 
An example for OpenShift can be found [here](https://docs.openshift.com/container-platform/4.1/monitoring/exposing-custom-application-metrics-for-autoscaling.html). 

Quick validation based on [this](https://blog.kloia.com/kubernetes-hpa-externalmetrics-prometheus-acb1d8a4ed50)

* Install kind. On Mac this is easy [using brew](https://kind.sigs.k8s.io/docs/user/quick-start/). Do not forget to set
[docker parameters](https://kind.sigs.k8s.io/docs/user/quick-start/#settings-for-docker-desktop) for it.
* Start kind cluster - `kind create cluster`
* Install metrics server. By default its not on kind. Also be aware that default metric server install is using secure communications, requiring a certificate.
To make it simpler, it supports insecure communications. I was following [instructions](https://computingforgeeks.com/how-to-deploy-metrics-server-to-kubernetes-cluster/)
  * Download manifest file from [here](https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml)
  * Modify it as [here](metricserver/components.yaml)
  * deploy it `kubectl apply -f <your location>components.yaml`  
  * verify that deployment is running `kubectl get deployment metrics-server -n kube-system`. Also verify that top command is working correctly `kubectl top nodes`
 
* Try HPA with default metrics:
  * Create ngnix deployment with this [file](default/ngnixdeployment.yaml) using `kubectl apply -f <your location>ngnixdeployment.yaml`. Make sure pods are running.
  * Create HPA with [this](default/hpa.yaml) using `kubectl apply -f <your location>hpa.yaml`  
  * To validate that it works run `kubectl get hpa`. Note that the amount of replicas is now 2.
  * Clean up. Run `kubectl delete deployment nginx-deployment` and `kubectl delete hpa nginx-cpu-hpa`  

* Try with custom metrics, follows this great writeup (https://github.com/kubernetes-sigs/prometheus-adapter/blob/master/docs/walkthrough.md):
  * Create sample app using [this yaml](custommetric/sample-app.yaml). Once deployed, access it by running `kubectl port-forward service/sample-app 8080:80` and acessing it at `http://localhost:8080/metric`
  * Install prometheus operator. I was following this [document](https://grafana.com/docs/grafana-cloud/quickstart/prometheus_operator/). 
    * First install operator bundle `kubectl apply -f https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/master/bundle.yaml`
    * Configure Prometheus RBAC Permissions using [this yaml](custommetric/prometheus_rbac.yaml) by running `kubectl apply -f <your location>/prometheus_rbac.yaml`.
    * Create prometheus using [this yaml](custommetric/prometheus.yaml) by running `kubectl apply -f <your location>/prometheus.yaml`. Make sure that prometheus is created (`kubectl get prometheus`) pods are running.
    * Create a Prometheus Service using [this yaml](custommetric/prometheus_service.yaml) by running `kubectl apply -f <your location>/prometheus_service.yaml`. To access the service run `kubectl port-forward svc/prometheus 9090` and go to `http://localhost:9090/` to see UI
  * Create service monitor using [this yaml](custommetric/servicemonitor.yaml) by running `kubectl apply -f <your location>/servicemonitor.yaml`. With the monitor in place, go to Prometheus UI and you should see your metrics `http_requests_total`. You should see something like
    `http_requests_total{container="metrics-provider",endpoint="http",instance="10.244.0.16:8080",job="sample-app",namespace="default",pod="sample-app-7cfb596f98-pkv8t",service="sample-app"}	20`
  * Creating Prometheus Adapter done using Helm chart as described [here](https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus-adapter). Alternatively you can use yaml files located in [this directory](custommetric)
  * To verify that everything is installed correctly run `kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1` and make sure that `"name":"pods/http_requests","singularName":"","namespaced":true,"kind":"MetricValueList","verbs":["get"]` is there.
  * Check the value of the metric `kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/default/pods/*/http_requests?selector=app%3Dsample-app"`
  * Create the HorizontalPodAutoscaler using [this yaml](custommetric/sample-app-hpa.yaml) by running `kubectl apply -f <your location>/sample-app-hpa.yaml`.
  * watch scaling

Additionally I tried to do the same on Minikube. The only difference there is that I did not have to install metric server manually.
It is part of Minikube addons and can be installed through [configuration](https://kubernetes.io/docs/tutorials/hello-minikube/)

