package com.dbdeploy

import com.dbdeploy.scripts.ChangeScript
import spock.lang.*

class PrettyPrinterSpec extends Specification {

	PrettyPrinter prettyPrinter = new PrettyPrinter()


	@Unroll
	def 'prettyPrinter.format #comment'() {
		expect:
			prettyPrinter.format(changes*.longValue() as List<Long>) == expected

		where:
			changes                    || expected             | comment
			[1, 3, 5]                  || '1, 3, 5'            | 'should display non ranged numbers as separate entities'
			[1, 2, 3, 4, 5]            || '1..5'               | 'should display a range as such'
			[1, 2]                     || '1, 2'               | 'ranges of two are not displayed as a range'
			[]                         || '(none)'             | 'should return none with an empty list'
			[1, 2, 4, 7, 8, 9, 10, 12] || '1, 2, 4, 7..10, 12' | 'can deal with mixture of ranges and non ranges'
	}


	def 'can format a change script list'() {
		given:
			ChangeScript change1 = new ChangeScript(1)
			ChangeScript change3 = new ChangeScript(3)

		expect:
			prettyPrinter.formatChangeScriptList([change1, change3]) == '1, 3'
	}
}
