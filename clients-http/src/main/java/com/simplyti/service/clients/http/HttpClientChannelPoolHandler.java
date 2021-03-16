package com.simplyti.service.clients.http;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.simplyti.service.clients.http.handler.HttpClientFullResponseAggregator;
import com.simplyti.service.clients.http.handler.HttpContentUnwrapHandled;
import com.simplyti.service.clients.http.handler.HttpRequestFilterHandler;
import com.simplyti.service.clients.http.handler.HttpServerSentEventDecoder;
import com.simplyti.service.clients.http.handler.HttpServerSentEventHandshakeHandled;
import com.simplyti.service.clients.http.handler.SetHostHeaderHandler;
import com.simplyti.service.filter.http.HttpRequestFilter;
import com.simplyti.service.filter.priority.Priorized;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

public class HttpClientChannelPoolHandler extends AbstractChannelPoolHandler {
	
	private static final ByteBuf DELIMITER = Unpooled.copiedBuffer("\n\n", CharsetUtil.UTF_8);

	private final SetHostHeaderHandler setHostHeaderHandler;
	private final long readTimeoutMillis;

	private final List<HttpRequestFilter> filters;

	public HttpClientChannelPoolHandler(long readTimeoutMillis, List<HttpRequestFilter> filters) {
		this.setHostHeaderHandler = new SetHostHeaderHandler();
		this.readTimeoutMillis=readTimeoutMillis;
		this.filters=filters==null?null:filters.stream().sorted(Priorized.PRIORITY_ANN_ORDER).collect(Collectors.toList());
	}

	@Override
	public void channelCreated(Channel ch) throws Exception {
		if(readTimeoutMillis >0) {
			ch.pipeline().addLast(new ReadTimeoutHandler(readTimeoutMillis, TimeUnit.MILLISECONDS));
		}
		ch.pipeline().addLast(new HttpClientCodec());
		
		ch.pipeline().addLast(new HttpRequestFilterHandler(filters));
		
		ch.pipeline().addLast(setHostHeaderHandler);
		
		ch.pipeline().addLast(new HttpServerSentEventHandshakeHandled());
		ch.pipeline().addLast(new DelimiterBasedFrameDecoder(100000, DELIMITER));
		ch.pipeline().addLast(new HttpServerSentEventDecoder());
		
		ch.pipeline().addLast(new HttpClientFullResponseAggregator(52428800));
		ch.pipeline().addLast(new HttpContentUnwrapHandled());
		
	}
	
}