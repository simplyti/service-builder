package com.simplyti.service.examples.api;

import java.util.stream.Collectors;

import com.simplyti.service.api.builder.ApiBuilder;
import com.simplyti.service.api.builder.ApiProvider;

public class APIUploadTest implements ApiProvider{

	@Override
	public void build(ApiBuilder builder) {
		builder.when().post("/upload")
			.asFileUpload()
			.then(ctx->{
				ctx.send("Got "+ctx.body().stream().map(file->file.filename()+" ("+file.content().readableBytes()+"b)").collect(Collectors.toList()));
			});
	}

}
