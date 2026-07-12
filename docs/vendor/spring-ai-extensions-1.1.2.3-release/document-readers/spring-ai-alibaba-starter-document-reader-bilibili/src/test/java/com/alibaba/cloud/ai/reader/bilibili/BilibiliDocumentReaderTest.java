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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for Bilibili document reader.
 * These tests do not require credentials and can run in CI environments.
 *
 * @author zshs000
 */
public class BilibiliDocumentReaderTest {

	private static final Logger logger = LoggerFactory.getLogger(BilibiliDocumentReaderTest.class);

	/**
	 * Test BV ID extraction from various URL formats.
	 * This test does not require credentials.
	 */
	@Test
	void testBvidExtraction() {
		BilibiliCredentials testCredentials = BilibiliCredentials.builder()
			.sessdata("test_sessdata")
			.biliJct("test_bili_jct")
			.build();

		// Test with full URL
		BilibiliResource resource1 = new BilibiliResource(
				"https://www.bilibili.com/video/BV1XCcEzfEye/?spm_id_from=333.1007.tianma.1-1-1.click",
				testCredentials);
		assertEquals("BV1XCcEzfEye", resource1.getBvid());

		// Test with simple URL
		BilibiliResource resource2 = new BilibiliResource("https://www.bilibili.com/video/BV1xx411c7mD/",
				testCredentials);
		assertEquals("BV1xx411c7mD", resource2.getBvid());

		// Test with raw BV ID
		BilibiliResource resource3 = new BilibiliResource("BV1yy422c8mE", testCredentials);
		assertEquals("BV1yy422c8mE", resource3.getBvid());

		logger.info("BV ID extraction test passed");
	}

	/**
	 * Test invalid BV ID should throw exception.
	 */
	@Test
	void testInvalidBvidThrowsException() {
		BilibiliCredentials testCredentials = BilibiliCredentials.builder()
			.sessdata("test_sessdata")
			.biliJct("test_bili_jct")
			.build();

		// Test with invalid input (no BV ID)
		assertThrows(IllegalArgumentException.class, () -> {
			new BilibiliResource("https://www.youtube.com/watch?v=123", testCredentials);
		});

		assertThrows(IllegalArgumentException.class, () -> {
			new BilibiliResource("hello world", testCredentials);
		});

		assertThrows(IllegalArgumentException.class, () -> {
			new BilibiliResource("invalid_video_id", testCredentials);
		});

		logger.info("Invalid BV ID validation test passed");
	}

	/**
	 * Test credentials builder validation.
	 * This test does not require credentials.
	 */
	@Test
	void testCredentialsValidation() {
		// Valid credentials
		BilibiliCredentials validCredentials = BilibiliCredentials.builder()
			.sessdata("test_sessdata")
			.biliJct("test_bili_jct")
			.buvid3("test_buvid3")
			.build();

		assertNotNull(validCredentials);
		assertEquals("test_sessdata", validCredentials.getSessdata());
		assertEquals("test_bili_jct", validCredentials.getBiliJct());
		assertEquals("test_buvid3", validCredentials.getBuvid3());

		// Missing required field should throw exception
		assertThrows(IllegalArgumentException.class, () -> {
			BilibiliCredentials.builder().biliJct("test_bili_jct").build();
		});

		assertThrows(IllegalArgumentException.class, () -> {
			BilibiliCredentials.builder().sessdata("test_sessdata").build();
		});

		logger.info("Credentials validation test passed");
	}

}
