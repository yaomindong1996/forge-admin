/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.reader.bilibili;

import org.springframework.util.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Bilibili video resource with authentication credentials.
 *
 * @author zshs000
 */
public class BilibiliResource {

	private final String bvid;

	private final BilibiliCredentials credentials;

	public BilibiliResource(String bvid, BilibiliCredentials credentials) {
		Assert.hasText(bvid, "BV ID must not be empty");
		Assert.notNull(credentials, "Credentials must not be null");
		this.bvid = extractBvid(bvid);
		this.credentials = credentials;
	}

	public String getBvid() {
		return bvid;
	}

	public BilibiliCredentials getCredentials() {
		return credentials;
	}

	/**
	 * Extract BV ID from URL or raw BV ID
	 * @throws IllegalArgumentException if input doesn't contain a valid BV ID
	 */
	private String extractBvid(String input) {
		Pattern pattern = Pattern.compile("(BV\\w+)");
		Matcher matcher = pattern.matcher(input);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException("Invalid Bilibili video identifier: " + input);
	}

}
