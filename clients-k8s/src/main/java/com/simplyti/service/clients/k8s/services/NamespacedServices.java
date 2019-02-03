package com.simplyti.service.clients.k8s.services;

import com.simplyti.service.clients.k8s.common.NamespacedK8sApi;
import com.simplyti.service.clients.k8s.services.builder.ServiceBuilder;
import com.simplyti.service.clients.k8s.services.domain.Service;
import com.simplyti.service.clients.k8s.services.updater.ServicesUpdater;

public interface NamespacedServices extends NamespacedK8sApi<Service> {

	ServiceBuilder builder();

	ServicesUpdater update(String name);

}
