package com.dbdeploy.tools;

import com.dbdeploy.*;
import com.dbdeploy.database.changelog.*;
import com.dbdeploy.exceptions.SchemaVersionTrackingException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class Database {
	private String connectionString;

	private Connection connection;

	private final String changeLogTableName;

	private static final String DATABASE_SYNTAX = "hsql";
	private static final String DATABASE_DRIVER = "org.hsqldb.jdbcDriver";
	private static final String DATABASE_USERNAME = "sa";
	private static final String DATABASE_PASSWORD = "";

	public Database(String databaseName) throws ClassNotFoundException, SQLException {
		this(databaseName, "changelog");
	}

	public Database(String databaseName, String changeLogTableName) throws ClassNotFoundException, SQLException {
		this.changeLogTableName = changeLogTableName;
		connectionString = "jdbc:hsqldb:mem:" + databaseName;
		connection = openConnection();
	}

	private Connection openConnection() throws ClassNotFoundException, SQLException {
		Class.forName(DATABASE_DRIVER);
		return DriverManager.getConnection(connectionString, DATABASE_USERNAME, DATABASE_PASSWORD);
	}

	public String createSchemaVersionTable() throws SQLException {
		execute("CREATE TABLE " + changeLogTableName +
				        " ( " +
				        "  change_number INTEGER NOT NULL, " +
				        "  complete_dt TIMESTAMP NOT NULL, " +
				        "  applied_by VARCHAR(100) NOT NULL, " +
				        "  description VARCHAR(500) NOT NULL " +
				        ")");

		execute("ALTER TABLE " + changeLogTableName +
				        " ADD CONSTRAINT Pkchangelog PRIMARY KEY (change_number)");

		return changeLogTableName;
	}

	private void execute(String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	public DbDeploy applyDatabaseSettingsTo(DbDeploy dbDeploy) {
		dbDeploy.setDbms(DATABASE_SYNTAX);
		dbDeploy.setDriver(DATABASE_DRIVER);
		dbDeploy.setUrl(connectionString);
		dbDeploy.setUserid(DATABASE_USERNAME);
		dbDeploy.setPassword(DATABASE_PASSWORD);

		return dbDeploy;
	}

	public void applyScript(File sqlFile) throws SQLException, IOException {
		String sql = FileUtils.readFileToString(sqlFile, StandardCharsets.UTF_8);

		for (String statement : sql.split(";"))
			execute(statement);
	}

	public List<Object[]> executeQuery(String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			final ResultSet rs = statement.executeQuery(sql);

			List<Object[]> results = new ArrayList<>();

			ResultSetMetaData meta = rs.getMetaData();
			int columnCount = meta.getColumnCount();

			while (rs.next()) {
				Object[] thisRow = new Object[columnCount];
				for (int i = 0; i < columnCount; ++i)
					thisRow[i] = rs.getObject(i + 1);

				results.add(thisRow);
			}

			return results;
		}
	}

	public List<Long> getChangelogEntries() throws SchemaVersionTrackingException, SQLException {
		final QueryExecuter queryExecuter = new QueryExecuter(connectionString, DATABASE_USERNAME, DATABASE_PASSWORD);
		AppliedChangesProvider schemaVersionManager = new DatabaseSchemaVersionManager(queryExecuter, changeLogTableName);

		return schemaVersionManager.getAppliedChanges();
	}
}
