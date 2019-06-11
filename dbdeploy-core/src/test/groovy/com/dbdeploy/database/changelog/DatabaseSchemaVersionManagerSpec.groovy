package com.dbdeploy.database.changelog

import com.dbdeploy.scripts.ChangeScript
import spock.lang.Specification

import java.sql.*
import java.util.Date

import static org.hamcrest.Matchers.startsWith

class DatabaseSchemaVersionManagerSpec extends Specification {

	ChangeScript script = new ChangeScript(99, "Some Description")

	DatabaseSchemaVersionManager schemaVersionManager

	ResultSet expectedResultSet
	QueryExecuter queryExecuter
	DatabaseSchemaVersionManager.CurrentTimeProvider timeProvider


	def 'should use query executer to read information from the changelog table'() {
		when:
			final numbers = schemaVersionManager.appliedChanges

		then:
			4 * expectedResultSet.next() >>> [true, true, true, false]
			3 * expectedResultSet.getLong('change_number') >>> [5L, 9L, 12L]
			1 * expectedResultSet.close()

		and:
			numbers == [5L, 9L, 12L]
	}


	def 'should update changelog table'() {
		when:
			schemaVersionManager.recordScriptApplied script

		then:
			1 * queryExecuter.getDatabaseUsername() >> user
			1 * timeProvider.now() >> now
			1 * queryExecuter.execute(expected, script.id, new Timestamp(now.time), user, script.description)

		where:
			user = 'BANANA'
			now = new Date()
			expected = "INSERT INTO changelog (change_number, complete_dt, applied_by, description) " +
					"VALUES (?, ?, ?, ?)"
	}


	def 'should generate sql string to delete changelog table after undo script application'() {
		when:
			final sql = schemaVersionManager.getChangelogDeleteSql(script)

		then:
			sql.equalsIgnoreCase 'DELETE FROM changelog WHERE change_number = 99'
	}


	def 'should get applied changes from specified changelog table name'() {
		given: 'schema version manager with different table name'
			final manager = new DatabaseSchemaVersionManager(queryExecuter, changelogTableName)

		when:
			manager.appliedChanges

		then:
			1 * queryExecuter.executeQuery(startsWith("SELECT change_number FROM $changelogTableName ")) >> expectedResultSet

		where:
			changelogTableName = "user_specified_changelog"
	}


	def 'should generate sql string containing specified changelog table name on delete'() {
		given: 'schema version manager with different table name'
			final manager = new DatabaseSchemaVersionManager(queryExecuter, changelogTableName)

		when:
			final updateSql = manager.getChangelogDeleteSql script

		then:
			updateSql.startsWith "DELETE FROM $changelogTableName "

		where:
			changelogTableName = "user_specified_changelog"
	}


	/* LIFECYCLE */

	def setup() {
		expectedResultSet = Mock(ResultSet)
		timeProvider = Mock(DatabaseSchemaVersionManager.CurrentTimeProvider)
		queryExecuter = Mock(QueryExecuter, {
			executeQuery(_ as String) >> expectedResultSet
		})

		schemaVersionManager = new DatabaseSchemaVersionManager(queryExecuter, "changelog")
		schemaVersionManager.timeProvider = timeProvider
	}
}
