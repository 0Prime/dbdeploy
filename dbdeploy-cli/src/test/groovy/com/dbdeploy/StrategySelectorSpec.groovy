package com.dbdeploy


import spock.lang.*

import static com.dbdeploy.StrategySelector.Strategy

class StrategySelectorSpec extends Specification {

	StrategySelector strategySelector = new StrategySelector()


	@Unroll
	def 'strategySelector.apply -> OK'() {
		when:
			final actual = strategySelector.apply patchesDir

		then:
			actual == expected

		where:
			testData << [
					[expected  : Strategy.NOT_EXISTS,
					 patchesDir: Mock(File, { exists() >> false })],

					[expected  : Strategy.NOT_DIRECTORY,
					 patchesDir:
							 Mock(File, {
								 exists() >> true
								 isDirectory() >> false
							 })],

					[expected  : Strategy.EMPTY,
					 patchesDir:
							 Mock(File, {
								 exists() >> true
								 isDirectory() >> true
								 listFiles() >> ([] as File[])
							 })],

					[expected  : Strategy.LINEAR,
					 patchesDir:
							 Mock(File, {
								 exists() >> true
								 isDirectory() >> true
								 listFiles() >> ([Mock(File)] as File[])
								 listFiles(_ as FileFilter) >> ([] as File[])
							 })],

					[expected  : Strategy.MIXED,
					 patchesDir:
							 Mock(File, {
								 exists() >> true
								 isDirectory() >> true
								 listFiles() >> ([Mock(File), Mock(File)] as File[])
								 listFiles(_ as FileFilter) >> ([Mock(File)] as File[])
							 })],

					[expected  : Strategy.INVALID_TREE,
					 patchesDir:
							 Mock(File, {
								 exists() >> true
								 isDirectory() >> true
								 listFiles() >> ([Mock(File)] as File[])
								 listFiles(_ as FileFilter) >> ([Mock(File, {
									 listFiles() >> ([Mock(File, { isDirectory() >> true })])
								 })] as File[])
							 })],

					[expected  : Strategy.TREE,
					 patchesDir:
							 Mock(File, {
								 exists() >> true
								 isDirectory() >> true
								 listFiles() >> ([Mock(File)] as File[])
								 listFiles(_ as FileFilter) >> ([Mock(File, {
									 listFiles() >> ([Mock(File, { isDirectory() >> false })])
								 })] as File[])
							 })],
			]

			expected = testData.expected
			patchesDir = testData.patchesDir as File
	}
}
