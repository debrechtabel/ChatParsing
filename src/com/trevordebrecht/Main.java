package com.trevordebrecht;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class Main {

	// ideally this would go into some sort of account controller, but this is out of scope for this project
	// setting a new username basically just for fun here
	public static String username = "username";

    public static void main(String[] args) {

	    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	    System.out.println();
	    while (true) {
		    System.out.print(username + " > ");

		    String input;
		    try {
			    input = in.readLine();
		    }
		    catch (IOException e) {
			    e.printStackTrace();
			    break;
		    }

		    if (input == null) break;

		    if (input.length() > 0) {

			    // attempt to parse as a command
			    TokenFactory.Command command = TokenFactory.Command.parseCommand(input);
			    if (command != null) {
				    System.out.print(command.getMessage());
				    if (command.shouldQuit())
					    break;
			    }

			    // parse as a message
			    else {
				    Map<String, List<TokenFactory.Token>> tokens = TokenFactory.Token.parseTokens(input);

				    printJson(tokens);
			    }
		    }
	    }

	    System.out.println("Logged out.");
    }

	private static void printJson(Map<String, List<TokenFactory.Token>> jsonMap) {
		if (jsonMap.isEmpty()) return;

		// for a more robust solution, using a 3rd party library for JSON support would be better
		// for this, a simple loop for printing the json will suffice
		StringBuilder sb = new StringBuilder("{\n");
		boolean firstTokenType = true;
		for (Map.Entry<String, List<TokenFactory.Token>> entry : jsonMap.entrySet()) {
			if (!firstTokenType) {
				sb.append(",\n");
			}
			else firstTokenType = false;

			sb.append("    \"").append(entry.getKey()).append("\": [\n");

			boolean firstToken = true;
			for (TokenFactory.Token token : entry.getValue()) {
				if (!firstToken) {
					sb.append(",\n");
				}
				else firstToken = false;

				sb.append("        ").append(token);
			}

			sb.append("\n    ]");
		}
		sb.append("\n}\n");

		System.out.print(sb.toString());
	}
}
