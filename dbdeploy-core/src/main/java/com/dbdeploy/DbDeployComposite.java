package com.dbdeploy;

import com.dbdeploy.database.*;

import java.io.File;
import java.util.*;

public class DbDeployComposite implements IDbDeploy {

	private final Iterable<IDbDeploy> values;


	public DbDeployComposite(Iterable<File> scriptDirectories) {
		Collection<IDbDeploy> deploys = new LinkedList<>();

		scriptDirectories.forEach(dir -> deploys.add(new DbDeploy(dir)));

		this.values = Collections.unmodifiableCollection(deploys);
	}


	@Override public void setDriver(String driver) {
		each(x -> x.setDriver(driver));
	}


	@Override public void setUrl(String url) {
		each(x -> x.setUrl(url));
	}


	@Override public void setUserid(String userid) {
		each(x -> x.setUserid(userid));
	}


	@Override public void setPassword(String password) {
		each(x -> x.setPassword(password));
	}


	@Override public void setOutputfile(File outputfile) {
		each(x -> x.setOutputfile(outputfile));
	}


	@Override public void setDbms(String dbms) {
		each(x -> x.setDbms(dbms));
	}


	@Override public void setLastChangeToApply(Long lastChangeToApply) {
		each(x -> x.setLastChangeToApply(lastChangeToApply));
	}


	@Override public void setUndoOutputfile(File undoOutputfile) {
		each(x -> x.setUndoOutputfile(undoOutputfile));
	}


	@Override public void setChangeLogTableName(String changeLogTableName) {
		each(x -> x.setChangeLogTableName(changeLogTableName));
	}


	@Override public void setEncoding(String encoding) {
		each(x -> x.setEncoding(encoding));
	}


	@Override public void setLineEnding(LineEnding lineEnding) {
		each(x -> x.setLineEnding(lineEnding));
	}

	@Override public void setDelimiterType(DelimiterType delimiterType) {
		each(x -> x.setDelimiterType(delimiterType));
	}


	@Override public void go() throws Exception {
		for (IDbDeploy value : values)
			value.go();
	}


	/* HELPERS */

	private interface ThrowingConsumer {
		void accept(IDbDeploy value) throws RuntimeException;
	}


	private void each(ThrowingConsumer f) {
		for (IDbDeploy value : values)
			f.accept(value);
	}
}
