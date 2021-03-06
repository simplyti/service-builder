package com.simplyti.service.api.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.simplyti.service.api.ApiInvocationContext;
import com.simplyti.service.api.ApiOperation;
import com.simplyti.service.api.serializer.json.TypeLiteral;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.concurrent.Future;

public abstract class FinishableApiBuilder<I,O> {
	
	private static final int DEFAULT_MAX_BODY = 10000000;
	
	protected final ApiBuilder builder;
	protected final HttpMethod method;
	protected final String uri;
	protected final TypeLiteral<I> requestType;
	protected final boolean multipart;
	
	protected boolean notFoundOnNull;
	protected int maxBodyLength;
	
	private Map<String, String> metadata;
	
	public FinishableApiBuilder(ApiBuilder builder, HttpMethod method, String uri, TypeLiteral<I> requestType, boolean multipart,
			int maxBodyLength, boolean notFoundOnNull) {
		this.builder=builder;
		this.method=method;
		this.uri=uri;
		this.requestType=requestType;
		this.multipart=multipart;
		this.maxBodyLength=maxBodyLength;
		this.notFoundOnNull=notFoundOnNull;
	}
	
	public FinishableApiBuilder<I,O> withMeta(String name, String value) {
		if(metadata ==null) {
			metadata = new HashMap<>();
		}
		metadata.put(name, value);
		return this;
	}
	
	public void then(Consumer<ApiInvocationContext<I, O>> consumer) {
		PathPattern pathPattern = PathPattern.build(uri);
		builder.add(new ApiOperation<I,O,ApiInvocationContext<I, O>>(method, pathPattern,consumer,requestType,pathPattern.literalCount(),
				multipart,noNegative(maxBodyLength,DEFAULT_MAX_BODY),metadata(),false,notFoundOnNull));
	}
	
	public void thenFuture(Function<ApiInvocationContext<I,O>,Future<O>> futureFunction) {
		then(new InvocationFutureHandle<I,O>(futureFunction));
	}
	
	public void thenFutureHttp(Function<ApiInvocationContext<I,FullHttpResponse>,Future<FullHttpResponse>> futureFunction) {
		Consumer<ApiInvocationContext<I, FullHttpResponse>> consumer = new InvocationHttpFutureHandle<I,FullHttpResponse>(futureFunction);
		PathPattern pathPattern = PathPattern.build(uri);
		builder.add(new ApiOperation<I,FullHttpResponse,ApiInvocationContext<I, FullHttpResponse>>(method, pathPattern,consumer,requestType,pathPattern.literalCount(),
				multipart,noNegative(maxBodyLength,DEFAULT_MAX_BODY),metadata(),false,notFoundOnNull));
	}
	
	private Map<String,String> metadata() {
		if(metadata==null) {
			return Collections.emptyMap();
		}else {
			return new HashMap<>(metadata);
		}
	}

	private int noNegative(int value,int defaultValue) {
		if(value<0) {
			return defaultValue;
		}else {
			return value;
		}
	}
	
	public FinishableApiBuilder<I,O> withNotFoundOnNull() {
		this.notFoundOnNull=true;
		return this;
	}
	

}
