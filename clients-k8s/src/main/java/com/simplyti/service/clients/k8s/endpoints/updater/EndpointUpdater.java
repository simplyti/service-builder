package com.simplyti.service.clients.k8s.endpoints.updater;

import com.simplyti.service.clients.k8s.endpoints.builder.EndpointSubsetBuilder;
import com.simplyti.service.clients.k8s.endpoints.domain.Endpoint;

import io.netty.util.concurrent.Future;

public interface EndpointUpdater {

	EndpointSubsetBuilder<? extends EndpointUpdater> addSubset();
	EndpointSubsetBuilder<? extends EndpointUpdater> setSubset();
	
	Future<Endpoint> update();



}