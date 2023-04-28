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
