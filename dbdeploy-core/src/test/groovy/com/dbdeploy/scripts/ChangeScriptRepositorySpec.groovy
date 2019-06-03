package com.dbdeploy.scripts

import com.dbdeploy.exceptions.DuplicateChangeScriptException
import spock.lang.Specification

class ChangeScriptRepositorySpec extends Specification {

	def 'should return an ordered list of change-scripts'() {
		given: 'changes-cripts'
			final one = new ChangeScript(1)
			final two = new ChangeScript(2)
			final three = new ChangeScript(3)
			final four = new ChangeScript(4)

		and: 'repository'
			final repository = new ChangeScriptRepository([three, two, four, one])

		when:
			final list = repository.orderedListOfDoChangeScripts

		then:
			list == [one, two, three, four]
	}


	def 'should throw when change script list contains duplicates'() {
		given: 'change-scripts with duplicates'
			final two = new ChangeScript(2)
			final three = new ChangeScript(3)
			final anotherTwo = new ChangeScript(2)

		when:
			new ChangeScriptRepository([three, two, anotherTwo])

		then:
			final ex = thrown DuplicateChangeScriptException
			ex.message == 'There is more than one change script with number 2'
	}


	def 'should allow change scripts that start from zero'() {
		given: 'scripts'
			final zero = new ChangeScript(0)
			final four = new ChangeScript(4)

		and: 'repository'
			final repository = new ChangeScriptRepository(Arrays.asList(zero, four))

		when:
			final list = repository.orderedListOfDoChangeScripts

		then:
			list == [zero, four]
	}
}
