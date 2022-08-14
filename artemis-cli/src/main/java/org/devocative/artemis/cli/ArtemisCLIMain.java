package org.devocative.artemis.cli;

import picocli.CommandLine;

public class ArtemisCLIMain {

	public static void main(String[] args) {
		final CommandLine line = new CommandLine(new ArtemisCommand());
		line
			.addSubcommand(new CExec())
			.execute(args);
	}

	@CommandLine.Command(name = "\b")
	private static class ArtemisCommand {
	}

}
