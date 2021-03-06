package com.dbdeploy;

import com.dbdeploy.exceptions.UsageException;

public class CommandLineTarget {

	public static void main(String[] args) {

		DbDeployCommandLineParser commandLineParser = new DbDeployCommandLineParser();

		try {
			IDbDeploy dbDeploy = commandLineParser.parse(args);
			dbDeploy.go();
		}
		catch (UsageException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			commandLineParser.printUsage();
		}
		catch (Exception ex) {
			System.err.println("Failed to apply changes: " + ex);
			ex.printStackTrace();
			System.exit(2);
		}

		System.exit(0);
	}
}
