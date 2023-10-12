/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oceanbase.connector.flink;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class OceanBaseContainer extends JdbcDatabaseContainer<OceanBaseContainer> {

    public static final String DOCKER_IMAGE_NAME = "oceanbase/oceanbase-ce";

    private static final DockerImageName DEFAULT_IMAGE_NAME =
            DockerImageName.parse(DOCKER_IMAGE_NAME);

    private static final Integer SQL_PORT = 2881;
    private static final Integer RPC_PORT = 2882;

    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_TENANT_NAME = "test";
    private static final String DEFAULT_DATABASE_NAME = "test";

    private String tenantName = DEFAULT_TENANT_NAME;

    public OceanBaseContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public OceanBaseContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.waitStrategy =
                Wait.forLogMessage(".*boot success!.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(5));

        addExposedPorts(SQL_PORT, RPC_PORT);
    }

    @Override
    public String getDriverClassName() {
        return "com.oceanbase.jdbc.Driver";
    }

    public Integer getSqlPort() {
        return getActualPort(SQL_PORT);
    }

    public Integer getActualPort(int port) {
        return "host".equals(getNetworkMode()) ? port : getMappedPort(port);
    }

    @Override
    public String getJdbcUrl() {
        return getJdbcUrl(DEFAULT_DATABASE_NAME);
    }

    public String getJdbcUrl(String databaseName) {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return "jdbc:oceanbase://"
                + getHost()
                + ":"
                + getSqlPort()
                + "/"
                + databaseName
                + additionalUrlParams;
    }

    public OceanBaseContainer withTenant(String tenantName) {
        this.tenantName = tenantName;
        return this;
    }

    public String getTenantName() {
        return tenantName;
    }

    @Override
    public String getDatabaseName() {
        return DEFAULT_DATABASE_NAME;
    }

    @Override
    public String getUsername() {
        return DEFAULT_USERNAME + "@" + tenantName;
    }

    @Override
    public String getPassword() {
        return DEFAULT_PASSWORD;
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    @Override
    protected void configure() {
        withEnv("MODE", "slim");
        withEnv("OB_TENANT_NAME", tenantName);
    }
}
