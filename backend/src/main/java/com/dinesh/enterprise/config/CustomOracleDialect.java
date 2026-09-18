package com.dinesh.enterprise.config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

/**
 * Custom Oracle dialect for Oracle 10g XE (Release 10.2.0.1.0).
 *
 * <p>Oracle 10g does not support the ISO SQL:2008 "FETCH FIRST N ROWS ONLY" syntax
 * which Hibernate 6 emits by default. This dialect overrides {@link #getLimitHandler()}
 * to return {@link LegacyOracleLimitHandler}, which generates ROWNUM-based pagination
 * queries that are fully compatible with Oracle 10g.</p>
 *
 * <p>Without this override, all paginated queries (products, inventory, warehouses,
 * orders, analytics) fail with ORA-00933: SQL command not properly ended.</p>
 */
public class CustomOracleDialect extends OracleDialect {

    private static final DatabaseVersion ORACLE_10G = DatabaseVersion.make(10, 2);
    private static final LimitHandler LEGACY_LIMIT_HANDLER = new LegacyOracleLimitHandler(ORACLE_10G);

    public CustomOracleDialect() {
        super(ORACLE_10G);
    }

    @Override
    public LimitHandler getLimitHandler() {
        // LegacyOracleLimitHandler wraps queries with ROWNUM (Oracle 8i/9i/10g compatible)
        // instead of the modern "FETCH FIRST N ROWS ONLY" syntax (Oracle 12c+)
        return LEGACY_LIMIT_HANDLER;
    }

    @Override
    public boolean supportsFetchClause(org.hibernate.query.sqm.FetchClauseType type) {
        // Oracle 10g does NOT support FETCH FIRST / OFFSET ROWS syntax (introduced in Oracle 12c)
        return false;
    }
}

