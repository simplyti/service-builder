package com.simplyti.server.http.api.builder;

import java.util.HashMap;
import java.util.Map;

import com.simplyti.server.http.api.context.ApiContextFactory;
import com.simplyti.server.http.api.operations.AnyApiOperation;
import com.simplyti.server.http.api.operations.ApiOperations;
import com.simplyti.server.http.api.pattern.ApiPattern;
import com.simplyti.service.api.serializer.json.TypeLiteral;

import io.netty.handler.codec.http.HttpMethod;

public class ResponseBodyTypableApiBuilderImpl implements ResponseTypableApiBuilder {
	
	private final ApiOperations operations;
	private final HttpMethod method;
	private final String path;
	private final ApiContextFactory responseTypeContextFactory;
	private final ApiContextFactory anyContextFactory;
	
	private Map<String,Object> metadata;

	public ResponseBodyTypableApiBuilderImpl(ApiContextFactory anyContextFactory, ApiContextFactory responseTypeContextFactory,ApiOperations operations, HttpMethod method, String path) {
		this.operations=operations;
		this.method=method;
		this.path=path;
		this.responseTypeContextFactory=responseTypeContextFactory;
		this.anyContextFactory=anyContextFactory;
	}

	@Override
	public <T> ResponseBodyTypedApiBuilder<T> withResponseType(Class<T> clazz) {
		return new ResponseBodyTypedApiBuilderImpl<>(responseTypeContextFactory,operations,method,path);
	}
	
	@Override
	public <T> ResponseBodyTypedApiBuilder<T> withResponseType(TypeLiteral<T> clazz) {
		return new ResponseBodyTypedApiBuilderImpl<>(responseTypeContextFactory,operations,method,path);
	}

	@Override
	public void then(ApiContextConsumer consumer) {
		ApiPattern apiPattern = ApiPattern.build(path);
		operations.add(new AnyApiOperation(method,apiPattern,metadata,consumer,anyContextFactory));
	}

	@Override
	public <T> ResponseBodyTypedApiBuilder<T> withResponseBodyType(Class<T> clazz) {
		return withResponseType(clazz);
	}

	@Override
	public <T> ResponseBodyTypedApiBuilder<T> withResponseBodyType(TypeLiteral<T> clazz) {
		return withResponseType(clazz);
	}

	@Override
	public ResponseTypableApiBuilder withMeta(String key, String value) {
		if(metadata==null) {
			metadata = new HashMap<>();
		}
		metadata.put(key, value);
		return this;
	}
}