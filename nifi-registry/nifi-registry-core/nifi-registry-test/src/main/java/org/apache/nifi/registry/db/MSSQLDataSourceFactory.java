package org.apache.nifi.registry.db;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import javax.annotation.PostConstruct;
import javax.script.ScriptException;
import javax.sql.DataSource;
import java.sql.SQLException;

public abstract class MSSQLDataSourceFactory extends TestDataSourceFactory{
    protected abstract MSSQLServerContainer mssqlContainer();

    @Override
    protected DataSource createDataSource() {
        final MSSQLServerContainer container = mssqlContainer();
        final SQLServerDataSource dataSource = new SQLServerDataSource();
        dataSource.setURL(container.getJdbcUrl());
        dataSource.setUser(container.getUsername());
        dataSource.setPassword(container.getPassword());
        dataSource.setDatabaseName(container.getDatabaseName());
        return dataSource;
    }

    @PostConstruct
    public void initDatabase() throws SQLException, ScriptException {
        DatabaseDelegate databaseDelegate = new JdbcDatabaseDelegate(mssqlContainer(), "");
        databaseDelegate.execute("DROP DATABASE test; CREATE DATABASE test;", "", 0, false, true);
    }
}
