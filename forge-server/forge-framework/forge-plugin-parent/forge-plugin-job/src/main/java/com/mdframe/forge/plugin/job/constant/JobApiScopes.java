package com.mdframe.forge.plugin.job.constant;

import java.util.Set;

public final class JobApiScopes {

    public static final String JOBS_READ = "jobs:read";
    public static final String JOBS_TRIGGER = "jobs:trigger";
    public static final String EXECUTIONS_READ = "executions:read";

    public static final Set<String> ALLOWED = Set.of(JOBS_READ, JOBS_TRIGGER, EXECUTIONS_READ);

    private JobApiScopes() {
    }
}
