package com.dbdeploy.scripts

import com.dbdeploy.exceptions.UnrecognisedFilenameException
import spock.lang.*

class FilenameParserSpec extends Specification {

	@Unroll
	def 'can parse any file name that starts with a number: "#fileName" is "#expected"'() {
		given:
			final parser = new FilenameParser()

		when:
			final id = parser.extractIdFromFilename(fileName)

		then:
			id == expected

		where:
			fileName              || expected
			'0001_a_filename.txt' || 1L
			'1_a_filename.txt'    || 1L
			'1 a filename.txt'    || 1L
			'1.txt'               || 1L
			'00123_something.txt' || 123L
	}


	def 'throws when file name does not start with a number'() {
		given:
			final parser = new FilenameParser()

		when:
			parser.extractIdFromFilename fileName

		then:
			final ex = thrown UnrecognisedFilenameException
			ex.message.contains fileName

		where:
			fileName = "blah blah blah"
	}
}
