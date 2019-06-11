package com.dbdeploy.integration


import groovy.sql.Sql
import org.apache.commons.io.FileUtils
import spock.lang.*

class JarIntegrationTest extends Specification {

	def 'patches is folder of sql scripts -> OK'() {
		given: 'DB with a changelog table'
			final sql = Sql.newInstance url, username, password
			createChangeLog sql, changeLogTableName
			assert sql.dataSet(changeLogTableName).rows().empty

		and: 'dbdeploy, driver, patches and output files'
			final dbdeployCli = copyToTemp findJar('dbdeploy', 'fatJar')
			final driver = copyToTemp findJar('sqlite', 'copyDrivers')
			final patches = copyToTemp findResource('/patches/linear')

		and: 'cli params'
			final driverPath = driver.canonicalPath
			final dbdeployPath = dbdeployCli.canonicalPath
			final dbdeployParams = [
					url            : url,
					userid         : username,
					password       : password,
					driver         : driverClassName,
					//changeLogTableName: changeLogTableName, // should be auto-inferred to 'changelog' by default
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

				//assert changelog table
			}

		where:
			dbName = UUID.randomUUID()
			driverClassName = 'org.sqlite.JDBC'
			url = "jdbc:sqlite:memory:$dbName"
			username = 'user_whatever'
			password = 'password_whatever'
			changeLogTableName = 'changelog'
			dbms = 'mysql'
	}


	@Ignore('TBD')
	def 'patches is folder of sub-folders, where each sub-folder contains scripts'() {
		expect:
			false
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
			assert false, "too many files, can't decide which is target jar: ${files*.name.join '; '}"

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
