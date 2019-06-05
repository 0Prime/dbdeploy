package com.dbdeploy;

import java.io.File;
import java.util.function.Function;

public class StrategySelector implements Function<File, StrategySelector.Strategy> {

	enum Strategy {
		NOT_EXISTS,
		NOT_DIRECTORY,
		EMPTY,
		LINEAR,
		MIXED,
		INVALID_TREE,
		TREE
	}


	@Override public Strategy apply(File patchesDir) {
		if (!patchesDir.exists())
			return Strategy.NOT_EXISTS;

		if (!patchesDir.isDirectory())
			return Strategy.NOT_DIRECTORY;

		final File[] allFiles = patchesDir.listFiles();

		//noinspection ConstantConditions (covered in previous 'if')
		if (allFiles.length == 0)
			return Strategy.EMPTY;

		final File[] subDirs = patchesDir.listFiles(File::isDirectory);

		//noinspection ConstantConditions (covered in previous 'if')
		if (subDirs.length == 0)
			return Strategy.LINEAR;

		if (allFiles.length != subDirs.length)
			return Strategy.MIXED;

		for (File subDir : subDirs)
			//noinspection ConstantConditions (covered in previous 'if')
			for (File file : subDir.listFiles())
				if (file.isDirectory())
					return Strategy.INVALID_TREE;

		return Strategy.TREE;
	}
}
