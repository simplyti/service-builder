package com.simplyti.service.gateway;

import java.net.InetSocketAddress;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.ServiceUnavailableException;

import com.simplyti.service.ServerConfig;
import com.simplyti.service.api.filter.FilterChain;
import com.simplyti.service.channel.handler.DefaultBackendRequestHandler;
import com.simplyti.service.clients.Endpoint;
import com.simplyti.service.clients.InternalClient;
import com.simplyti.service.commons.netty.pending.PendingMessages;
import com.simplyti.service.gateway.handler.BackendProxyHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class GatewayRequestHandler extends DefaultBackendRequestHandler {
	
	private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());
	
	private final ServiceDiscovery serviceDiscovery;
	private final InternalClient client;
	private final ServerConfig config;

	private final PendingMessages pendingMessages;
	
	private boolean frontSsl;
	private Channel backendChannel;
	private boolean ignoreNextMessages;


	@Inject
	public GatewayRequestHandler(InternalClient client, ServiceDiscovery serviceDiscovery, ServerConfig config) {
		super(false);
		this.client = client;
		this.config=config;
		this.serviceDiscovery = serviceDiscovery;
		this.pendingMessages = new PendingMessages();
	}
	
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
		this.frontSsl = config.securedPort()==localAddress.getPort();
    }

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		final Object rewritedRequest;
		if (msg instanceof HttpRequest) {
			HttpRequest request = (HttpRequest) msg;
			QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
			BackendServiceMatcher service = serviceDiscovery.get(request.headers().get(HttpHeaderNames.HOST),request.method(), decoder.path());
			if (service == null) {
				ctx.fireExceptionCaught(new NotFoundException());
				this.ignoreNextMessages=true;
				return;
			} else {
				rewritedRequest = service.rewrite(request);
				if(service.get().filters().isEmpty()) {
					serviceProceed(ctx,service.get());
				}else {
					filterRequest(ctx,service.get(),request);
				}
			}
		}else {
			rewritedRequest = msg;
		}
		
		if (backendChannel != null) {
			backendChannel.writeAndFlush(rewritedRequest).addListener(f->handleWriteFuture(ctx,f));
		} else if(!ignoreNextMessages){
			pendingMessages.pending(ctx.executor().newPromise(), rewritedRequest);
		}
	}
	
	private void filterRequest(ChannelHandlerContext ctx, BackendService service, HttpRequest request) {
		Future<Boolean> futureHandled = FilterChain.of(service.filters(),ctx,request).execute();
		futureHandled.addListener(result->{
			if(ctx.executor().inEventLoop()) {
				handleFilterResult(ctx,futureHandled,service);
			}else {
				ctx.executor().execute(()->handleFilterResult(ctx,futureHandled,service));
			}
		});
	}

	private void handleFilterResult(ChannelHandlerContext ctx, Future<Boolean> futureHandled, BackendService service) {
		if(futureHandled.isSuccess()) {
			if(futureHandled.getNow()) {
				pendingMessages.fail(new RuntimeException("Handled by filter"));
				this.ignoreNextMessages=true;
			}else {
				serviceProceed(ctx,service);
			}
		}else {
			ctx.fireExceptionCaught(futureHandled.cause());
			pendingMessages.fail(futureHandled.cause());
			this.ignoreNextMessages=true;
		}
	}

	private void serviceProceed(ChannelHandlerContext ctx, BackendService service) {
		Endpoint endpoint = service.loadBalander().next();
		if (endpoint == null) {
			ctx.fireExceptionCaught(new ServiceUnavailableException());
			pendingMessages.fail(new RuntimeException("No endpoints"));
			this.ignoreNextMessages=true;
		} else {
			connectToEndpoint(ctx, endpoint);
		}
	}

	private void connectToEndpoint(ChannelHandlerContext ctx, Endpoint endpoint) {
		ChannelPool pool = client.pool(endpoint);
		Future<Channel> channelFuture = pool.acquire();
		if (channelFuture.isDone()) {
			handleBackendChannelFuture(ctx, channelFuture, pool, endpoint);
		} else {
			channelFuture.addListener(f -> handleBackendChannelFuture(ctx, channelFuture, pool, endpoint));
		}
	}
	
	private void handleBackendChannelFuture(ChannelHandlerContext ctx, Future<Channel> backendChannelFuture,
			ChannelPool pool, Endpoint endpoint) {
		if(ctx.executor().inEventLoop()) {
			handleBackendChannelFuture0(ctx,backendChannelFuture,pool,endpoint);
		}else {
			ctx.executor().execute(()->handleBackendChannelFuture0(ctx,backendChannelFuture,pool,endpoint));
		}
	}
	
	private void handleBackendChannelFuture0(ChannelHandlerContext ctx, Future<Channel> backendChannelFuture,
			ChannelPool pool, Endpoint endpoint) {
		if (backendChannelFuture.isSuccess()) {
			backendChannelFuture.getNow().pipeline().addLast(new BackendProxyHandler(pool, ctx.channel(),endpoint,frontSsl));
			this.backendChannel=backendChannelFuture.getNow();
			pendingMessages.write(backendChannel).addListener(f->handleWriteFuture(ctx, f));
		}else {
			log.warn("Cannot connect to backend {}: {}", endpoint, backendChannelFuture.cause().toString());
			ctx.fireExceptionCaught(new ServerErrorException(HttpResponseStatus.BAD_GATEWAY.code()));
			pendingMessages.fail(backendChannelFuture.cause());
			ignoreNextMessages=true;
		}
	}

	@Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().config().setAutoRead(false);
    }
	
	@Override
	 public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().config().setAutoRead(true);
    }

	private void handleWriteFuture(ChannelHandlerContext ctx,Future<?> f) {
		if(f.isSuccess()) {
			ctx.channel().read();
		}else {
			ctx.channel().close();
		}
	}

}
