package com.dbdeploy.database;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.*;

import java.util.*;

public class QueryStatementSplitter {
	private String delimiter = ";";
	private DelimiterType delimiterType = DelimiterType.normal;
	private LineEnding lineEnding = LineEnding.platform;

	public QueryStatementSplitter() {
	}

	public List<String> split(String input) {
		List<String> statements = new ArrayList<>();
		StrBuilder currentSql = new StrBuilder();

		StrTokenizer lineTokenizer = new StrTokenizer(input);
		lineTokenizer.setDelimiterMatcher(StrMatcher.charSetMatcher("\r\n"));

		for (String line : lineTokenizer.getTokenArray()) {
			String strippedLine = StringUtils.stripEnd(line, null);
			if (!currentSql.isEmpty()) {
				currentSql.append(lineEnding.get());
			}

			currentSql.append(strippedLine);

			if (delimiterType.matches(strippedLine, delimiter)) {
				statements.add(currentSql.substring(0, currentSql.length() - delimiter.length()));
				currentSql.clear();
			}
		}

		if (!currentSql.isEmpty()) {
			statements.add(currentSql.toString());
		}

		return statements;
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

	public void setOutputLineEnding(LineEnding lineEnding) {
		this.lineEnding = lineEnding;
	}
}
