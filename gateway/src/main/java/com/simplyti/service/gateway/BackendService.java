package com.simplyti.service.gateway;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.MoreObjects;
import com.simplyti.service.api.builder.PathPattern;
import com.simplyti.service.api.filter.HttpRequetFilter;
import com.simplyti.service.clients.Endpoint;
import com.simplyti.service.gateway.balancer.RoundRobinLoadBalancer;
import com.simplyti.service.gateway.balancer.ServiceBalancer;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent=true)
@AllArgsConstructor
@EqualsAndHashCode(of= {"host","method","path"})
@ToString(of= {"host","method","path", "loadBalander"})
public class BackendService implements Comparable<BackendService>{
	
	private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());
	
	private final String host;
	private final HttpMethod method;
	private final String path;
	private final Set<HttpRequetFilter> filters;
	private final Pattern pattern;
	private final int literalCount;
	
	private ServiceBalancer loadBalander;

	public BackendService(String host, HttpMethod method, String path, Set<HttpRequetFilter> filters, Collection<Endpoint> endpoints) {
		this.loadBalander = new RoundRobinLoadBalancer(endpoints);
		this.host=host;
		this.method=method;
		this.path=path;
		this.filters=MoreObjects.firstNonNull(filters, Collections.emptySet());
		if(path==null) {
			this.pattern = null;
			literalCount=0;
		}else {
			PathPattern thePattern = PathPattern.build(path);
			if(thePattern.pathParamNameToGroup().isEmpty()) {
				PathPattern pathPattern = PathPattern.build(path.replaceAll("/+$",  StringUtil.EMPTY_STRING)+"/{any:.*}");
				this.pattern = pathPattern.pattern();
				this.literalCount = pathPattern.literalCount();
			}else {
				this.pattern = thePattern.pattern();
				this.literalCount = thePattern.literalCount();
			}
		}
	}

	@Override
	public int compareTo(BackendService other) {
		int compare = compareHost(other);
		if(compare==0) {
			compare = compareMethod(other);
			if(compare ==0) {
				return comparePath(other);
			}else{
				return compare;
			}
		}else{
			return compare;
		}
	}

	private int compareMethod(BackendService other) {
		if(method() == null) {
			if(other.method() == null) {
				return 0;
			}else {
				return 1;
			}
		}else {
			if(other.method() == null) {
				return -1;
			}else {
				return method().compareTo(other.method());
			}
		}
	}

	private int compareHost(BackendService other) {
		if(host() == null) {
			if(other.host() == null) {
				return 0;
			}else {
				return 1;
			}
		}else {
			if(other.host() == null) {
				return -1;
			}else {
				return host().compareTo(other.host());
			}
		}
	}

	private int comparePath(BackendService other) {
		if(path() == null) {
			if(other.path() == null) {
				return 0;
			}else {
				return 1;
			}
		}else {
			if(other.path() == null) {
				return -1;
			}else {
				int literalParts = other.literalCount()-literalCount();
				if(literalParts==0) {
					return this.path().compareTo(other.path());
				}else {
					return literalParts;
				}
			}
		}
	}

	public void add(Endpoint endpoint) {
		this.loadBalander = loadBalander.add(endpoint);
		log.info("Added service endpoint: {}",endpoint);
	}
	
	public void delete(Endpoint endpoint) {
		this.loadBalander = loadBalander.delete(endpoint);
		log.info("Deleted service endpoint: {}",endpoint);
	}

	public void clear() {
		this.loadBalander = loadBalander.clear();
		log.info("Cleared service endpoints");
	}

	public Set<HttpRequetFilter> filters() {
		return filters;
	}

}
