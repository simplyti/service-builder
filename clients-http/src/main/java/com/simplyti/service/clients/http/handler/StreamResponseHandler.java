package com.simplyti.service.clients.http.handler;

import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.simplyti.service.clients.ClientRequestChannel;
import com.simplyti.service.clients.http.exception.HttpException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.LastHttpContent;

public class StreamResponseHandler extends SimpleChannelInboundHandler<HttpObject> {

	private final ClientRequestChannel<Void> clientChannel;
	private final Consumer<ByteBuf> consumer;
	private final List<String> originalHandlers;
	private final boolean checkStatusCode;

	public StreamResponseHandler(ClientRequestChannel<Void> clientChannel, Consumer<ByteBuf> consumer, boolean checkStatusCode) {
		this.clientChannel=clientChannel;
		this.consumer=consumer;
		this.checkStatusCode=checkStatusCode;
		this.originalHandlers = StreamSupport.stream(clientChannel.pipeline().spliterator(),false)
				.map(entry->entry.getKey())
				.collect(Collectors.toList());
	}
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		clientChannel.setFailure(new ClosedChannelException());
		clientChannel.release();
    }

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		if(msg instanceof HttpResponse && checkStatusCode && isError(((HttpResponse) msg).status().codeClass())) {
			clientChannel.setFailure(new HttpException(((HttpResponse) msg).status().code()));
		}
		
		if(!clientChannel.isDone() && msg instanceof HttpContent && ((HttpContent) msg).content().isReadable()) {
			if(consumer!=null) {
				consumer.accept(((HttpContent) msg).content());
			}else {
				ctx.fireChannelRead(((HttpContent) msg).content().retain());
			}
		}
		
		if(msg instanceof LastHttpContent) {
			if(!clientChannel.isDone()) {
				clientChannel.setSuccess(null);
			}
			cleanChannel(ctx.channel());
			clientChannel.release();
		}
	}
	
	private boolean isError(HttpStatusClass codeClass) {
		return codeClass.equals(HttpStatusClass.CLIENT_ERROR) || 
				codeClass.equals(HttpStatusClass.SERVER_ERROR);
	}

	private void cleanChannel(Channel channel) {
		List<String> toRemove = StreamSupport.stream(channel.pipeline().spliterator(),false)
			.map(entry->entry.getKey())
			.filter(name->!originalHandlers.contains(name))
			.collect(Collectors.toList());
		toRemove.forEach(name->channel.pipeline().remove(name));
	}


}
