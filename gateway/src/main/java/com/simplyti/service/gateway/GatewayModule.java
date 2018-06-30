package com.simplyti.service.gateway;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.simplyti.service.api.builder.ApiProvider;
import com.simplyti.service.channel.handler.DefaultBackendRequestHandler;
import com.simplyti.service.clients.http.HttpClient;
import com.simplyti.service.gateway.api.GatewayApi;

public class GatewayModule extends AbstractModule{
	
	@Override
	public void configure() {
		bind(DefaultBackendRequestHandler.class).to(GatewayRequestHandler.class);
		bind(HttpClient.class).toProvider(HttpClientProvider.class);
		
		Multibinder.newSetBinder(binder(), ApiProvider.class).addBinding().to(GatewayApi.class);
	}

}