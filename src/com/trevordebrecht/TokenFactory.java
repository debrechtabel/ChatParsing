package com.trevordebrecht;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

public class TokenFactory {

	private static Map<String, Class<? extends Token>> tokenRegistry;
	private static Map<String, Class<? extends Command>> commandRegistry;

	static {
		tokenRegistry = new HashMap<String, Class<? extends Token>>(1);
		tokenRegistry.put(MentionToken.TAG, MentionToken.class);
		tokenRegistry.put(EmoticonToken.TAG, EmoticonToken.class);
		tokenRegistry.put(LinkToken.TAG, LinkToken.class);

		commandRegistry = new HashMap<String, Class<? extends Command>>(3);
		commandRegistry.put(HelpCommand.REGEX, HelpCommand.class);
		commandRegistry.put(QuitCommand.REGEX, QuitCommand.class);
		commandRegistry.put(SetUsernameCommand.REGEX, SetUsernameCommand.class);
	}

	public static abstract class Token {
		private static final String REGEX = MentionToken.REGEX + "|" +
		                                    EmoticonToken.REGEX + "|" +
		                                    LinkToken.REGEX;

		public static Map<String, List<Token>> parseTokens(String in) {
			Pattern p = Pattern.compile(REGEX);
			Matcher matcher = p.matcher(in);

			// json mapping of all tokens
			Map<String, List<Token>> tokenMap = new HashMap<String, List<Token>>();

			while (matcher.find()) {

				// not looping through this would be awesome, but java has no support for getting the matched group name
				for (Map.Entry<String, Class<? extends Token>> entry : tokenRegistry.entrySet()) {

					// is the match this type of token?
					String tag = entry.getKey();
					String token = matcher.group(tag);

					// yes!
					if (token != null) {

						Token t = null;
						try {
							t = entry.getValue().newInstance();
							t.init(token);
						}
						catch (Exception e) {
							e.printStackTrace();
						}

						// if token was invalid or an issue happened, don't save it
						if (t != null) {
							List<Token> tokenList = tokenMap.get(tag);
							if (tokenList == null) {
								tokenList = new ArrayList<Token>(1);
								tokenMap.put(tag, tokenList);
							}

							tokenList.add(t);
						}

						break;
					}
				}
			}

			return tokenMap;
		}

		protected abstract void init(String in) throws InvalidTokenException;
	}

	private static class MentionToken extends Token {
		private static final String TAG = "mentions";
		private static final String REGEX = "(?<" + TAG + ">@\\w+)";

		private String mUsername;

		public MentionToken() {}

		@Override
		protected void init(String in) throws InvalidTokenException {
			// strip @
			mUsername = in.substring(1);
		}

		@Override
		public String toString() {
			return mUsername;
		}
	}

	private static class EmoticonToken extends Token {
		private static final String TAG = "emoticons";
		private static final String REGEX = "(?<" + TAG + ">\\(\\w{1,15}\\))";

		private String mEmoticon;

		public EmoticonToken() {}

		@Override
		protected void init(String in) throws InvalidTokenException {
			// strip parens
			mEmoticon = in.substring(1, in.length() - 1);
		}

		@Override
		public String toString() {
			return mEmoticon;
		}
	}

	private static class LinkToken extends Token {
		private static final String TAG = "links";
		private static final String REGEX = "(?<" + TAG + ">(https?|ftp):\\/\\/[^\\s/$.?#].[^\\s]*)";

		private String mTitle;
		private String mUrl;

		public LinkToken() {}

		@Override
		protected void init(String in) throws InvalidTokenException {
			try {
				URL url = new URL(in);
				BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

				HTMLEditorKit kit = new HTMLEditorKit();
				Document document = kit.createDefaultDocument();
				document.putProperty("IgnoreCharsetDirective", true);
				kit.read(reader, document, 0);

				mTitle = (String)document.getProperty(Document.TitleProperty);
				mUrl = in;
			}
			catch (Exception e) {
				throw new InvalidTokenException(e);
			}
		}

		@Override
		public String toString() {
			return "{\n" +
			       "            \"url\": \"" + mUrl + "\",\n" +
			       "            \"title\": \"" + mTitle + "\"\n" +
			       "        }";
		}
	}

	public static abstract class Command {
		private static final String REGEX = "^/[a-zA-Z]+";

		public static Command parseCommand(String in) {
			Pattern p = Pattern.compile(REGEX);
			Matcher matcher = p.matcher(in);

			if (!matcher.find()) {
				return null;
			}

			for (Map.Entry<String, Class<? extends Command>> entry : commandRegistry.entrySet()) {
				if (Pattern.matches(entry.getKey(), in)) {
					try {
						Command cmd = entry.getValue().newInstance();
						cmd.init(in);
						return cmd;
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			HelpCommand cmd = new HelpCommand();
			cmd.init(in);
			return cmd;
		}

		protected abstract void init(String in);

		public abstract String getMessage();

		public abstract boolean shouldQuit();
	}

	private static class HelpCommand extends Command {
		private static final String REGEX = "^/help\\s*$";

		public HelpCommand() { }

		@Override
		protected void init(String in) { }

		@Override
		public String getMessage() {
			return "Parse chat input by simply typing a message.\n\n" +
					"Commands: \n" +
					"    /help - display this message\n" +
					"    /setUsername [username] - set the username for the input prompt\n" +
					"    /quit - quit this program\n\n";
		}

		@Override
		public boolean shouldQuit() {
			return false;
		}
	}

	private static class QuitCommand extends Command {
		private static final String REGEX = "^/quit\\s*$";

		public QuitCommand() { }

		@Override
		protected void init(String in) { }

		@Override
		public String getMessage() {
			return "Goodbye!\n";
		}

		@Override
		public boolean shouldQuit() {
			return true;
		}
	}

	private static class SetUsernameCommand extends Command {
		private static final String REGEX = "^/setUsername (\\S+)$";

		private boolean mSuccess;

		public SetUsernameCommand() { }

		@Override
		protected void init(String in) {
			Pattern p = Pattern.compile(REGEX);
			Matcher matcher = p.matcher(in);

			if (matcher.find()) {
				Main.username = matcher.group(1);
				mSuccess = true;
			}
		}

		@Override
		public String getMessage() {
			return mSuccess ? "Username successfully changed\n" : "Username change failed. For help, enter /help\n";
		}

		@Override
		public boolean shouldQuit() {
			return false;
		}
	}

	private static class InvalidTokenException extends Exception {
		public InvalidTokenException(Throwable cause) {
			super(cause);
		}
	}
}
