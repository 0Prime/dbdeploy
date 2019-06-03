package com.dbdeploy.appliers

import com.dbdeploy.database.QueryStatementSplitter
import com.dbdeploy.database.changelog.*
import com.dbdeploy.exceptions.ChangeScriptFailedException
import com.dbdeploy.scripts.ChangeScript
import com.dbdeploy.tools.StubChangeScript
import spock.lang.Specification

import java.sql.SQLException

class DirectToDbApplierSpec extends Specification {

	QueryExecuter queryExecuter
	DatabaseSchemaVersionManager schemaVersionManager
	QueryStatementSplitter splitter
	DirectToDbApplier applier


	def 'should set connection to manual commit mode at start'() {
		when:
			applier.begin()

		then:
			1 * queryExecuter.setAutoCommit(false)
			0 * _
	}


	def 'should apply change script by splitting content using the splitter'() {
		when:
			applier.applyChangeScript(new StubChangeScript(1, 'script', 'split; content'))

		then:
			1 * splitter.split('split; content') >> ['split', 'content']
			1 * queryExecuter.execute('split')
			1 * queryExecuter.execute('content')
			0 * _
	}


	def 'should rethrow sql exceptions with information about what string failed'() {
		given:
			ChangeScript script = new StubChangeScript(1, 'script', 'split; content')

		when:
			applier.applyChangeScript script

		then:
			final e = thrown ChangeScriptFailedException
			e.executedSql == 'split'
			e.script == script

		and:
			1 * splitter.split('split; content') >> ['split', 'content']
			1 * queryExecuter.execute('split') >> { throw new SQLException('dummy exception') }
			0 * queryExecuter.execute('content')
			0 * _
	}


	def 'should insert to schema version table'() {
		given:
			final changeScript = new ChangeScript(1, 'script.sql')

		when:
			applier.insertToSchemaVersionTable(changeScript)

		then:
			1 * schemaVersionManager.recordScriptApplied(changeScript)
			0 * _
	}


	def 'should commit transaction on error commit transaction'() {
		when:
			applier.commitTransaction()
		then:
			1 * queryExecuter.commit()
			0 * _
	}


	/* LIFECYCLE */

	def setup() {
		queryExecuter = Mock(QueryExecuter)
		schemaVersionManager = Mock(DatabaseSchemaVersionManager)
		splitter = Mock(QueryStatementSplitter)

		applier = new DirectToDbApplier(queryExecuter, schemaVersionManager, splitter)
	}
}
