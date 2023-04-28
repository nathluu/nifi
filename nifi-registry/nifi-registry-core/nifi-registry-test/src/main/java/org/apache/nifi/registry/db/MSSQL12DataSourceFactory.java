package org.apache.nifi.registry.db;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.MSSQLServerContainer;

@Configuration
@Profile({"mssql", "mssql-12"})
public class MSSQL12DataSourceFactory extends MSSQLDataSourceFactory{
    private static final MSSQLServerContainer MSSQL_CONTAINER = new MSSQLCustomContainer("mcr.microsoft.com/mssql/server:2022-latest");

    static {
        MSSQL_CONTAINER.start();
    }

    @Override
    protected MSSQLServerContainer mssqlContainer() {
        return MSSQL_CONTAINER;
    }
}
