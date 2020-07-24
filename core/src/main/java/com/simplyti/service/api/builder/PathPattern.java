package com.simplyti.service.api.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.re2j.Pattern;

import io.netty.util.internal.StringUtil;

public class PathPattern {

	private final Pattern pattern;
	private final Map<String, Integer> pathParamNameToGroup;
	private final int literalCount;
	private final String template;

	public PathPattern(String template, Pattern pattern, Map<String, Integer> pathParamNameToGroup, int literalCount) {
		this.template=template;
		this.pattern=pattern;
		this.pathParamNameToGroup=pathParamNameToGroup;
		this.literalCount=literalCount;
	}
	
	public Pattern pattern() {
		return pattern;
	}
	
	public String template() {
		return template;
	}
	
	public Map<String, Integer> pathParamNameToGroup() {
		return pathParamNameToGroup;
	}
	
	public int literalCount() {
		return literalCount;
	}

	public static PathPattern build(String uri) {
		StringBuilder pathTemplateBuilder = new StringBuilder();
		Map<String, Integer> pathParamNameToGroup = new HashMap<>();
		AtomicInteger pathParamGroupCount = new AtomicInteger(1);
		AtomicReference<StringBuilder> pathParamNameRef = new AtomicReference<>();
		AtomicInteger literalCharsCount = new AtomicInteger();
		
		uri.replaceAll("^/+", StringUtil.EMPTY_STRING).replaceAll("/+$",  StringUtil.EMPTY_STRING).chars()
			.mapToObj(i -> (char) i)
			.forEach(character -> process(character, pathTemplateBuilder, pathParamNameToGroup, pathParamGroupCount,
				pathParamNameRef,literalCharsCount));
		
		pathTemplateBuilder.append("/?$").insert(0, "^/");
		
		return new PathPattern(uri,Pattern.compile(pathTemplateBuilder.toString()),Collections.unmodifiableMap(pathParamNameToGroup),literalCharsCount.get());
	}
	
	private static void process(Character character, StringBuilder pathTemplateBuilder,
			Map<String, Integer> pathParamNameToGroup, AtomicInteger pathParamGroupCount,
			AtomicReference<StringBuilder> pathParamNameRef, AtomicInteger literalCharsCount) {
		if (character.equals('{')) {
			pathParamNameRef.set(new StringBuilder());
		} else if (character.equals('}')) {
			StringBuilder pathParamName = pathParamNameRef.getAndSet(null);
			int regexInit = pathParamName.lastIndexOf(":");
			String regex;
			if (regexInit == -1) {
				regex = "[^/]+";
			} else {
				regex = pathParamName.substring(regexInit + 1);
				pathParamName.replace(regexInit, pathParamName.length(), "");
			}
			pathTemplateBuilder.append("(" + regex + ")");
			pathParamNameToGroup.put(pathParamName.toString(), pathParamGroupCount.getAndIncrement());
		} else if (pathParamNameRef.get() == null) {
			pathTemplateBuilder.append(character);
			literalCharsCount.incrementAndGet();
		} else {
			pathParamNameRef.get().append(character);
		}
	}

}
