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

import com.alibaba.cloud.ai.parser.bshtml.BsHtmlDocumentParser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CsdnDocumentReader}.
 */
class CsdnDocumentReaderTest {

	private static final String URL = "https://blog.csdn.net/test_author/article/details/123456789?spm=test";

	private static final String HTML_FIXTURE = "csdn-fixture.html";

	@Test
	void shouldExtractContentAndMetadataFromFixture() throws IOException {
		String html = readResource();
		CsdnDocumentReader reader = createReader(html);

		List<Document> documents = reader.get();
		assertThat(documents).hasSize(1);

		Document document = documents.get(0);
		String text = document.getText();
		Map<String, Object> metadata = document.getMetadata();
		String title = "测试标题：CSDN Reader Fixture";

		assertThat(text).startsWith("Article Title: " + title);
		assertThat(text).contains("Content: ");
		assertThat(text).contains("这是用于单元测试的正文内容");
		assertThat(text).doesNotContain("recommend-item-box");
		assertThat(text).doesNotContain("kunpeng-sc.csdnimg.cn");
		assertThat(text).doesNotContain("adsbygoogle");

		assertThat(metadata).containsEntry("source", URL);
		assertThat(metadata).containsEntry("title", title);
		assertThat(metadata).containsEntry("author", "test_author");
		assertThat(metadata).containsEntry("article_id", "123456789");
		assertThat(metadata).containsOnlyKeys("source", "article_id", "title", "author");
	}

	@Test
	void shouldKeepAuthorOriginalTextWithConservativeCleanup() throws IOException {
		String html = readResource();
		CsdnDocumentReader reader = createReader(html);

		String text = reader.get().get(0).getText();
		assertThat(text).contains("测试提示：立即体验 智能协同");
		assertThat(text).doesNotContain("raphael-marker-block");
		assertThat(text).doesNotContain("M5,0 0,2.5 5,5z");
	}

	/**
	 * Verifies the fallback path in parseArticleText:
	 * when BsHtmlDocumentParser fails at runtime, the reader should still return
	 * content via Jsoup text extraction instead of throwing an exception.
	 */
	@Test
	void shouldFallbackToJsoupTextWhenHtmlParserFails() throws IOException {
		String html = readResource();
		WebClient webClient = createWebClient(html);
		BsHtmlDocumentParser failingParser = new BsHtmlDocumentParser() {
			@Override
			public List<Document> parse(InputStream inputStream) {
				throw new RuntimeException("forced parser failure for fallback test");
			}
		};
		CsdnDocumentReader reader = new CsdnDocumentReader(URL, webClient, failingParser);

		List<Document> documents = reader.get();
		assertThat(documents).hasSize(1);

		Document document = documents.get(0);
		assertThat(document.getText()).startsWith("Article Title: ");
		assertThat(document.getText()).contains("Content: ");
		assertThat(document.getText()).contains("这是用于单元测试的正文内容");
		assertThat(document.getMetadata()).containsEntry("source", URL);
		assertThat(document.getMetadata()).containsEntry("article_id", "123456789");
	}

	@Test
	void shouldValidateCsdnArticleUrl() {
		assertThatThrownBy(() -> new CsdnDocumentReader("https://www.youtube.com/watch?v=q-9wxg9tQRk"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid CSDN article URL");

		assertThatThrownBy(() -> new CsdnDocumentReader("https://evilcsdn.net/user/article/details/1"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid CSDN article URL");

		assertThatThrownBy(() -> new CsdnDocumentReader(""))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("must not be empty");
	}

	private CsdnDocumentReader createReader(String html) {
		return new CsdnDocumentReader(URL, createWebClient(html));
	}

	private WebClient createWebClient(String html) {
		return WebClient.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
				.body(html)
				.build()))
			.build();
	}

	private String readResource() throws IOException {
		ClassPathResource resource = new ClassPathResource(HTML_FIXTURE);
		try (InputStream inputStream = resource.getInputStream()) {
			return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
		}
	}

}
