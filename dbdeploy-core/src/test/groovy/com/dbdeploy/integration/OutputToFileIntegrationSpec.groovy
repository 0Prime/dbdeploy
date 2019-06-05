package com.dbdeploy.integration

import com.dbdeploy.DbDeploy
import com.dbdeploy.tools.Database
import spock.lang.Specification

import java.sql.SQLException

class OutputToFileIntegrationSpec extends Specification {

	def 'should successfully apply a valid set of deltas'() {
		given: 'DB'
			final db = new Database('success_test')
			db.createSchemaVersionTable()

		and: 'output file'
			final outputFile = File.createTempFile('success', '.sql')

		and: 'dbdeploy'
			final dbDeploy = db.applyDatabaseSettingsTo new DbDeploy()
			dbDeploy.scriptdirectory = findScriptDirectory 'src/it/db/deltas'
			dbDeploy.outputfile = outputFile

		when:
			dbDeploy.go()
			db.applyScript outputFile

		then:
			db.getChangelogEntries() == [1L, 2L]

		and:
			final results = db.executeQuery 'select id from Test'
			results.size() == 1
			results.head().head() == 6
	}


	def 'should be able to recover from bad scripts just by running corrected scripts again'() {
		given:
			final db = new Database('failure_recovery_test')
			db.createSchemaVersionTable()

		and:
			final outputFile = File.createTempFile 'recovery', '.sql'

		and:
			final dbDeploy = makeDbDeploy(db, badScripts, outputFile)

		when:
			dbDeploy.go()
			db.applyScript outputFile

		then: 'script 2 failed'
			final ex = thrown SQLException
			ex.message.startsWith 'Column count does not match in statement'


		and: 'script 2 failed, so it should not be considered applied to the database'
			db.changelogEntries.contains(1L)
			!db.changelogEntries.contains(2L)

			db.executeQuery('select id from Test').empty

		and: 'now rerun dbdeploy with valid scripts, should recover'
			final dbDeploy2 = makeDbDeploy(db, goodScripts, outputFile)
			dbDeploy2.go()

			db.applyScript outputFile

			db.changelogEntries == [1L, 2L]
			db.executeQuery('select id from Test').size() == 1

		where:
			badScripts = 'src/it/db/invalid_deltas'
			goodScripts = 'src/it/db/deltas'

			makeDbDeploy = { database, scripts, output ->
				final ret = database.applyDatabaseSettingsTo new DbDeploy(findScriptDirectory(scripts as String))
				ret.outputfile = output

				ret
			}
	}


	def 'should use specified changelog table'() {
		given:
			final db = new Database('user_defined_changelog_test', 'user_defined_changelog_table')
			db.createSchemaVersionTable()

		and:
			final outputFile = File.createTempFile 'changelog_success', '.sql'

		and:
			final dbDeploy = db.applyDatabaseSettingsTo new DbDeploy()
			dbDeploy.scriptdirectory = findScriptDirectory 'src/it/db/deltas'
			dbDeploy.outputfile = outputFile
			dbDeploy.changeLogTableName = 'user_defined_changelog_table'

		when:
			dbDeploy.go()
			db.applyScript outputFile

		then:
			db.changelogEntries == [1L, 2L]
	}


	def 'should not add comma separators to high numbered scripts'() {
		given:
			final db = new Database('high_number_test')
			db.createSchemaVersionTable()

		and:
			final outputFile = File.createTempFile 'high_number_test', '.sql'

		and:
			final dbDeploy = db.applyDatabaseSettingsTo new DbDeploy()
			dbDeploy.scriptdirectory = findScriptDirectory 'src/it/db/high_numbers'
			dbDeploy.outputfile = outputFile

		when:
			dbDeploy.go()
			db.applyScript(outputFile)

		then: 'em... we probably should check filenames here...'
			true //fixme it's strange that there's no assertion
	}


	/* HELPERS */

	File findScriptDirectory(String directoryName) {
		final directoryWhenRunningUnderMaven = new File(directoryName)

		if (directoryWhenRunningUnderMaven.isDirectory())
			return directoryWhenRunningUnderMaven

		final directoryWhenRunningUnderIde = new File('dbdeploy-core', directoryName)

		if (directoryWhenRunningUnderIde.isDirectory())
			return directoryWhenRunningUnderIde

		assert false, "Could not find script directory: $directoryName"
		return null
	}
}
