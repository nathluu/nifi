/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
