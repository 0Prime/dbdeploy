package com.dbdeploy.tools;

import com.dbdeploy.DbDeploy;
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

	public void createSchemaVersionTable() throws SQLException {
		execute("CREATE TABLE " + changeLogTableName +
				        " ( " +
				        "  change_number INTEGER NOT NULL, " +
				        "  complete_dt TIMESTAMP NOT NULL, " +
				        "  applied_by VARCHAR(100) NOT NULL, " +
				        "  description VARCHAR(500) NOT NULL " +
				        ")");

		execute("ALTER TABLE " + changeLogTableName +
				        " ADD CONSTRAINT Pkchangelog PRIMARY KEY (change_number)");
	}

	private void execute(String sql) throws SQLException {
		final Statement statement = connection.createStatement();
		statement.execute(sql);
		statement.close();
	}

	public void applyDatabaseSettingsTo(DbDeploy dbDeploy) {
		dbDeploy.setDbms(DATABASE_SYNTAX);
		dbDeploy.setDriver(DATABASE_DRIVER);
		dbDeploy.setUrl(connectionString);
		dbDeploy.setUserid(DATABASE_USERNAME);
		dbDeploy.setPassword(DATABASE_PASSWORD);
	}

	public void applyScript(File sqlFile) throws SQLException, IOException {
		String sql = FileUtils.readFileToString(sqlFile, StandardCharsets.UTF_8);

		final String[] statements = sql.split(";");

		for (String statement : statements) {
			execute(statement);
		}
	}

	public List<Object[]> executeQuery(String sql) throws SQLException {
		final Statement statement = connection.createStatement();
		final ResultSet rs = statement.executeQuery(sql);


		List<Object[]> results = new ArrayList<>();

		ResultSetMetaData meta = rs.getMetaData();
		int colmax = meta.getColumnCount();

		for (; rs.next(); ) {
			Object[] thisRow = new Object[colmax];
			for (int i = 0; i < colmax; ++i) {
				thisRow[i] = rs.getObject(i + 1);
			}

			results.add(thisRow);
		}

		statement.close();

		return results;
	}

	public List<Long> getChangelogEntries() throws SchemaVersionTrackingException, SQLException {
		final QueryExecuter queryExecuter = new QueryExecuter(connectionString, DATABASE_USERNAME, DATABASE_PASSWORD);

		DatabaseSchemaVersionManager schemaVersionManager =
				new DatabaseSchemaVersionManager(queryExecuter, changeLogTableName);
		return schemaVersionManager.getAppliedChanges();
	}
}
