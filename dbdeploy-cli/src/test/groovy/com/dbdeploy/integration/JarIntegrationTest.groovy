package com.dbdeploy.integration


import groovy.sql.Sql
import org.apache.commons.io.FileUtils
import spock.lang.*

@Narrative('''
linear strategy - patches is folder of sql scripts
tree strategy - patches is folder of sub-folders, where each sub-folder contains sql scripts
''')
class JarIntegrationTest extends Specification {

	def 'empty DB, linear strategy'() {
		given: 'patches'
			final patches = copyToTemp findResource('/patches/emptyDb/linear')

		and: 'DB with a changelog table'
			def url = "jdbc:sqlite:${tempDir.canonicalPath}/$dbName"
			sql = Sql.newInstance url, USER_NAME, PASSWORD
			createChangeLog changeLogTableName
			assert sql.dataSet(changeLogTableName).rows().empty

		and: 'cli params'
			final driverPath = driver.canonicalPath
			final dbdeployPath = dbdeployCli.canonicalPath
			final dbdeployParams = toCommandLineParams([
					url            : url,
					userid         : USER_NAME,
					password       : PASSWORD,
					driver         : DRIVER_CLASS_NAME,
					//changeLogTableName: changeLogTableName, //should be auto-inferred to 'changelog' by default
					dbms           : DBMS,
					scriptdirectory: patches.canonicalPath,
			])

		when: 'invoking cli with params'
			runCmd($/java -cp $driverPath;$dbdeployPath com.dbdeploy.CommandLineTarget $dbdeployParams/$)

		then: 'DB state is correct'
			verifyLinear changeLogTableName

		where:
			dbName = UUID.randomUUID()
			changeLogTableName = 'changelog'
	}


	//depends on the correct work of previous test
	def 'existing DB, linear strategy'() {
		given: 'dbdeploy, driver and patches files'
			final patchesCreate = copyToTemp findResource('/patches/existingDb/linear/create')
			final patchesUpdate = copyToTemp findResource('/patches/existingDb/linear/update')

		and: 'DB with a changelog tables'
			def url = "jdbc:sqlite:${tempDir.canonicalPath}/$dbName"
			sql = Sql.newInstance url, USER_NAME, PASSWORD

			createChangeLog changeLogTableName
			assert sql.dataSet(changeLogTableName).rows().empty

		and: 'cli params'
			final driverPath = driver.canonicalPath
			final dbdeployPath = dbdeployCli.canonicalPath
			final sharedParams = [
					url     : url,
					userid  : USER_NAME,
					password: PASSWORD,
					driver  : DRIVER_CLASS_NAME,
					//changeLogTableName: changeLogTableName, //should be auto-inferred to 'changelog' by default
					dbms    : DBMS]

			final paramsCreate = toCommandLineParams sharedParams + [scriptdirectory: patchesCreate.canonicalPath]
			final paramsUpdate = toCommandLineParams sharedParams + [scriptdirectory: patchesUpdate.canonicalPath]

		when: 'invoking cli with params'
			runCmd($/java -cp $driverPath;$dbdeployPath com.dbdeploy.CommandLineTarget $paramsCreate/$)
			runCmd($/java -cp $driverPath;$dbdeployPath com.dbdeploy.CommandLineTarget $paramsUpdate/$)

		then: 'DB state is correct'
			verifyLinear changeLogTableName

		where:
			dbName = UUID.randomUUID()
			changeLogTableName = 'changelog'
	}


	def 'empty DB tree strategy'() {
		given: 'patches'
			final patches = copyToTemp findResource('/patches/emptyDb/tree')

		and: 'DB with a changelog tables'
			def url = "jdbc:sqlite:${tempDir.canonicalPath}/$dbName"
			sql = Sql.newInstance url, USER_NAME, PASSWORD
			patches.listFiles().collect { createChangeLog it.name }

		and: 'cli params'
			final driverPath = driver.canonicalPath
			final dbdeployPath = dbdeployCli.canonicalPath
			final dbdeployParams = toCommandLineParams([
					url            : url,
					userid         : USER_NAME,
					password       : PASSWORD,
					driver         : DRIVER_CLASS_NAME,
					//changeLogTableName: changeLogTableName, //should be auto-inferred to respective patches dir name
					dbms           : DBMS,
					scriptdirectory: patches.canonicalPath
			])

		when: 'invoking cli with params'
			runCmd($/java -cp $driverPath;$dbdeployPath com.dbdeploy.CommandLineTarget $dbdeployParams/$)

		then: 'DB state is correct'
			verifyTree()

		where:
			dbName = UUID.randomUUID()
	}


