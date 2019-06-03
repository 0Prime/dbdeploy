package com.dbdeploy.appliers

import com.dbdeploy.database.DelimiterType
import com.dbdeploy.exceptions.UsageException
import org.apache.commons.io.output.NullWriter
import spock.lang.Specification

class TemplateBasedApplierSpec extends Specification {

	def 'should throw usage exception when template not found'() {
		given:
			final applier = new TemplateBasedApplier(
					new NullWriter(), "some_complete_rubbish", null, ";", DelimiterType.normal, null)

		when:
			applier.apply null

		then:
			final e = thrown UsageException
			e.message == "Could not find template named some_complete_rubbish_apply.ftl\n" +
					"Check that you have got the name of the database syntax correct."
	}
}
