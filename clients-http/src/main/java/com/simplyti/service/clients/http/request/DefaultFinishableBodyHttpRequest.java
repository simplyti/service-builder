package com.simplyti.service.clients.http.request;

import java.util.function.Function;

import com.simplyti.service.clients.ClientConfig;
import com.simplyti.service.clients.InternalClient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class DefaultFinishableBodyHttpRequest extends AbstractFinishableHttpRequest implements FinishableBodyHttpRequest {

	private final HttpMethod method;
	private final String uri;
	
	private ByteBuf nullableBody;
	
	private HttpHeaders headers;

	public DefaultFinishableBodyHttpRequest(InternalClient client,boolean checkStatusCode, HttpMethod method, String uri, HttpHeaders headers, ClientConfig config) {
		super(client, checkStatusCode,config);
		this.method = method;
		this.uri = uri;
		this.headers=headers;
	}
	
	@Override
	public FinishableHttpRequest body(Function<ByteBufAllocator, ByteBuf> bodySupplier) {
		this.nullableBody = bodySupplier.apply(ByteBufAllocator.DEFAULT);
		return this;
	}
	
	protected FullHttpRequest request0() {
		final ByteBuf body;
		if(nullableBody!=null) {
			body=nullableBody;
		}else {
			body = Unpooled.EMPTY_BUFFER;
		}
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri,body,headers,EmptyHttpHeaders.INSTANCE);
		setHostHeader(request);
		request.headers().set(HttpHeaderNames.CONTENT_LENGTH,body.readableBytes());
		return request;
	}

}
