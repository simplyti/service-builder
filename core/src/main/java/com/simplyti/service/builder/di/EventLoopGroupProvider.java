package com.simplyti.service.builder.di;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class EventLoopGroupProvider implements Provider<EventLoopGroup>{
	
	private final int size;
	private final Optional<NativeIO> nativeIO;

	@Inject
	public EventLoopGroupProvider(Optional<NativeIO> nativeIO) {
		this(0,nativeIO);
	}
	
	public EventLoopGroupProvider(int size,Optional<NativeIO> nativeIO) {
		this.size=size;
		this.nativeIO=nativeIO;
	}

	@Override
	public EventLoopGroup get() {
		return nativeIO.map(n->n.eventLoopGroup(size))
			.orElseGet(()->new NioEventLoopGroup(size));
	}

}
