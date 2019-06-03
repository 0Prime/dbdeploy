package com.dbdeploy.scripts

import spock.lang.Specification

class ChangeScriptCreatorSpec extends Specification {

	ChangeScriptCreator changeScriptCreator
	File scriptDirectory


	def 'default change-script creator creates script with timestamp'() {
		when:
			final newChangeScript = changeScriptCreator.createScript()

		then: 'change-script exists'
			newChangeScript.exists()

		and: 'file name have sql suffix'
			final fileName = newChangeScript.name
			fileName.endsWith '.sql'

		and: 'file name timestamped correctly'
			fileName.matches '[0-9]{14}\\.sql'
	}


	def 'creates script with timestamp and description'() {
		given:
			final scriptDescription = 'test_1234'

		when:
			changeScriptCreator.scriptDescription = scriptDescription
			final newChangeScript = changeScriptCreator.createScript()

		then: 'file name timestamped and named correctly'
			final fileName = newChangeScript.name
			fileName.matches "[0-9]{14}_$scriptDescription\\.sql"
	}


	/* LIFECYCLE */

	def setup() {
		scriptDirectory = new File(System.getProperty('java.io.tmpdir'))
		changeScriptCreator = new ChangeScriptCreator()
		changeScriptCreator.scriptDirectory = scriptDirectory
	}
}
