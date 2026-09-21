package com.mdframe.forge.plugin.print.vo;

public record PrintAvailableTemplateVO(Long id, String templateName, Long templateVersionId, Integer versionNo, boolean isDefault, int sortOrder) {
}
