package com.dbdeploy.appliers;

import com.dbdeploy.database.DelimiterType;

import java.io.*;

public class UndoTemplateBasedApplier extends TemplateBasedApplier {
	public UndoTemplateBasedApplier(Writer writer, String syntax,
	                                String changeLogTableName, String delimiter, DelimiterType delimiterType, File templateDirectory) throws IOException {
		super(writer, syntax, changeLogTableName, delimiter, delimiterType, templateDirectory);
	}

	@Override
	protected String getTemplateQualifier() {
		return "undo";
	}
}
