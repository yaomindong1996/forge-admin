package com.mdframe.forge.plugin.job.support;

public record IssuedJobApiToken(
        String token,
        String keyId,
        String prefix,
        String tokenHash) {
}
