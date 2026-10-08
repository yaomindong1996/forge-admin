package com.mdframe.forge.starter.plugin.license;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "forge.license")
public class RuntimeLicenseProperties {
    private boolean enabled;
    private String customerId;
    private String installationId;
    private List<Path> files = List.of();
    private Map<String, Path> publicKeys = Map.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getInstallationId() { return installationId; }
    public void setInstallationId(String installationId) { this.installationId = installationId; }
    public List<Path> getFiles() { return files; }
    public void setFiles(List<Path> files) { this.files = files; }
    public Map<String, Path> getPublicKeys() { return publicKeys; }
    public void setPublicKeys(Map<String, Path> publicKeys) { this.publicKeys = publicKeys; }
}
