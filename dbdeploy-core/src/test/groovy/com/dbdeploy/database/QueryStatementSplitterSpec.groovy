package com.dbdeploy.database

import org.apache.commons.lang.SystemUtils
import spock.lang.*

class QueryStatementSplitterSpec extends Specification {

	QueryStatementSplitter splitter


	def 'should not split statements that have no delimiter'() {
		when:
			final result = splitter.split 'SELECT 1'

		then:
			result == ['SELECT 1']
	}


	def 'should ignore semicolons in the middle of a line'() {
		when:
			final result = splitter.split "SELECT ';'"

		then:
			result == ["SELECT ';'"]
	}


	def 'should split statements on a semicolon at the end of a line'() {
		when:
			final result = splitter.split 'SELECT 1;\nSELECT 2;'

		then:
			result == ['SELECT 1', 'SELECT 2']
	}


	def 'should split statements on a semicolon at the end of a line even with windows line endings'() {
		when:
			final result = splitter.split 'SELECT 1;\r\nSELECT 2;'

		then:
			result == ['SELECT 1', 'SELECT 2']
	}


	def 'should split statements on a semicolon at the end of a line ignoring whitespace'() {
		when:
			final result = splitter.split 'SELECT 1;  \nSELECT 2;  '

		then:
			result == ['SELECT 1', 'SELECT 2']
	}


	def 'should leave line breaks alone'() {
		expect:
			splitter.split('SELECT\n1') == ['SELECT' + SystemUtils.LINE_SEPARATOR + '1']
			splitter.split('SELECT\r\n1') == ['SELECT' + SystemUtils.LINE_SEPARATOR + '1']
	}


	def 'should support row style terminators'() {
		given:
			splitter.delimiter = '/'
			splitter.delimiterType = DelimiterType.row

		when:
			final result = splitter.split('SHOULD IGNORE /\nAT THE END OF A LINE\n/\nSELECT BLAH FROM DUAL')

		then:
			result == ['SHOULD IGNORE /' + SystemUtils.LINE_SEPARATOR + 'AT THE END OF A LINE' + SystemUtils.LINE_SEPARATOR,
			           'SELECT BLAH FROM DUAL']
	}


	@Unroll
	def 'should support `#lineEnding` line ending'() {
		given:
			splitter.outputLineEnding = lineEnding

		expect:
			splitter.split('SELECT\n1') == expected
			splitter.split('SELECT\r\n1') == expected

		where:
			lineEnding          || expected
			LineEnding.crlf     || ['SELECT\r\n1']
			LineEnding.cr       || ['SELECT\r1']
			LineEnding.lf       || ['SELECT\n1']
			LineEnding.platform || ['SELECT' + SystemUtils.LINE_SEPARATOR + '1']
	}


	/* LIFECYCLE */

	def setup() {
		splitter = new QueryStatementSplitter()
	}
}
