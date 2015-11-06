package de.swm.nis;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;



public class LogParser {

	/**
	 * parses a log line into a Row object
	 * 
	 * @param message
	 *            the message to convert
	 * @return row object, potentially null (for begin/commit) messages.
	 */
	public Row parseLogLine(String message) {

		Row row = null;
		// determine if we have a begin, commit or table modification message (summing up Insert, update,
		// delete):
		String startToken = message.substring(0, 6);
		switch (startToken) {
			case "BEGIN ": {
				log("Transaction start found, ignoring");
				break;
			}
			case "COMMIT": {
				log("Transaction end found, ignoring");
				break;
			}
			case "table ": {
				row = parseRow(message);
				break;
			}
		}
		return row;
	}



	/**
	 * Parses a line containing an update, insert or delete statement into a Row object.
	 * 
	 * @param message
	 *            the string to convert
	 * @return a row object describing the update.
	 */
	public Row parseRow(String message) {
		Row row = new Row();
		List<String> tokens = splitKeyValuePairs(message);
		row.setTableName(tokens.get(1).substring(0, tokens.get(1).length() - 1));
		switch (tokens.get(2)) {
			case "INSERT:": {
				// leave "old values" empty
				for (int i = 3; i < tokens.size(); i++) {
					row.getNewValues().add(parseCell(tokens.get(i)));
				}
				break;
			}
			case "UPDATE:": {
				boolean oldValues = true;
				for (int i = 3; i < tokens.size(); i++) {
					String token = tokens.get(i);
					if (token.equals("old-key:")) {
						continue;
					} else if (token.equals("new-tuple:")) {
						oldValues = false;
						continue;
					}
					if (oldValues) {
						row.getOldValues().add(parseCell(token));
					} else {
						row.getNewValues().add(parseCell(token));
					}
				}
				break;
			}
			case "DELETE:": {
				for (int i = 3; i < tokens.size(); i++) {
					// leave "new values" empty
					row.getOldValues().add(parseCell(tokens.get(i)));
				}
				break;
			}
		}
		return row;
	}



	/**
	 * Converts a String formatted like columnname[type]:value into its parts
	 * 
	 * @param description
	 * @return
	 */
	public Cell parseCell(String description) {
		// 1. split at ":"
		List<String> splitted = Lists.newArrayList(Splitter.on(':').trimResults().omitEmptyStrings().limit(2)
				.split(description));
		String nameAndType = splitted.get(0);
		String value = splitted.get(1);
		// 2. split the first part at "["
		List<String> nameAndTypeSplitted = Lists.newArrayList(Splitter.on('[').split(nameAndType));
		String name = nameAndTypeSplitted.get(0);
		String type = nameAndTypeSplitted.get(1).substring(0, nameAndTypeSplitted.get(1).length() - 1);
		return new Cell(type, name, value);
	}



	/**
	 * Splits a List of tokens separated by space into its parts. Keeps track of quoting: '' starts and ends a string
	 * within spaces are not considered as delimiters.
	 * 
	 * @param message
	 *            the string to split.
	 * @return List of Strings, the parts.
	 */
	public List<String> splitKeyValuePairs(String message) {
		// within '' escaped Strings blanks are not consiedered as delimiters
		List<String> tokens = Lists.newArrayList(Splitter.on(' ').trimResults().omitEmptyStrings().split(message));

		List<String> returnvalues = new ArrayList<String>(tokens.size());

		boolean quoteOn = false;
		StringBuffer collector = new StringBuffer();
		for (String token : tokens) {
			int singleQuoteCount = CharMatcher.is('\'').countIn(token);
			if (singleQuoteCount == 2) {
				// Begin of Quoting, collect all incoming tokens into a string
				if (!quoteOn) {
					quoteOn = true;
					collector = new StringBuffer(token);
				} else {
					// End of Quoting
					quoteOn = false;
					collector.append(" " + token);
					returnvalues.add(collector.toString());
				}
			} else if (quoteOn) {
				// Middle of quoting
				collector.append(" " + token);
			} else {
				returnvalues.add(token);
			}
		}
		return returnvalues;
	}



	// TODO setup logging
	private void log(String message) {
		System.out.println(message);
	}

}
