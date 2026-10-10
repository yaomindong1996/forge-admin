package com.mdframe.forge.starter.plugin.delivery;

import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptor;

import java.util.List;

/** 预览只出摘要，不返回源码内容或绝对来源路径。 */
public record SourcePluginPackage(PluginDescriptor descriptor, String sha256, int archiveBytes,
                                  long expandedBytes, int fileCount, List<FileSummary> files) {
    public record FileSummary(String path, int bytes, String sha256) {
    }
}
