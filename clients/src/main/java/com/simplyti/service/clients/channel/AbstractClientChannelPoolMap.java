package com.simplyti.service.clients.channel;

import java.nio.channels.ClosedChannelException;

import com.simplyti.service.clients.channel.proxy.NoResolvingSocketAddress;
import com.simplyti.service.clients.endpoint.Endpoint;
import com.simplyti.service.clients.endpoint.ssl.SSLEndpoint;
import com.simplyti.service.clients.monitor.ClientMonitorHandler;
import com.simplyti.service.clients.monitor.MonitoredHandler;
import com.simplyti.service.clients.proxy.ProxiedEndpoint;
import com.simplyti.service.clients.proxy.Proxy;
import com.simplyti.util.concurrent.DefaultFuture;
import com.simplyti.util.concurrent.Future;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.concurrent.Promise;

public abstract class AbstractClientChannelPoolMap extends AbstractChannelPoolMap<Endpoint, ChannelPool> implements ClientChannelFactory {
	
	private final Bootstrap bootstrap;
	private final ChannelPoolHandler handler;
	private final EventLoopGroup eventLoopGroup;
	private final SslProvider sslProvider;

	public AbstractClientChannelPoolMap(Bootstrap bootstrap, EventLoopGroup eventLoopGroup, ChannelPoolHandler handler,
			SslProvider sslProvider, ClientMonitorHandler monitor) {
		this.bootstrap=bootstrap;
		this.eventLoopGroup=eventLoopGroup;
		this.handler=monitor!=null?new MonitoredHandler(monitor,handler):handler;
		this.sslProvider=sslProvider;
	}

	@Override
	public Future<ClientChannel> channel(Endpoint endpoint, long responseTimeoutMillis) {
		ChannelPool pool = get(endpoint);
		EventLoop loop = eventLoopGroup.next();
		Promise<ClientChannel> promise = loop.newPromise();
		io.netty.util.concurrent.Future<Channel> futureChannel = pool.acquire();
		futureChannel.addListener(f->{
			if(f.isSuccess()) {
				Channel channel = futureChannel.getNow();
				if(channel.isActive()) {
					PooledClientChannel pooledClient = new PooledClientChannel(pool,endpoint.address(), channel, responseTimeoutMillis);
					if(ChannelInitializedHandler.isNew(channel)) {
						channel.pipeline().addLast(new ChannelInitializedHandler(pooledClient, promise));
						channel.pipeline().fireUserEventTriggered(ClientChannelEvent.INIT);
					} else {
						promise.setSuccess(pooledClient);
					}
				} else {
					pool.release(futureChannel.getNow());
					promise.setFailure(new ClosedChannelException());
				}
			} else {
				promise.setFailure(f.cause());
			}
		});
		return new DefaultFuture<>(promise, loop);
	}

	@Override
	protected ChannelPool newPool(Endpoint key) {
		if(key.isProxied()) {
			ProxiedEndpoint proxy = (ProxiedEndpoint) key;
			return newPool(remoteAddress(key), handler(key),proxy.proxy());
		} else {
			return newPool(remoteAddress(key), handler(key),null);
		}
		
	}

	private Bootstrap remoteAddress(Endpoint key) {
		if(key.isProxied()) {
			return bootstrap.clone().remoteAddress(new NoResolvingSocketAddress(key.address()));
		} else {
			return bootstrap.clone().remoteAddress(key.address().host(), key.address().port());
		}
	}

	private ChannelPoolHandler handler(Endpoint key) {
		if(isSsl(key) && !key.isProxied()) {
			return new SSLChannelInitializeHandler(sslProvider, handler, key);
		}else {
			return handler;
		}
	}

	private boolean isSsl(Endpoint key) {
		return (key.schema()!=null && key.schema().ssl()) || key instanceof SSLEndpoint;
	}

	protected abstract ChannelPool newPool(Bootstrap bootstrap, ChannelPoolHandler handler, Proxy proxy);

}