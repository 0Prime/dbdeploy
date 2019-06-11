package com.dbdeploy;

import com.dbdeploy.database.*;
import com.dbdeploy.exceptions.UsageException;
import org.apache.commons.cli.*;

import java.beans.*;
import java.io.File;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class DbDeployCommandLineParser {

	private final UserInputReader userInputReader;
	private final Function<File, StrategySelector.Strategy> strategySelector;


	DbDeployCommandLineParser() {
		this(new UserInputReader(), new StrategySelector());
	}


	DbDeployCommandLineParser(UserInputReader userInputReader, Function<File, StrategySelector.Strategy> strategySelector) {
		this.userInputReader = userInputReader;
		this.strategySelector = strategySelector;
	}


	IDbDeploy parse(String[] args) throws UsageException {
		try {
			return parseInternal(args);
		}
		catch (ParseException e) {
			throw new UsageException(e.getMessage(), e);
		}
	}


	public IDbDeploy makeDbDeploy(StrategySelector.Strategy strategy, File patches) {
		System.out.println(MessageFormat.format("strategy: {0}", strategy));

		switch (strategy) {
			case LINEAR:
				return new DbDeploy(patches);

			case TREE:
				return new DbDeployComposite(Arrays.asList(requireNonNull(patches.listFiles())));

			case NOT_EXISTS:
				throw UsageException.of("failed to find directory: {0}", patches.getAbsolutePath());

			case NOT_DIRECTORY:
				throw UsageException.of("not a directory: {0}", patches.getAbsolutePath());

			case EMPTY:
				throw UsageException.of("empty directory: {0}", patches.getAbsolutePath());

			case MIXED:
				throw UsageException.of(
						"patches dir should contain either only sql-patches or only sub-directories with sql-patches: {0}",
						patches.getAbsolutePath());

			case INVALID_TREE:
				throw UsageException.of("patches sub-directories should contain no directories: {0}",
				                        patches.getAbsolutePath());
		}

		throw new IllegalStateException(MessageFormat.format(
				"unsupported strategy: {0}", strategy));
	}


	private IDbDeploy parseInternal(String[] args) throws ParseException {

		CommandLine commandLine = new DefaultParser().parse(getOptions(), args);

		final File patches = resolvePatchesDirectory(commandLine);

		final StrategySelector.Strategy strategy = strategySelector.apply(patches);

		final IDbDeploy dbDeploy = makeDbDeploy(strategy, patches);

		copyValuesFromCommandLineToDbDeployBean(dbDeploy, commandLine);

		if (commandLine.hasOption("password") && commandLine.getOptionValue("password") == null)
			dbDeploy.setPassword(userInputReader.read("Password"));

		return dbDeploy;
	}


	private File resolvePatchesDirectory(CommandLine commandLine) {
		return new File(
				commandLine.hasOption("scriptdirectory")
						? commandLine.getOptionValue("scriptdirectory")
						: ".");
	}


	private void copyValuesFromCommandLineToDbDeployBean(IDbDeploy dbDeploy, CommandLine commandLine) {
		try {
			final BeanInfo info = Introspector.getBeanInfo(dbDeploy.getClass());

			for (PropertyDescriptor p : info.getPropertyDescriptors()) {
				final String propertyName = p.getDisplayName();

				if (commandLine.hasOption(propertyName)) {
					Object value = commandLine.getOptionValue(propertyName);

					if (p.getPropertyType().isAssignableFrom(File.class))
						value = new File((String) value);

					final Method writeMethod = p.getWriteMethod();

					if (writeMethod != null)
						writeMethod.invoke(dbDeploy, value);
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
