package com.dbdeploy

import com.dbdeploy.database.*
import spock.lang.*

class DbDeployCommandLineParserSpec extends Specification {

	DbDeploy dbDeploy
	UserInputReader userInputReader
	DbDeployCommandLineParser parser


	def 'can parse user id from command line'() {
		when:
			parser.parse '-U myuserid'.split(' '), dbDeploy

		then:
			dbDeploy.userid == 'myuserid'
	}


	def 'this is not really a test because there is no assert, but it`s very useful to look at the result'() {
		when:
			parser.printUsage()

		then:
			true
	}


	def 'check all of the other fields parse ok here'() {
		when:
			parser.parse(('-U userid ' +
					'-P password ' +
					'--driver a.b.c ' +
					'--url b:c:d ' +
					'--scriptdirectory . -o output.sql ' +
					'--changeLogTableName my-change-log ' +
					'--dbms ora ' +
					'--templatedir /tmp/mytemplates ' +
					'--delimiter \\ --delimitertype row').split(' '), dbDeploy)

		then:
			verifyAll dbDeploy, {
				userid == 'userid'
				password == 'password'
				driver == 'a.b.c'
				url == 'b:c:d'
				scriptdirectory.name == '.'
				outputfile.name == 'output.sql'
				dbms == 'ora'
				changeLogTableName == 'my-change-log'
				delimiter == '\\'
				delimiterType == DelimiterType.row
				templatedir.path == File.separator + 'tmp' + File.separator + 'mytemplates'
			}
	}


	@Unroll
	def 'delimiter type works ok'() {
		when:
			parser.parse "--delimitertype $delimiter".split(' '), dbDeploy

		then:
			dbDeploy.delimiterType == expected

		where:
			delimiter || expected
			'normal'  || DelimiterType.normal
			'row'     || DelimiterType.row
	}


	@Unroll
	def 'line ending command: `#command` parses into `#expected`'() {
		when:
			if (command)
				parser.parse command.split(' '), dbDeploy

		then:
			dbDeploy.lineEnding == expected

		where:
			command                 || expected
			null                    || LineEnding.platform
			'--lineending cr'       || LineEnding.cr
			'--lineending crlf'     || LineEnding.crlf
			'--lineending lf'       || LineEnding.lf
			'--lineending platform' || LineEnding.platform
	}


	def 'should prompt from stdin for password if password param supplied with no arg'() {
		when:
			parser.parse(['-P'] as String[], dbDeploy)

		then:
			1 * userInputReader.read('Password') >> expected
			dbDeploy.password == expected

		where:
			expected = 'user entered password'
	}


	def 'should not prompt for password when supplied'() {
		when:
			parser.parse(['-P', 'password'] as String[], dbDeploy)

		then:
			0 * userInputReader._
	}


	def 'should not prompt for password not specified on command line'() {
		when: 'this is important: not all databases require passwords :)'
			parser.parse([] as String[], dbDeploy)

		then:
			0 * userInputReader._
	}


	/* LIFECYCLE */

	def setup() {
		dbDeploy = new DbDeploy()
		userInputReader = Mock(UserInputReader)
		parser = new DbDeployCommandLineParser(userInputReader)
	}
}
