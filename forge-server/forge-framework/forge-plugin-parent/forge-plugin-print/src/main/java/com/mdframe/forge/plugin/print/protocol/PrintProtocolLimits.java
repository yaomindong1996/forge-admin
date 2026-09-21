package com.mdframe.forge.plugin.print.protocol;

/**
 * forge-print v1 技术语法限制，与前端 protocol/types.js 同步。
 */
public final class PrintProtocolLimits {

    public static final int DOCUMENT_BYTES = 1024 * 1024;

    public static final int SCHEMA_VERSION = 1;

    public static final int JSON_DEPTH = 64;

    public static final int MAX_ISSUES = 100;

    public static final int SECTIONS = 200;

    public static final int ELEMENTS = 1000;

    public static final int COLUMNS = 50;

    public static final int STATIC_TABLE_COLUMNS = 20;

    public static final int STATIC_TABLE_ROWS = 50;

    public static final int RESOURCES = 100;

    public static final int HEADER_ROWS = 10;

    public static final int TEXT_LENGTH = 100000;

    public static final int INLINE_IMAGE_BYTES = 512 * 1024;

    public static final int PAPER_SIZE_MM = 2000;

    public static final double GEOMETRY_TOLERANCE_MM = 0.001;

    private PrintProtocolLimits() {
    }
}
