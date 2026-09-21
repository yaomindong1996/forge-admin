package com.mdframe.forge.plugin.print.spi;

/**
 * Provider 核验来源后，由编排服务创建；不是请求体。
 */
public record AuthorizedPrintSource(PrintActor actor, PrintSourceRequest source) {
}
