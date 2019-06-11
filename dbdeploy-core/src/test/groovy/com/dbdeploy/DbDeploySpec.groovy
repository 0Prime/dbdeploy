package com.dbdeploy

import com.dbdeploy.exceptions.UsageException
import spock.lang.Specification

class DbDeploySpec extends Specification {

	DbDeploy dbDeploy


	def 'should throw if invalid driver class name specified'() {
		when:
			dbDeploy.driver = 'some.class.that.will.not.be.Found'
			dbDeploy.go()

		then:
			thrown ClassNotFoundException
	}


	def 'should throw if user id not specified'() {
		when:
			dbDeploy.userid = null
			dbDeploy.go()

		then:
			thrown UsageException
	}


	def 'should throw if driver not specified'() {
		when:
			dbDeploy.driver = null
			dbDeploy.go()

		then:
			thrown UsageException
	}


	def 'should throw if url not specified'() {
		when:
			dbDeploy.url = null
			dbDeploy.go()

		then:
			thrown UsageException
	}


	def 'should throw if script directory is not a valid directory'() {
		given:
			dbDeploy.scriptdirectory = new File("fileThatDoesntExist.txt")

		expect:
			try {
				dbDeploy.go()
				assert false, 'exception expected'
			}
			catch (UsageException e) {
				assert e.message == 'Script directory must point to a valid directory'
			}
	}


	def 'should report version number without crashing'() {
		expect:
			dbDeploy.welcomeString.startsWith 'dbdeploy'
	}


	/* LIFECYCLE */

	def setup() {
		// set sensible default values for all parameters
		dbDeploy = new DbDeploy(Mock(File))
		dbDeploy.driver = getClass().getName()
		dbDeploy.userid = "someUser"
		dbDeploy.dbms = "hsql"
		dbDeploy.url = "jdbc:hsqldb:mem:dbdeploy"
		dbDeploy.scriptdirectory = new File(".")
		dbDeploy.outputfile = new File("a.txt")
	}
}
