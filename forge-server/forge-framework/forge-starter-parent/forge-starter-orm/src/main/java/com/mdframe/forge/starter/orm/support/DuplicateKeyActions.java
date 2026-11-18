package com.mdframe.forge.starter.orm.support;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * 跨库幂等写：用「先更新 / 再插入 + 捕获重复键」替代 MySQL {@code ON DUPLICATE KEY}
 * 与 PostgreSQL {@code ON CONFLICT}，新增数据库时无需再写 Mapper 方言分支。
 */
public final class DuplicateKeyActions {

    /** MySQL / MariaDB ER_DUP_ENTRY */
    private static final int MYSQL_DUPLICATE = 1062;
    /** SQL Server violation of UNIQUE KEY / PRIMARY KEY constraint */
    private static final int SQLSERVER_UNIQUE_CONSTRAINT = 2627;
    /** SQL Server unique index duplicate key */
    private static final int SQLSERVER_UNIQUE_INDEX = 2601;
    /** PostgreSQL unique_violation */
    private static final String PG_UNIQUE_SQLSTATE = "23505";

    private DuplicateKeyActions() {
    }

    /** 插入；唯一键冲突时视为已存在，返回 0。 */
    public static int insertIgnoreConflict(Supplier<Integer> insert) {
        try {
            Integer rows = insert.get();
            return rows == null ? 0 : rows;
        } catch (DuplicateKeyException ex) {
            return 0;
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateKey(ex)) {
                return 0;
            }
            throw ex;
        }
    }

    /**
     * 先按唯一键更新；无行再插入；并发下插入撞唯一键则再更新一次。
     *
     * @return 受影响行数（更新或插入）
     */
    public static int upsert(Supplier<Integer> updateByUniqueKey, Supplier<Integer> insert) {
        Integer updated = updateByUniqueKey.get();
        if (updated != null && updated > 0) {
            return updated;
        }
        try {
            Integer inserted = insert.get();
            return inserted == null ? 0 : inserted;
        } catch (DuplicateKeyException ex) {
            Integer retried = updateByUniqueKey.get();
            return retried == null ? 0 : retried;
        } catch (DataIntegrityViolationException ex) {
            if (!isDuplicateKey(ex)) {
                throw ex;
            }
            Integer retried = updateByUniqueKey.get();
            return retried == null ? 0 : retried;
        }
    }

    static boolean isDuplicateKey(DataIntegrityViolationException ex) {
        if (ex instanceof DuplicateKeyException) {
            return true;
        }
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sqlEx) {
                int code = sqlEx.getErrorCode();
                if (code == MYSQL_DUPLICATE
                        || code == SQLSERVER_UNIQUE_CONSTRAINT
                        || code == SQLSERVER_UNIQUE_INDEX) {
                    return true;
                }
                if (PG_UNIQUE_SQLSTATE.equals(sqlEx.getSQLState())) {
                    return true;
                }
            }
        }
        Throwable cause = ex.getMostSpecificCause();
        String message = cause == null ? ex.getMessage() : cause.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("duplicate")
                || lower.contains("unique")
                || lower.contains("constraint");
    }
}
