package com.dbdeploy.database;

import com.dbdeploy.*;
import com.dbdeploy.appliers.TemplateBasedApplier;
import com.dbdeploy.database.changelog.DatabaseSchemaVersionManager;
import com.dbdeploy.exceptions.SchemaVersionTrackingException;
import com.dbdeploy.scripts.*;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class ScriptGenerationTest {

	@Test
	public void generateConsolidatedChangesScriptForAllDatabasesAndCompareAgainstTemplate() {
		for (String syntax : Arrays.asList("hsql", "mssql", "mysql", "ora", "syb-ase", "db2", "pgsql")) {
			try {
				System.out.printf("Testing syntax %s\n", syntax);
				runIntegratedTestAndConfirmOutputResults(syntax);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed while testing syntax " + syntax, e);
			}
		}
	}


	private void runIntegratedTestAndConfirmOutputResults(String syntaxName) throws Exception {

		StringWriter writer = new StringWriter();

		ChangeScript changeOne = new StubChangeScript(1, "001_change.sql", "-- contents of change script 1");
		ChangeScript changeTwo = new StubChangeScript(2, "002_change.sql", "-- contents of change script 2");

		List<ChangeScript> changeScripts = Arrays.asList(changeOne, changeTwo);
		ChangeScriptRepository changeScriptRepository = new ChangeScriptRepository(changeScripts);


		final StubSchemaManager schemaManager = new StubSchemaManager();
		ChangeScriptApplier applier = new TemplateBasedApplier(writer, syntaxName, "changelog", ";", DelimiterType.normal, null);
		Controller controller = new Controller(changeScriptRepository, schemaManager, applier, null);

		controller.processChangeScripts(Long.MAX_VALUE);

		String expected = readExpectedFileContents(getExpectedFilename(syntaxName));
		String actual = writer.toString();
		assertEquals(normalizeLineSeparators(expected), normalizeLineSeparators(actual));
	}


	private String getExpectedFilename(String dbSyntaxName) {
		return dbSyntaxName + "_expected.sql";
	}


	private String readExpectedFileContents(String expectedFilename) throws IOException {
		final InputStream stream = getClass().getResourceAsStream(expectedFilename);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
			return readEntireStreamIntoAStringWithConversionToSystemDependantLineTerminators(reader);
		}
	}


	private String readEntireStreamIntoAStringWithConversionToSystemDependantLineTerminators(BufferedReader reader) throws IOException {
		StringWriter contentWithSystemDependentLineTerminators = new StringWriter();
		try (PrintWriter newLineConvertingContentWriter = new PrintWriter(contentWithSystemDependentLineTerminators)) {
			String line;
			while ((line = reader.readLine()) != null) {
				newLineConvertingContentWriter.println(line);
			}
			newLineConvertingContentWriter.flush();
			return contentWithSystemDependentLineTerminators.toString();
		}
	}


	private String normalizeLineSeparators(String str) {
		return str.replaceAll("\\n|\\r\\n", System.lineSeparator());
	}


	private class StubSchemaManager extends DatabaseSchemaVersionManager {
		StubSchemaManager() {
			super(null, "changelog");
		}

		@Override
		public List<Long> getAppliedChanges() throws SchemaVersionTrackingException {
			return Collections.emptyList();
		}
	}
}
