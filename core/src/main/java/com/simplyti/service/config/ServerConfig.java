package com.simplyti.service.config;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent=true)
public class ServerConfig {
	
	private final String name;
	private final int blockingThreadPool;
	private final int insecuredPort;
	private final int securedPort;
	private final boolean externalEventLoopGroup;
	private final boolean verbose;
	private final int maxBodySize;
	
	public ServerConfig(String name, int blockingThreadPool, int insecuredPort, int securedPort, 
			boolean externalEventLoopGroup,boolean verbose, int maxBodySize){
		this.name=name;
		this.blockingThreadPool=blockingThreadPool;
		this.insecuredPort=insecuredPort;
		this.securedPort=securedPort;
		this.externalEventLoopGroup=externalEventLoopGroup;
		this.verbose=verbose;
		this.maxBodySize=maxBodySize;
	}
	
}
