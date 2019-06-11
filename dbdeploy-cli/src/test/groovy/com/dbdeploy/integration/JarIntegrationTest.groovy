package com.dbdeploy.integration

import groovy.sql.Sql
import org.apache.commons.io.FileUtils
import spock.lang.Specification

class JarIntegrationTest extends Specification {

	def 'patches is folder of sql scripts -> OK'() {
		given: 'dbdeploy, driver and patches files'
			final dbdeployCli = copyToTemp findJar('dbdeploy-cli-all', 'fatJar')
			final driver = copyToTemp findJar('sqlite', 'copyDrivers')
			final patches = copyToTemp findResource('/patches/linear')

		and: 'DB with a changelog table'
			def url = "jdbc:sqlite:${tempDir.canonicalPath}/$dbName"
			def sql = Sql.newInstance url, username, password
			createChangeLog sql, changeLogTableName
			assert sql.dataSet(changeLogTableName).rows().empty

		and: 'cli params'
			final driverPath = driver.canonicalPath
			final dbdeployPath = dbdeployCli.canonicalPath
			final dbdeployParams = [
					url            : url,
					userid         : username,
					password       : password,
					driver         : driverClassName,
					//changeLogTableName: changeLogTableName, //should be auto-inferred to 'changelog' by default
					dbms           : dbms,
					scriptdirectory: patches.canonicalPath,
			].inject '', { acc, param, value -> "$acc --$param $value" }

		when: 'invoking cli with params'
			runCmd($/java -cp $driverPath;$dbdeployPath com.dbdeploy.CommandLineTarget $dbdeployParams/$)

		then: 'DB state is correct'
			verifyAll sql, {
				dataSet 'foo' rows() empty

				final bar = dataSet 'bar' rows()
				bar.size() == 1
				bar.head() == [bar_int: 42, bar_string: 'banana']

				dataSet(changeLogTableName).rows().size() == 3
			}

		cleanup:
			sql.close()

		where:
			dbName = UUID.randomUUID()
			driverClassName = 'org.sqlite.JDBC'
			username = 'user_whatever'
			password = 'password_whatever'
			changeLogTableName = 'changelog'
			dbms = 'mysql'
	}


	def 'patches is folder of sub-folders, where each sub-folder contains scripts -> OK'() {
		given: 'dbdeploy, driver, and patches files'
			final dbdeployCli = copyToTemp findJar('dbdeploy-cli-all', 'fatJar')
			final driver = copyToTemp findJar('sqlite', 'copyDrivers')
			final patches = copyToTemp findResource('/patches/tree')

		and: 'DB with a changelog tables'
			def url = "jdbc:sqlite:${tempDir.canonicalPath}/$dbName"
			def sql = Sql.newInstance url, username, password
			patches.listFiles().collect { createChangeLog sql, it.name }

		and: 'cli params'
			final driverPath = driver.canonicalPath
			final dbdeployPath = dbdeployCli.canonicalPath
			final dbdeployParams = [
					url            : url,
					userid         : username,
					password       : password,
					driver         : driverClassName,
					//changeLogTableName: changeLogTableName, //should be auto-inferred to respective patches dir name
					dbms           : dbms,
					scriptdirectory: patches.canonicalPath,
			].inject '', { acc, param, value -> "$acc --$param $value" }

		when: 'invoking cli with params'
			runCmd($/java -cp $driverPath;$dbdeployPath com.dbdeploy.CommandLineTarget $dbdeployParams/$)

		then: 'DB state is correct'
			verifyAll sql, {
				dataSet('alpha').rows().size() == 3

				dataSet('beta').rows().size() == 2

				dataSet 'foo' rows() empty
				dataSet('bar').rows().size() == 1
				dataSet 'baz' rows() empty
			}

		cleanup:
			sql.close()

		where:
			dbName = UUID.randomUUID()
			driverClassName = 'org.sqlite.JDBC'
			username = 'user_whatever'
			password = 'password_whatever'
			changeLogTableName = 'changelog'
			dbms = 'mysql'
	}


	/* HELPERS */

	void createChangeLog(Sql sql, String changeLogTableName) {
		//noinspection SqlNoDataSourceInspection
		sql.execute """
		CREATE TABLE $changeLogTableName (
      change_number	NUMERIC	NOT NULL, 
      complete_dt		NUMERIC	NOT NULL, 
      applied_by		TEXT		NOT NULL, 
      description		TEXT		NOT NULL 
		);
    """.toString()
	}


	File findJar(String namePart, String jarGeneratingTask) {
		final libs = new File('./build/libs')
		final files = libs.listFiles({ _, name -> name.contains namePart } as FilenameFilter) as List

		if (files.size() > 1)
			assert false, "too many files, can't decide which is '$namePart' enough: ${files*.name.join '; '}"

		if (files.size() == 1)
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
			println "Out:$outputStream, Err: $errStream"
			assert false, 'failed to run command'
		}
	}


	/* LIFECYCLE */

	def setup() {
		tempDir = File.createTempDir()
		tempDir.deleteOnExit()
	}


	def cleanup() {
		tempDir.deleteDir()
	}


	File tempDir
}
