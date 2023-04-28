package org.apache.nifi.registry.db;

import org.testcontainers.containers.MSSQLServerContainer;

public class MSSQLCustomContainer extends MSSQLServerContainer {
    public MSSQLCustomContainer(String dockerImageName) {
        super(dockerImageName);
    }
}
