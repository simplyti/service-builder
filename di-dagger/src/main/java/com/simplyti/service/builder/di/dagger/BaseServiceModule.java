package com.simplyti.service.builder.di.dagger;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import com.simplyti.service.DefaultStartStopMonitor;
import com.simplyti.service.ServerConfig;
import com.simplyti.service.StartStopMonitor;
import com.simplyti.service.builder.di.EventLoopGroupProvider;
import com.simplyti.service.builder.di.ExecutorServiceProvider;
import com.simplyti.service.builder.di.NativeIO;
import com.simplyti.service.builder.di.StartStopLoop;
import com.simplyti.service.builder.di.StartStopLoopProvider;
import com.simplyti.service.builder.di.dagger.apibuilder.APIBuilderModule;
import com.simplyti.service.builder.di.dagger.defaultbackend.DefaultBackendModule;
import com.simplyti.service.channel.ClientChannelGroup;
import com.simplyti.service.channel.DefaultHttpEntryChannelInit;
import com.simplyti.service.channel.DefaultServiceChannelInitializer;
import com.simplyti.service.channel.EntryChannelInit;
import com.simplyti.service.channel.ServerChannelFactoryProvider;
import com.simplyti.service.channel.ServiceChannelInitializer;
import com.simplyti.service.channel.handler.ChannelExceptionHandler;
import com.simplyti.service.channel.handler.ServerHeadersHandler;
import com.simplyti.service.channel.handler.inits.HandlerInit;
import com.simplyti.service.exception.DefaultExceptionHandler;
import com.simplyti.service.exception.ExceptionHandler;
import com.simplyti.service.filter.http.HttpRequestFilter;
import com.simplyti.service.filter.http.HttpResponseFilter;
import com.simplyti.service.json.DslJsonModule;
import com.simplyti.service.ssl.SslHandlerFactory;
import com.simplyti.service.sync.DefaultSyncTaskSubmitter;
import com.simplyti.service.sync.SyncTaskSubmitter;

import dagger.Module;
import dagger.Provides;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

@Module(includes= { Multibindings.class, APIBuilderModule.class, DefaultBackendModule.class, DslJsonModule.class})
public class BaseServiceModule {
	
	@Provides
	@Singleton
	public EventLoopGroup eventLoopGroup(Optional<NativeIO> nativeIO) {
		return new EventLoopGroupProvider(nativeIO).get();
	}
	
	@Provides @Singleton @StartStopLoop
	public EventLoop startStopLoop(ServerConfig config, Optional<NativeIO> nativeIO, ServerConfig serverConfig) {
		return new StartStopLoopProvider(nativeIO).get();
	}
	
	@Provides
	@Singleton
	public ServerConfig serverConfig(
			@Nullable @Named("name") String name,
			@Nullable @Named("blockingThreadPool") Integer blockingThreadPool, 
			@Nullable @Named("insecuredPort") Integer insecuredPort, 
			@Nullable @Named("securedPort") Integer  securedPort,
			@Nullable @Named("verbose") Boolean verbose) {
		return new ServerConfig(name,
				firstNonNull(blockingThreadPool, 500),
				firstNonNull(insecuredPort, 8080),
				firstNonNull(securedPort, 8443), 
				false, 
				firstNonNull(verbose, false));
	}
	
	private static <T> T firstNonNull(T o1, T o2) {
		if(o1 != null){
			return o1;
		}
		return o2;
	}

	@Provides
	@Singleton
	public ChannelFactory<ServerChannel> channelFactory(Optional<NativeIO> nativeIO) {
		return new ServerChannelFactoryProvider(nativeIO).get();
	}
	
	@Provides
	@Singleton
	public EntryChannelInit entryChannelInit() {
		return new DefaultHttpEntryChannelInit();
	}

	@Provides
	@Singleton
	public StartStopMonitor startStopMonitor() {
		return new DefaultStartStopMonitor();
	}
	

	@Provides
	@Singleton
	public ClientChannelGroup clientChannelGroup(@StartStopLoop EventLoop startStopLoop) {
		return new ClientChannelGroup(startStopLoop);
	}

	@Provides
	@Singleton
	public ExceptionHandler exceptionHandler() {
		return new DefaultExceptionHandler();
	}

	@Provides
	@Singleton
	public ChannelExceptionHandler apiExceptionHandler(ExceptionHandler exceptionHandler) {
		return new ChannelExceptionHandler(exceptionHandler);
	}
	
	@Provides
	@Singleton
	public ExecutorService executorService(ServerConfig serverConfig) {
		return new ExecutorServiceProvider(serverConfig).get();
	}

	@Provides
	@Singleton
	public SyncTaskSubmitter syncTaskSubmitter(ExecutorService executorService) {
		return new DefaultSyncTaskSubmitter(executorService);
	}

	@Provides
	@Singleton
	public ServerHeadersHandler serverHeadersHandler(ServerConfig serverConfig) {
		return new ServerHeadersHandler(serverConfig);
	}
	
	@Provides
	@Singleton
	public ServiceChannelInitializer serviceChannelInitializer(ClientChannelGroup clientChannelGroup,
			Optional<SslHandlerFactory> sslHandlerFactory, StartStopMonitor startStopMonitor,
			ChannelExceptionHandler channelExceptionHandler,
			Set<HandlerInit> handlers, Set<HttpRequestFilter> requestFilters, Set<HttpResponseFilter> responseFilters,
			EntryChannelInit entryChannelInit, ServerConfig serverConfig) {
		return new DefaultServiceChannelInitializer(clientChannelGroup, serverConfig, sslHandlerFactory.orElse(null),
				startStopMonitor, channelExceptionHandler, handlers, requestFilters, responseFilters,
				entryChannelInit);
	}
	
}