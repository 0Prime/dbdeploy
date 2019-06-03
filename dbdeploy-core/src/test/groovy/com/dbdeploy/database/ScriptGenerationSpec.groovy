package com.dbdeploy.database

import com.dbdeploy.Controller
import com.dbdeploy.appliers.TemplateBasedApplier
import com.dbdeploy.database.changelog.DatabaseSchemaVersionManager
import com.dbdeploy.exceptions.SchemaVersionTrackingException
import com.dbdeploy.scripts.ChangeScriptRepository
import com.dbdeploy.tools.StubChangeScript
import spock.lang.*

class ScriptGenerationSpec extends Specification {

	@Unroll
	def 'generate consolidated changes script for all databases and compare against template, syntax: `#syntax`'() {
		when:
			printf "Testing syntax %s\n", syntax
			runIntegratedTestAndConfirmOutputResults syntax

		then:
			noExceptionThrown()

		where:
			syntax << ["hsql", "mssql", "mysql", "ora", "syb-ase", "db2", "pgsql"]
	}


	void runIntegratedTestAndConfirmOutputResults(String syntaxName) throws Exception {

		final writer = new StringWriter()

		final changeOne = new StubChangeScript(1, "001_change.sql", "-- contents of change script 1")
		final changeTwo = new StubChangeScript(2, "002_change.sql", "-- contents of change script 2")

		final changeScripts = [changeOne, changeTwo]
		final changeScriptRepository = new ChangeScriptRepository(changeScripts)


		final schemaManager = new StubSchemaManager()
		final applier = new TemplateBasedApplier(writer, syntaxName, "changelog", ";", DelimiterType.normal, null)
		final controller = new Controller(changeScriptRepository, schemaManager, applier, null)

		controller.processChangeScripts(Long.MAX_VALUE)

		final expected = readExpectedFileContents(getExpectedFilename(syntaxName))
		final actual = writer.toString()


		assert normalizeLineSeparators(expected) == normalizeLineSeparators(actual)
	}


	String getExpectedFilename(String dbSyntaxName) {
		dbSyntaxName + "_expected.sql"
	}


	String readExpectedFileContents(String expectedFilename) throws IOException {
		final stream = getClass().getResourceAsStream expectedFilename

		new BufferedReader(new InputStreamReader(stream)).withCloseable {
			readEntireStreamIntoAStringWithConversionToSystemDependantLineTerminators it
		}
	}


	String readEntireStreamIntoAStringWithConversionToSystemDependantLineTerminators(BufferedReader reader) throws IOException {
		final contentWithSystemDependentLineTerminators = new StringWriter()

		new PrintWriter(contentWithSystemDependentLineTerminators).withCloseable {
			String line

			while ((line = reader.readLine()) != null)
				it.println(line)

			it.flush()

			return contentWithSystemDependentLineTerminators.toString()
		}
	}


	String normalizeLineSeparators(String str) {
		str.replaceAll("\\n|\\r\\n", System.lineSeparator())
	}


	class StubSchemaManager extends DatabaseSchemaVersionManager {
		StubSchemaManager() { super(null, "changelog") }

		@Override List<Long> getAppliedChanges() throws SchemaVersionTrackingException { [] }
	}
}
