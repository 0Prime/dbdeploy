package com.dbdeploy.scripts

import spock.lang.Specification

class ChangeScriptSpec extends Specification {

	def 'change scripts have an id and a file and encoding'() {
		given:
			final file = new File('abc.txt')

		when:
			final changeScript = new ChangeScript(5, file, 'UTF-8')

		then:
			changeScript.id == 5L
			changeScript.file == file
	}


	def 'should return contents of file'() {
		given:
			final file = createTemporaryFileWithContent content

		when:
			final changeScript = new ChangeScript(5, file, 'UTF-8')

		then:
			changeScript.content == content

		where:
			content = 'Hello\nThere!\n'
	}


	def 'contents of file should exclude anything after an undo marker'() {

		given:
			final file = createTemporaryFileWithContent content

		when:
			final changeScript = new ChangeScript(5, file, 'UTF-8')

		then:
			changeScript.content == 'Hello\nThere!\n'

		where:
			content = 'Hello\n' +
					'There!\n' +
					'--//@UNDO\n' +
					'This is after the undo marker!\n'
	}


	def 'contents of file should exclude anything after an undo marker even when that marker has some whitespace at the end'() {
		given:
			final file = createTemporaryFileWithContent(content)

		when:
			final changeScript = new ChangeScript(5, file, 'UTF-8')

		then:
			changeScript.content == 'Hello\nThere!\n'

		where:
			content = 'Hello\n' +
					'There!\n' +
					'--//@UNDO   \n' +
					'This is after the undo marker!\n'
	}


	def 'should return undo contents of file'() {
		given:
			final file = createTemporaryFileWithContent(content)

		when:
			final changeScript = new ChangeScript(5, file, 'UTF-8')

		then:
			changeScript.undoContent == 'This is after the undo marker!\n'

		where:
			content = 'Hello\n' +
					'There!\n' +
					'--//@UNDO\n' +
					'This is after the undo marker!\n'
	}


	def 'change-scripts naturally order by id'() {
		when:
			final one = new ChangeScript(1)
			final two = new ChangeScript(2)

		then:
			one < two
			two >= one
	}


	def 'to string returns a sensible value'() {
		given:
			final file = new File('abc.txt')

		and:
			final s1 = new ChangeScript(5, file, 'UTF-8')
			final s2 = new ChangeScript(5, 'abc.txt')

		expect:
			s1.toString() == '#5: abc.txt'
			s2.toString() == '#5: abc.txt'
	}


	/* HELPERS */

	File createTemporaryFileWithContent(String content) throws IOException {
		final file = File.createTempFile('changeScriptTest', '.sql')
		file.deleteOnExit()

		final out = new BufferedWriter(new FileWriter(file))
		out.write(content)
		out.close()

		return file
	}
}
