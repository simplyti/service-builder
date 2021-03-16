package com.simplyti.service.channel;

import javax.inject.Inject;

import com.simplyti.service.channel.handler.ServerHeadersHandler;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DefaultHttpEntryChannelInit implements EntryChannelInit {
	
	private final ServerHeadersHandler serverHeadersHandler;
	
	@Override
	public void init(ChannelPipeline pipeline) {
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false));
		pipeline.addLast(serverHeadersHandler);
		pipeline.addLast(new HttpContentDecompressor());
	}

}