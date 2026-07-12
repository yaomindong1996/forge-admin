/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.toolcalling.python;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Python Tool powered by GraalVM Polyglot.
 *
 * This class provides configuration options for the GraalVM Engine and Context
 * used to execute Python code in a sandboxed environment.
 *
 * @author guotao
 * @since 2026/1/2
 */
@ConfigurationProperties(prefix = PythonConstants.CONFIG_PREFIX)
public class PythonProperties {

    private boolean enabled = true;

    /**
     * Engine configuration options
     */
    private Engine engine = new Engine();

    /**
     * Context sandbox security configuration
     */
    private Context context = new Context();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * GraalVM Engine configuration
     */
    public static class Engine {

        /**
         * Whether to warn about interpreter-only execution mode.
         * Default is false to suppress warnings.
         */
        private boolean warnInterpreterOnly = false;

        public boolean isWarnInterpreterOnly() {
            return warnInterpreterOnly;
        }

        public void setWarnInterpreterOnly(boolean warnInterpreterOnly) {
            this.warnInterpreterOnly = warnInterpreterOnly;
        }

    }

    /**
     * GraalVM Context sandbox configuration
     */
    public static class Context {

        /**
         * Allow all access (overrides all other permissions).
         * Default is false for security.
         */
        private boolean allowAllAccess = false;

        /**
         * Allow file I/O operations.
         * Default is false for security.
         */
        private boolean allowIO = false;

        /**
         * Allow native code execution.
         * Default is false for security.
         */
        private boolean allowNativeAccess = false;

        /**
         * Allow creating new processes.
         * Default is false for security.
         */
        private boolean allowCreateProcess = false;

        /**
         * Allow access to Java host classes and objects.
         * Default is true to enable Python-Java interop.
         */
        private boolean allowHostAccess = true;

        /**
         * GraalVM Context options (e.g., python.Executable, python.PythonPath).
         */
        private Map<String, String> options = new HashMap<>();

        public boolean isAllowAllAccess() {
            return allowAllAccess;
        }

        public void setAllowAllAccess(boolean allowAllAccess) {
            this.allowAllAccess = allowAllAccess;
        }

        public boolean isAllowIO() {
            return allowIO;
        }

        public void setAllowIO(boolean allowIO) {
            this.allowIO = allowIO;
        }

        public boolean isAllowNativeAccess() {
            return allowNativeAccess;
        }

        public void setAllowNativeAccess(boolean allowNativeAccess) {
            this.allowNativeAccess = allowNativeAccess;
        }

        public boolean isAllowCreateProcess() {
            return allowCreateProcess;
        }

        public void setAllowCreateProcess(boolean allowCreateProcess) {
            this.allowCreateProcess = allowCreateProcess;
        }

        public boolean isAllowHostAccess() {
            return allowHostAccess;
        }

        public void setAllowHostAccess(boolean allowHostAccess) {
            this.allowHostAccess = allowHostAccess;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public void setOptions(Map<String, String> options) {
            this.options = options;
        }

    }

}
