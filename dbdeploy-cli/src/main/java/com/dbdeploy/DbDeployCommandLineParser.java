package com.dbdeploy;

import com.dbdeploy.database.*;
import com.dbdeploy.exceptions.UsageException;
import org.apache.commons.cli.*;

import java.beans.*;
import java.io.File;

class DbDeployCommandLineParser {
	private final UserInputReader userInputReader;

	DbDeployCommandLineParser() {
		this(new UserInputReader());
	}

	DbDeployCommandLineParser(UserInputReader userInputReader) {
		this.userInputReader = userInputReader;
	}


	void parse(String[] args, DbDeploy dbDeploy) throws UsageException {
		try {
			dbDeploy.setScriptdirectory(new File("."));
			final CommandLine commandLine = new DefaultParser().parse(getOptions(), args);
			copyValuesFromCommandLineToDbDeployBean(dbDeploy, commandLine);

			if (commandLine.hasOption("password") && commandLine.getOptionValue("password") == null)
				dbDeploy.setPassword(userInputReader.read("Password"));
		}
		catch (ParseException e) {
			throw new UsageException(e.getMessage(), e);
		}
	}


	private void copyValuesFromCommandLineToDbDeployBean(DbDeploy dbDeploy, CommandLine commandLine) {
		try {
			final BeanInfo info = Introspector.getBeanInfo(dbDeploy.getClass());

			for (PropertyDescriptor p : info.getPropertyDescriptors()) {
				final String propertyName = p.getDisplayName();

				if (commandLine.hasOption(propertyName)) {
					Object value = commandLine.getOptionValue(propertyName);

					if (p.getPropertyType().isAssignableFrom(File.class))
						value = new File((String) value);

					p.getWriteMethod().invoke(dbDeploy, value);
				}
			}

			if (commandLine.hasOption("delimitertype"))
				dbDeploy.setDelimiterType(DelimiterType.valueOf(commandLine.getOptionValue("delimitertype")));

			if (commandLine.hasOption("lineending"))
				dbDeploy.setLineEnding(LineEnding.valueOf(commandLine.getOptionValue("lineending")));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("dbdeploy", getOptions());
	}


	@SuppressWarnings({"AccessStaticViaInstance"})
	private Options getOptions() {
		final Options options = new Options();

		options.addOption(Option.builder("U")
		                        .hasArg()
		                        .desc("database user id")
		                        .longOpt("userid")
		                        .build());

		options.addOption(Option.builder("P")
		                        .optionalArg(true)
		                        .numberOfArgs(1)
		                        .desc("database password (use -P without a argument value to be prompted)")
		                        .longOpt("password")
		                        .build());

		options.addOption(Option.builder("D")
		                        .hasArg()
		                        .desc("database driver class")
		                        .longOpt("driver")
		                        .build());

		options.addOption(Option.builder("u")
		                        .hasArg()
		                        .desc("database url")
		                        .longOpt("url")
		                        .build());

		options.addOption(Option.builder("s")
		                        .hasArg()
		                        .desc("directory containing change scripts (default: .)")
		                        .longOpt("scriptdirectory")
		                        .build());

		options.addOption(Option.builder("e")
		                        .hasArg()
		                        .desc("encoding for input and output files (default: UTF-8)")
		                        .longOpt("encoding")
		                        .build());

		options.addOption(Option.builder("o")
		                        .hasArg()
		                        .desc("output file")
		                        .longOpt("outputfile")
		                        .build());

		options.addOption(Option.builder("d")
		                        .hasArg()
		                        .desc("dbms type")
		                        .longOpt("dbms")
		                        .build());

		options.addOption(Option.builder()
		                        .hasArg()
		                        .desc("template directory")
		                        .longOpt("templatedir")
		                        .build());

		options.addOption(Option.builder("t")
		                        .hasArg()
		                        .desc("name of change log table to use (default: changelog)")
		                        .longOpt("changeLogTableName")
		                        .build());

		options.addOption(Option.builder()
		                        .hasArg()
		                        .desc("delimiter to separate sql statements")
		                        .longOpt("delimiter")
		                        .build());

		options.addOption(Option.builder()
		                        .hasArg()
		                        .desc("delimiter type to separate sql statements (row or normal)")
		                        .longOpt("delimitertype")
		                        .build());

		options.addOption(Option.builder()
		                        .hasArg()
		                        .desc("line ending to use when applying scripts direct to db (platform, cr, crlf, lf)")
		                        .longOpt("lineending")
		                        .build());

		return options;
	}
}
