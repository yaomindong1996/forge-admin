/*
 * Copyright 2026-2027 the original author or authors.
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
package com.alibaba.cloud.ai.reader.csdn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link CsdnDocumentReader} using real network requests.
 *
 * <p>
 * To run:
 * <pre>
 * set CSDN_IT_ENABLED=true
 * set CSDN_ARTICLE_URL=<csdn-article-url>
 * mvn -pl document-readers/spring-ai-alibaba-starter-document-reader-csdn -Dtest=CsdnDocumentReaderIT test
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "CSDN_IT_ENABLED", matches = "true")
class CsdnDocumentReaderIT {

	private static final Logger logger = LoggerFactory.getLogger(CsdnDocumentReaderIT.class);

	private static final String DEFAULT_URL =
			"https://blog.csdn.net/zsy520real/article/details/157656758?spm=1001.2014.3001.5502";

	@Test
	void shouldReadRealCsdnArticleAndPrintLogs() {
		String url = System.getenv("CSDN_ARTICLE_URL");
		if (url == null || url.isBlank()) {
			url = DEFAULT_URL;
		}

		CsdnDocumentReader reader = new CsdnDocumentReader(url);
		List<Document> documents = reader.get();

		assertThat(documents).hasSize(1);
		Document document = documents.get(0);
		assertThat(document.getText()).isNotBlank();

		String text = document.getText();
		int previewLength = Math.min(300, text.length());
		String preview = text.substring(0, previewLength).replaceAll("\\s+", " ").trim();

		logger.info("CSDN IT url: {}", url);
		logger.info("CSDN IT metadata: {}", document.getMetadata());
		logger.info("CSDN IT text length: {}", text.length());
		logger.info("CSDN IT text preview (first {} chars): {}", previewLength, preview);
	}

}