	//depends on the correct work of previous test
	def 'existing DB, tree strategy'() {
		given: 'patches'
			final patchesCreate = copyToTemp findResource('/patches/existingDb/tree/create')
			final patchesUpdate = copyToTemp findResource('/patches/existingDb/tree/update')

		and: 'DB with a changelog tables'
			def url = "jdbc:sqlite:${tempDir.canonicalPath}/$dbName"
			sql = Sql.newInstance url, USER_NAME, PASSWORD
			[patchesCreate, patchesUpdate]
					.collectMany { it.listFiles() as Collection }
					*.name
					.unique()
					.each { createChangeLog it as String }

		and: 'cli params'
			final driverPath = driver.canonicalPath
			final dbdeployPath = dbdeployCli.canonicalPath
			final sharedParams = [
					url     : url,
					userid  : USER_NAME,
					password: PASSWORD,
					driver  : DRIVER_CLASS_NAME,
					//changeLogTableName: changeLogTableName, //should be auto-inferred to 'changelog' by default
					dbms    : DBMS]

			final paramsCreate = toCommandLineParams sharedParams + [scriptdirectory: patchesCreate.canonicalPath]
			final paramsUpdate = toCommandLineParams sharedParams + [scriptdirectory: patchesUpdate.canonicalPath]

		when: 'invoking cli with params'
			runCmd($/java -cp $driverPath;$dbdeployPath com.dbdeploy.CommandLineTarget $paramsCreate/$)
			runCmd($/java -cp $driverPath;$dbdeployPath com.dbdeploy.CommandLineTarget $paramsUpdate/$)

		then: 'DB state is correct'
			verifyTree()

		where:
			dbName = UUID.randomUUID()
	}


	/* HELPERS */

	void createChangeLog(String changeLogTableName) {
		sql.execute """
		CREATE TABLE $changeLogTableName (
      change_number	NUMERIC	NOT NULL, 
      complete_dt		NUMERIC	NOT NULL, 
      applied_by		TEXT		NOT NULL, 
      description		TEXT		NOT NULL 
		);
    """.toString()
	}


	String toCommandLineParams(Map params) {
		params.inject '', { acc, param, value -> "$acc --$param $value" }
	}


	File findJar(String namePart, String jarGeneratingTask, boolean killExisting = false) {
		final libs = new File('./build/libs')
		final files = libs.listFiles({ _, name -> name.contains namePart } as FilenameFilter) as List

		if (files.size() > 1)
			assert false, "too many files, can't decide which is '$namePart' enough: ${files*.name.join '; '}"

		if (files.size() == 1)
			if (killExisting)
				files.head().delete()
			else
				return files.head()

		println "failed to find jar at: '${libs.canonicalPath}'"
		println "need to run 'gradle $jarGeneratingTask' task..."

		final osName = System.getProperty 'os.name'
		println "detected OS: $osName"

		final isWindows = osName.toLowerCase() startsWith 'win'
		final gradle = new File("../gradlew${isWindows ? '.bat' : ''}")

		runCmd "${gradle.path} $jarGeneratingTask"

		findJar namePart, jarGeneratingTask
	}


	File findResource(String path) {
		final resource = getClass() getResource path toURI()
		new File(resource)
	}


	File copyToTemp(File file) {
		final what = file.isDirectory() ? 'Directory' : 'File'
		FileUtils."copy${what}ToDirectory" file, tempDir

		new File(tempDir, file.name)
	}


	void runCmd(String cmd) {
		println 'running command:'
		println cmd

		final proc = cmd.execute()
		final outputStream = new StringBuffer()
		final errStream = new StringBuffer()

		proc.waitForProcessOutput outputStream, errStream

		if (proc.exitValue() != 0) {
			println "Out:$outputStream,\n Err: $errStream"
			assert false, 'failed to run command'
		}
	}


	void verifyLinear(String changeLogTableName) {
		verifyAll sql, {
			dataSet(changeLogTableName).rows().size() == 3

			dataSet('foo').rows().empty

			final barTable = dataSet('bar').rows()
			barTable.size() == 1
			barTable.head() == [bar_int: 42, bar_string: 'banana']

			final changelogTable = dataSet(changeLogTableName).rows() as List<Map>
			changelogTable.size() == 3
			changelogTable*.change_number == [1, 2, 3]
		}
	}


	void verifyTree() {
		verifyAll sql, {
			dataSet('alpha').rows().size() == 3

			dataSet('beta').rows().size() == 2

			dataSet('foo').rows().empty

			final barTable = dataSet('bar').rows()
			barTable.size() == 1
			barTable.head() == [bar_int: 42, bar_string: 'banana']

			dataSet 'baz' rows() empty
		}
	}


	/* LIFECYCLE */

	def setup() {
		tempDir = File.createTempDir()
		tempDir.deleteOnExit()
		dbdeployCli = copyToTemp findJar('dbdeploy-cli-all', 'fatJar', false)
		driver = copyToTemp findJar('sqlite', 'copyDrivers')
	}


	def cleanup() {
		tempDir.deleteDir()
		dbdeployCli = null
		driver = null
		sql?.close()
		sql = null
	}


	File tempDir
	File dbdeployCli
	File driver
	Sql sql

	static final DRIVER_CLASS_NAME = 'org.sqlite.JDBC'
	static final USER_NAME = 'user_whatever'
	static final PASSWORD = 'password_whatever'
	static final DBMS = 'mysql'
}
