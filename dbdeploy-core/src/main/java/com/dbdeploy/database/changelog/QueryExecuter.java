package com.dbdeploy.database.changelog;

import java.sql.*;

public class QueryExecuter {
	private final Connection connection;
	private final String username;

	public QueryExecuter(String connectionString, String username, String password) throws SQLException {
		this.username = username;
		connection = DriverManager.getConnection(connectionString, username, password);
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		//N.B. closing statement there makes ResultSet inaccessible, which can lead to exceptions or nulls
		//ResultSet _should_ be closed manually in calling code
		Statement statement = connection.createStatement();
		return statement.executeQuery(sql);
	}


	public void execute(String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	public void execute(String sql, Object... params) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				Object param = params[i];
				statement.setObject(i + 1, param);
			}
			statement.execute();
		}
	}

	public void close() throws SQLException {
		connection.close();
	}

	public void setAutoCommit(boolean autoCommitMode) throws SQLException {
		connection.setAutoCommit(autoCommitMode);
	}

	public void commit() throws SQLException {
		connection.commit();
	}

	public String getDatabaseUsername() {
		return username;
	}
}
