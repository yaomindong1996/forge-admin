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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

/**
 * @author guotao
 * @since 2026/1/2
 */
public class PythonConstants {

    public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".python";

    public static final String TOOL_NAME = "pythonExecute";

    public static final String DESCRIPTION = """
            Executes Python code and returns the result.

            Usage:
            - The code parameter must be valid Python code
            - The tool will execute the code and return the output
            - If the code produces a result, it will be returned as a string
            - Errors will be caught and returned as error messages
            - The execution is sandboxed for security

            Examples:
            - Simple calculation: code = "2 + 2" returns "4"
            - String operations: code = "'Hello, ' + 'World'" returns "Hello, World"
            - List operations: code = "[1, 2, 3][0]" returns "1"
            """;
}
