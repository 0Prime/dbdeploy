package com.dbdeploy;

import com.dbdeploy.appliers.*;
import com.dbdeploy.database.*;
import com.dbdeploy.database.changelog.*;
import com.dbdeploy.exceptions.UsageException;
import com.dbdeploy.scripts.*;

import java.io.*;

public class DbDeploy implements IDbDeploy {
	private String url;
	private String userid;
	private String password;
	private String encoding = "UTF-8";
	private File scriptdirectory;
	private File outputfile;
	private File undoOutputfile;
	private LineEnding lineEnding = LineEnding.platform;
	private String dbms;
	private Long lastChangeToApply = Long.MAX_VALUE;
	private String driver;
	private String changeLogTableName = "changelog";
	private String delimiter = ";";
	private DelimiterType delimiterType = DelimiterType.normal;
	private File templatedir;

	public DbDeploy(File scriptdirectory) {
		this.scriptdirectory = scriptdirectory;
	}

	@Override public void setDriver(String driver) {
		this.driver = driver;
	}

	@Override public void setUrl(String url) {
		this.url = url;
	}

	@Override public void setUserid(String userid) {
		this.userid = userid;
	}

	@Override public void setPassword(String password) {
		this.password = password;
	}

	@Override public void setOutputfile(File outputfile) {
		this.outputfile = outputfile;
	}

	@Override public void setDbms(String dbms) {
		this.dbms = dbms;
	}

	@Override public void setLastChangeToApply(Long lastChangeToApply) {
		this.lastChangeToApply = lastChangeToApply;
	}

	@Override public void setUndoOutputfile(File undoOutputfile) {
		this.undoOutputfile = undoOutputfile;
	}

	@Override public void setChangeLogTableName(String changeLogTableName) {
		this.changeLogTableName = changeLogTableName;
	}

	@Override public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override public void setLineEnding(LineEnding lineEnding) {
		this.lineEnding = lineEnding;
	}

	@Override public void go() throws Exception {
		System.err.println(getWelcomeString());

		validate();

		Class.forName(driver);

		QueryExecuter queryExecuter = new QueryExecuter(url, userid, password);

		DatabaseSchemaVersionManager databaseSchemaVersionManager = new DatabaseSchemaVersionManager(
				queryExecuter, changeLogTableName);

		ChangeScriptRepository changeScriptRepository = new ChangeScriptRepository(
				new DirectoryScanner(encoding).getChangeScriptsForDirectory(scriptdirectory));

		ChangeScriptApplier doScriptApplier;

		if (outputfile != null) {
			doScriptApplier = new TemplateBasedApplier(
					new PrintWriter(outputfile, encoding), dbms,
					changeLogTableName, delimiter, delimiterType, getTemplatedir());
		}
		else {
			QueryStatementSplitter splitter = new QueryStatementSplitter();
			splitter.setDelimiter(getDelimiter());
			splitter.setDelimiterType(getDelimiterType());
			splitter.setOutputLineEnding(lineEnding);
			doScriptApplier = new DirectToDbApplier(queryExecuter, databaseSchemaVersionManager, splitter);
		}

		ChangeScriptApplier undoScriptApplier = null;

		if (undoOutputfile != null)
			undoScriptApplier = new UndoTemplateBasedApplier(
					new PrintWriter(undoOutputfile), dbms, changeLogTableName, delimiter, delimiterType, templatedir);

		Controller controller = new Controller(changeScriptRepository, databaseSchemaVersionManager, doScriptApplier, undoScriptApplier);

		controller.processChangeScripts(lastChangeToApply);

		queryExecuter.close();
	}


	private void validate() throws UsageException {
		checkForRequiredParameter(userid, "userid");
		checkForRequiredParameter(driver, "driver");
		checkForRequiredParameter(url, "url");
		checkForRequiredParameter(scriptdirectory, "dir");

		if (scriptdirectory == null || !scriptdirectory.isDirectory())
			throw new UsageException("Script directory must point to a valid directory");
	}


	private void checkForRequiredParameter(String parameterValue, String parameterName) throws UsageException {
		if (parameterValue == null || parameterValue.length() == 0)
			UsageException.throwForMissingRequiredValue(parameterName);
	}

	private void checkForRequiredParameter(Object parameterValue, String parameterName) throws UsageException {
		if (parameterValue == null)
			UsageException.throwForMissingRequiredValue(parameterName);
	}

	public String getUserid() {
		return userid;
	}

	public String getUrl() {
		return url;
	}

	public String getPassword() {
		return password;
	}

	public File getScriptdirectory() {
		return scriptdirectory;
	}

	public File getOutputfile() {
		return outputfile;
	}

	public File getUndoOutputfile() {
		return undoOutputfile;
	}

	public String getDbms() {
		return dbms;
	}

	public Long getLastChangeToApply() {
		return lastChangeToApply;
	}

	public String getDriver() {
		return driver;
	}

	public void setTemplatedir(File templatedir) {
		this.templatedir = templatedir;
	}

	public File getTemplatedir() {
		return templatedir;
	}

	public String getChangeLogTableName() {
		return changeLogTableName;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public DelimiterType getDelimiterType() {
		return delimiterType;
	}

	public void setDelimiterType(DelimiterType delimiterType) {
		this.delimiterType = delimiterType;
	}

	public String getWelcomeString() {
		String version = getClass().getPackage().getImplementationVersion();
		return "dbdeploy " + version;
	}

	public String getEncoding() {
		return encoding;
	}

	public LineEnding getLineEnding() {
		return lineEnding;
	}
}
