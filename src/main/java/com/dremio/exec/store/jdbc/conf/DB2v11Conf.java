/*
 * Copyright (C) 2017-2018 Dremio Corporation
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
package com.dremio.exec.store.jdbc.conf;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.Secret;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.options.OptionManager;
import com.dremio.security.CredentialsService;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcPluginConfig;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.dremio.exec.store.jdbc.dialect.DB2Dialect;
import com.google.common.annotations.VisibleForTesting;
 import com.fasterxml.jackson.annotation.JsonIgnore;

import io.protostuff.Tag;

/**
 * Configuration for TIBCODataVirtualization sources.
 */
@SourceType(value = "IBMDB2V11ARP", label = "IBM DB2 v11", uiConfig = "IBMDB2V11ARP-layout.json", externalQuerySupported = true)
public class DB2v11Conf extends AbstractArpConf<DB2v11Conf> {
  private static final String ARP_FILENAME = "arp/implementation/db2v11-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (DB2Dialect::new));
  private static final String DRIVER = "com.ibm.db2.jcc.DB2Driver";

  @NotBlank
  @Tag(1)
  @DisplayMetadata(label = "Database Server")
  public String host;

  @NotBlank
  @Tag(2)
  @DisplayMetadata(label = "Database Port")
  public String port;

  @NotBlank
  @Tag(3)
  @DisplayMetadata(label = "Database")
  public String database;

  @NotBlank
  @Tag(4)
  @DisplayMetadata(label = "Username")
  public String username;

  @NotBlank
  @Tag(5)
  @Secret
  @DisplayMetadata(label = "Password")
  public String password;

  @Tag(6)
  @DisplayMetadata(label = "Record fetch size")
  @NotMetadataImpacting
  public int fetchSize = 500;

  @Tag(7)
  @NotMetadataImpacting
  @JsonIgnore
  @DisplayMetadata(label = "Grant External Query access (External Query allows creation of VDS from a DB2 query. Learn more here: https://docs.dremio.com/data-sources/external-queries.html#enabling-external-queries)")
  public boolean enableExternalQuery = false;

  @Tag(8)
  @DisplayMetadata(label = "Maximum idle connections")
  @NotMetadataImpacting
  public int maxIdleConns = 8;

  @Tag(9)
  @DisplayMetadata(label = "Connection idle time (s)")
  @NotMetadataImpacting
  public int idleTimeSec = 60;


  @VisibleForTesting
  public String toJdbcConnectionString() {
    final String database = checkNotNull(this.database, "Missing database.");
	final String host = checkNotNull(this.host, "Missing host.");
    final String port = checkNotNull(this.port, "Missing port.");
    final String username = checkNotNull(this.username, "Missing username.");
    final String password = checkNotNull(this.password, "Missing password.");
    return String.format("jdbc:db2://%s:%s/%s", host, port, database);
  }

  @Override
  @VisibleForTesting
  public JdbcPluginConfig buildPluginConfig(JdbcPluginConfig.Builder configBuilder, CredentialsService credentialsService, OptionManager optionManager) {
         return configBuilder.withDialect(getDialect())
        .withDatasourceFactory(this::newDataSource)
        .withShowOnlyConnDatabase(false)
        .withFetchSize(fetchSize)
        .withAllowExternalQuery(enableExternalQuery)
        .build();
  }

  private CloseableDataSource newDataSource() {
    return DataSources.newGenericConnectionPoolDataSource(
      DRIVER,
      toJdbcConnectionString(),
      username,
      password,
      null,
      DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE,
      maxIdleConns,
      idleTimeSec);
    }

  @Override
  public ArpDialect getDialect() {
    return ARP_DIALECT;
  }

  @VisibleForTesting
  public static ArpDialect getDialectSingleton() {
    return ARP_DIALECT;
  }
}
