package org.openforis.collect.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.ConnectionProvider;
import org.jooq.impl.DialectAwareJooqConfiguration;
import org.openforis.collect.persistence.jooq.CollectDSLContext;

public class DbInitializer {
	
	private static final Log LOG = LogFactory.getLog(DbInitializer.class);
	
	private ConnectionProvider connectionProvider;

	public DbInitializer(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}
	
	public void start() {
		DialectAwareJooqConfiguration jooqConf = new DialectAwareJooqConfiguration(connectionProvider);
		CollectDSLContext dslContext = new CollectDSLContext(jooqConf);
		if (! dslContext.isSchemaLess()) {
			createDbSchema();
		}
	}
	
	private void createDbSchema() {
		Connection conn = null;
		try {
			LOG.info("Acquiring connection...");
			
			conn = connectionProvider.acquire();
			conn.setAutoCommit(false);
			
			LOG.info("Connection acquired!");
			LOG.info(String.format("Creating schema %s if not exists...", DbUtils.SCHEMA_NAME));

			if (createSchemaIfNotExists(conn)) {
				LOG.info(String.format("Schema '%s' created (if not existing already)", DbUtils.SCHEMA_NAME));
			} else {
				LOG.info("Try to create schema withoud 'NOT EXISTS' clause");
				boolean schemaCreated = createSchema(conn);
				if (schemaCreated) {
					LOG.info(String.format("Schema '%s' created", DbUtils.SCHEMA_NAME));
				} else {
					LOG.info(String.format("Schema '%s' already exists", DbUtils.SCHEMA_NAME));
				}
			}
		} catch (Exception e) {
		} finally {
			if (conn != null) {
				connectionProvider.release(conn);
			}
		}
	}

	/**
	 * Creates the schema using a CREATE SCHEMA IF NOT EXISTS statement.
	 * 
	 * @return 'true' if the statement is executed correctly, false if the syntax is not supported
	 */
	private boolean createSchemaIfNotExists(Connection conn) {
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", DbUtils.SCHEMA_NAME));
			return true;
		} catch(Exception e) {
			LOG.info("CREATE SCHEMA IF NOT EXISTS is not supported by this DBMS");
			return false;
		} finally {
			//commit transaction anyway
			commitQuietly(conn);
		}
	}
	
	/**
	 * Creates a new schema using a CREATE SCHEMA statement.
	 * 
	 * @return 'true' if the schema is created, false if it already exists
	 */
	private boolean createSchema(Connection conn) {
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(String.format("CREATE SCHEMA %s", DbUtils.SCHEMA_NAME));
			return true;
		} catch(Exception e) {
			boolean schemaAlreadyExists = e.getMessage().toLowerCase(Locale.ENGLISH).contains("already exists");
			if (schemaAlreadyExists) {
				return false;
			} else {
				String errorMessage = String.format("Error creating schema '%s'", DbUtils.SCHEMA_NAME);
				LOG.error(errorMessage, e);
				throw new RuntimeException(errorMessage, e);
			}
		} finally {
			//commit transaction anyway
			commitQuietly(conn);
		}
	}

	private void commitQuietly(Connection conn) {
		try {
			conn.commit();
		} catch (SQLException e) {}
	}
	
}