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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for Bilibili document reader.
 * Tests are only run if BILIBILI_SESSDATA environment variable is set.
 *
 * Run with environment variables:
 * export BILIBILI_SESSDATA=your_sessdata
 * export BILIBILI_BILI_JCT=your_bili_jct
 * export BILIBILI_BUVID3=your_buvid3
 * mvn verify
 *
 * @author zshs000
 */
@EnabledIfEnvironmentVariable(named = "BILIBILI_SESSDATA", matches = ".+")
public class BilibiliDocumentReaderIT {

	private static final Logger logger = LoggerFactory.getLogger(BilibiliDocumentReaderIT.class);

	// Static initializer to log a message if credentials are not set
	static {
		if (System.getenv("BILIBILI_SESSDATA") == null || System.getenv("BILIBILI_SESSDATA").isEmpty()) {
			System.out.println(
					"Skipping Bilibili document reader tests because BILIBILI_SESSDATA environment variable is not set.");
		}
	}

	private BilibiliCredentials credentials;

	@BeforeEach
	void setUp() {
		// Read Bilibili credentials from environment variables
		String sessdata = System.getenv("BILIBILI_SESSDATA");
		String biliJct = System.getenv("BILIBILI_BILI_JCT");
		String buvid3 = System.getenv("BILIBILI_BUVID3");

		// Only create credentials if sessdata is provided
		if (sessdata != null && biliJct != null) {
			BilibiliCredentials.Builder builder = BilibiliCredentials.builder()
				.sessdata(sessdata)
				.biliJct(biliJct);

			if (buvid3 != null) {
				builder.buvid3(buvid3);
			}

			credentials = builder.build();
		}
	}

	/**
	 * Test reading a single Bilibili video with credentials.
	 */
	@Test
	void testSingleVideoWithCredentials() {
		// Skip test if credentials are not available
		Assumptions.assumeTrue(credentials != null, "Skipping test because credentials are not set");

		// Create resource
		BilibiliResource resource = new BilibiliResource(
				"https://www.bilibili.com/video/BV1XCcEzfEye/?spm_id_from=333.1007.tianma.1-1-1.click&vd_source=fe3a4ab4558efe9a3455cc73d1a1daaa",
				credentials);

		// Create reader and get documents
		BilibiliDocumentReader reader = new BilibiliDocumentReader(resource);
		List<Document> documents = reader.get();

		// Assertions
		assertNotNull(documents);
		assertFalse(documents.isEmpty());
		logger.info("Retrieved {} documents", documents.size());

		// Log document contents
		for (int i = 0; i < documents.size(); i++) {
			Document doc = documents.get(i);
			logger.info("Document {}: {}", i, doc.getText());
			logger.info("Metadata: {}", doc.getMetadata());
		}
	}

	/**
	 * Test reading multiple Bilibili videos with shared credentials.
	 */
	@Test
	void testMultipleVideosWithSharedCredentials() {
		// Skip test if credentials are not available
		Assumptions.assumeTrue(credentials != null, "Skipping test because credentials are not set");

		// Create multiple resources with the same credentials
		List<BilibiliResource> resources = List.of(new BilibiliResource("BV1XCcEzfEye", credentials),
				new BilibiliResource("BV1rPZJBeE9N", credentials));

		// Create reader with resource list
		BilibiliDocumentReader reader = new BilibiliDocumentReader(resources);
		List<Document> documents = reader.get();

		// Assertions
		assertNotNull(documents);
		assertFalse(documents.isEmpty());
		logger.info("Retrieved {} documents from {} videos", documents.size(), resources.size());

		// Log document contents
		for (int i = 0; i < documents.size(); i++) {
			Document doc = documents.get(i);
			logger.info("Document {}: {}", i, doc.getText().substring(0, Math.min(1000, doc.getText().length())));
		}
	}

}
