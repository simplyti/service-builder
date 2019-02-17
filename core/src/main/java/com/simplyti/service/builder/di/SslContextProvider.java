package com.simplyti.service.builder.di;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SslContextProvider implements Provider<SslContext>{
	
	private final KeyManagerFactory keyManagerFactory;
	
	private final TrustManagerFactory trustManagerFactory;

	@Override
	public SslContext get() {
		return Try.of(()->SslContextBuilder.forServer(keyManagerFactory)
				.trustManager(trustManagerFactory)
				.build()).get();
	}
	
}
