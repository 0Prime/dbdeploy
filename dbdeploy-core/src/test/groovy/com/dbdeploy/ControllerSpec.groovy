package com.dbdeploy

import com.dbdeploy.scripts.ChangeScript
import spock.lang.Specification

class ControllerSpec extends Specification {

	AvailableChangeScriptsProvider availableChangeScriptsProvider
	AppliedChangesProvider appliedChangesProvider
	Controller controller
	ChangeScript change1
	ChangeScript change2
	ChangeScript change3

	StubChangeScriptApplier applier
	StubChangeScriptApplier undoApplier


	def 'should apply change scripts in order'() {
		when:
			controller.processChangeScripts(Long.MAX_VALUE)

		then:
			1 * appliedChangesProvider.getAppliedChanges() >> []

		and:
			applier.changeScripts == [change1, change2, change3]
	}


	def 'should not crash when passed a null undo applier'() {
		given:
			controller = new Controller(availableChangeScriptsProvider, appliedChangesProvider, applier, null)

		when:
			controller.processChangeScripts(Long.MAX_VALUE)

		then:
			1 * appliedChangesProvider.getAppliedChanges() >> []
	}


	def 'should apply undo scripts in reverse order'() {
		when:
			controller.processChangeScripts(Long.MAX_VALUE)

		then:
			1 * appliedChangesProvider.getAppliedChanges() >> []

		and:
			undoApplier.changeScripts == [change3, change2, change1]
	}


	def 'should ignore changes already applied to the database'() {
		when:
			controller.processChangeScripts Long.MAX_VALUE

		then:
			1 * appliedChangesProvider.getAppliedChanges() >> [1L]

		and:
			applier.changeScripts == [change2, change3]
	}


	def 'should not apply changes greater than the max change to apply'() {
		when:
			controller.processChangeScripts 2L

		then:
			1 * appliedChangesProvider.getAppliedChanges() >> []

		and:
			applier.changeScripts == [change1, change2]
	}


	/* LIFECYCLE */

	def setup() {
		change1 = new ChangeScript(1)
		change2 = new ChangeScript(2)
		change3 = new ChangeScript(3)

		appliedChangesProvider = Mock(AppliedChangesProvider)

		availableChangeScriptsProvider = Mock(AvailableChangeScriptsProvider) {
			getAvailableChangeScripts() >> [change1, change2, change3]
		}

		applier = new StubChangeScriptApplier()
		undoApplier = new StubChangeScriptApplier()

		controller = new Controller(availableChangeScriptsProvider, appliedChangesProvider, applier, undoApplier)
	}


	class StubChangeScriptApplier implements ChangeScriptApplier {
		private List<ChangeScript> changeScripts

		void apply(List<ChangeScript> changeScripts) {
			this.changeScripts = new ArrayList<ChangeScript>(changeScripts)
		}
	}
}
