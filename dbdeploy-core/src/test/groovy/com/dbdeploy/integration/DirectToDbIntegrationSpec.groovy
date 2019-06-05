package com.dbdeploy.integration

import com.dbdeploy.DbDeploy
import com.dbdeploy.tools.Database
import spock.lang.Specification

class DirectToDbIntegrationSpec extends Specification {

	def 'should successfully apply a valid set of deltas'() {
		given: 'DB'
			final db = new Database('todb_success_test')
			db.createSchemaVersionTable()

		and: 'dbdeploy'
			final dbDeploy = new DbDeploy()
			db.applyDatabaseSettingsTo(dbDeploy)
			dbDeploy.scriptdirectory = findScriptDirectory('src/it/db/deltas')

		when:
			dbDeploy.go()

		then:
			db.changelogEntries == [1L, 2L]

		and:
			final results = db.executeQuery 'select id from Test'
			results.size() == 1
			results.head().head() == 6
	}


	def 'should successfully apply a valid set of deltas including multi-statement deltas'() {
		given: 'DB'
			final db = new Database('todb_multistatement_test')
			db.createSchemaVersionTable()

		and: 'dbdeploy'
			final dbDeploy = new DbDeploy(findScriptDirectory('src/it/db/multi_statement_deltas'))
			db.applyDatabaseSettingsTo(dbDeploy)

		when:
			dbDeploy.go()

		then:
			db.changelogEntries == [1L, 2L]

		and:
			final results = db.executeQuery 'select id from Test'
			results == expected

		where:
			expected = [[6], [7]]*.asType(Object[])
	}


	def 'should be able to recover from bad scripts just by running corrected scripts again'() {
		given:
			final db = new Database('todb_failure_recovery_test')
			db.createSchemaVersionTable()

		and:
			final dbDeploy = makeDbDeploy(db, badScripts)

		when:
			dbDeploy.go()

		then: 'script 2 failed'
			final ex = thrown Exception
			ex.message.contains 'Column count does not match in statement'

		and: 'script 2 failed, so it should not be considered applied to the database'
			db.changelogEntries.contains(1L)
			!db.changelogEntries.contains(2L)

		and:
			db.executeQuery('select id from Test').empty

		and: 'now rerun dbdeploy with valid scripts, should recover'
			final dbDeploy2 = makeDbDeploy(db, goodScripts)
			dbDeploy2.go()

			db.changelogEntries == [1L, 2L]

			db.executeQuery('select id from Test').size() == 1

		where:
			badScripts = 'src/it/db/invalid_deltas'
			goodScripts = 'src/it/db/deltas'

			makeDbDeploy = { database, scripts ->
				database.applyDatabaseSettingsTo new DbDeploy(findScriptDirectory(scripts as String))
			}
	}


	/* HELPERS */

	File findScriptDirectory(String directoryName) {
		File directoryWhenRunningUnderMaven = new File(directoryName)

		if (directoryWhenRunningUnderMaven.isDirectory())
			return directoryWhenRunningUnderMaven


		File directoryWhenRunningUnderIde = new File('dbdeploy-core', directoryName)

		if (directoryWhenRunningUnderIde.isDirectory())
			return directoryWhenRunningUnderIde

		assert false, "Could not find script directory: $directoryName"
		return null
	}
}
