package org.devocative.artemis.cli;

import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

public class ArtemisCLIMain {

	public static void main(String[] args) {
		AnsiConsole.systemInstall();

		final CommandLine line = new CommandLine(new ArtemisCommand());
		line
			.addSubcommand(new CExec())
			.addSubcommand(new CInit())
			.execute(args);

		AnsiConsole.systemUninstall();
	}

	@CommandLine.Command(name = "\b")
	private static class ArtemisCommand {
	}

}
