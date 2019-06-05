package com.dbdeploy

import com.dbdeploy.database.*
import com.dbdeploy.exceptions.UsageException
import spock.lang.*

import static com.dbdeploy.StrategySelector.Strategy

class DbDeployCommandLineParserSpec extends Specification {

	UserInputReader userInputReader
	DbDeployCommandLineParser parser


	@Unroll
	def 'parser.makeDbDeploy(#strategy) is #expectedSuccess'() {
		when:
			IDbDeploy dbDeploy = parser.makeDbDeploy strategy, Mock(File, { listFiles() >> [] })

		then:
			dbDeploy in expected

		where:
			strategy        || expected
			Strategy.LINEAR || DbDeploy
			Strategy.TREE   || DbDeployComposite

			expectedSuccess = expected.simpleName
	}


	@Unroll
	def 'parser.makeDbDeploy(#strategy) is #expectedError'() {
		when:
			parser.makeDbDeploy strategy, new File("")

		then:
			thrown expected

		where:
			strategy               || expected
			Strategy.NOT_EXISTS    || UsageException
			Strategy.NOT_DIRECTORY || UsageException
			Strategy.EMPTY         || UsageException
			Strategy.MIXED         || UsageException
			Strategy.INVALID_TREE  || UsageException

			expectedError = expected.simpleName
	}


	def 'can parse user id from command line'() {
		when:
			final dbdeploy = parser.parse('-U myuserid'.split(' ')) as DbDeploy

		then:
			dbdeploy.userid == 'myuserid'
	}


	def 'this is not really a test because there is no assert, but it`s very useful to look at the result'() {
		when:
			parser.printUsage()

		then: true
	}


	def 'check all of the other fields parse ok here'() {
		when:
			final dbDeploy = parser.parse(('-U userid ' +
					'-P password ' +
					'--driver a.b.c ' +
					'--url b:c:d ' +
					'--scriptdirectory . -o output.sql ' +
					'--changeLogTableName my-change-log ' +
					'--dbms ora ' +
					'--templatedir /tmp/mytemplates ' +
					'--delimiter \\ --delimitertype row').split(' ')) as DbDeploy

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
			final dbDeploy = parser.parse("--delimitertype $delimiter".split(' ')) as DbDeploy

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
			final dbDeploy = parser.parse(command?.split(' ')) as DbDeploy

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
			final dbDeploy = parser.parse(['-P'] as String[]) as DbDeploy

		then:
			1 * userInputReader.read('Password') >> expected
			dbDeploy.password == expected

		where:
			expected = 'user entered password'
	}


	def 'should not prompt for password when supplied'() {
		when:
			parser.parse(['-P', 'password'] as String[])

		then:
			0 * userInputReader._
	}


	def 'should not prompt for password not specified on command line'() {
		when: 'this is important: not all databases require passwords :)'
			parser.parse([] as String[])

		then:
			0 * userInputReader._
	}


	/* LIFECYCLE */

	def setup() {
		userInputReader = Mock(UserInputReader)
		parser = new DbDeployCommandLineParser(userInputReader, { Strategy.LINEAR })
	}
}
