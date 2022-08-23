package org.devocative.artemis.log;

class MessageParser {
	private boolean showANSIFormats;

	MessageParser(boolean showANSIFormats) {
		this.showANSIFormats = showANSIFormats;
	}

	private static String argToAnsiFormatter(String theArg) {
		switch (theArg) {
			case "black":
				return "\u001B[30m";
			case "red":
				return "\u001B[31m";
			case "green":
				return "\u001B[32m";
			case "yellow":
				return "\u001B[33m";
			case "blue":
				return "\u001B[34m";
			case "purple":
				return "\u001B[35m";
			case "cyan":
				return "\u001B[36m";
			case "white":
				return "\u001B[37m";
			case "reset":
				return "\u001B[0m";
			default:
				return "";
		}
	}

	public String parseFormatters(String sub) {
		int percentIndex = sub.indexOf("%");
		if (percentIndex == -1) return sub;
		String leadingPercent = sub.substring(percentIndex + 1);
		int openIndex = leadingPercent.indexOf("(");
		if (openIndex == -1) return sub;
		String leadingParentheses = leadingPercent.substring(openIndex + 1);
		String inner = parseFormatters(leadingParentheses);
		int closeIndex = inner.indexOf(")");
		if (closeIndex == -1) return sub;
		String theArg = leadingPercent.substring(0, openIndex);
		String preMsg = sub.substring(0, percentIndex);
		String theMsg = inner.substring(0, closeIndex);
		String postMsg = inner.substring(closeIndex + 1);
		return parseFormatters(makeMessage(preMsg, theMsg, postMsg, theArg));
	}

	private String makeMessage(String preMsg, String theMsg, String postMsg, String theArg) {
		String argInterpreted = this.showANSIFormats ? argToAnsiFormatter(theArg) : "";
		String closeInterpreted = this.showANSIFormats ? argToAnsiFormatter("reset") : "";
		return preMsg + argInterpreted + theMsg + closeInterpreted + postMsg;
	}
}
