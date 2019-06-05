package com.dbdeploy;

import com.dbdeploy.database.*;

import java.io.File;

public interface IDbDeploy {

	void setDriver(String driver);

	void setUrl(String url);

	void setUserid(String userid);

	void setPassword(String password);

	void setOutputfile(File outputfile);

	void setDbms(String dbms);

	void setLastChangeToApply(Long lastChangeToApply);

	void setUndoOutputfile(File undoOutputfile);

	void setChangeLogTableName(String changeLogTableName);

	void setEncoding(String encoding);

	void setLineEnding(LineEnding lineEnding);

	void setDelimiterType(DelimiterType delimiterType);


	void go() throws Exception;
}
