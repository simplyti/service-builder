package com.simplyti.service.clients.http.request;

public interface FinishableHttpRequestBuilder extends BaseFinishableHttpRequestBuilder<FinishableHttpRequestBuilder>, HeaderAppendableRequestBuilder<FinishableHttpRequestBuilder>, ParamAppendableRequestBuilder<FinishableHttpRequestBuilder>, FilterableRequestBuilder<FinishableHttpRequestBuilder> {

}