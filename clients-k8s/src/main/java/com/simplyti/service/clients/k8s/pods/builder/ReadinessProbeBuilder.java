package com.simplyti.service.clients.k8s.pods.builder;

public interface ReadinessProbeBuilder<T> {

	HttpReadinessProbeBuilder<T> http();

}
