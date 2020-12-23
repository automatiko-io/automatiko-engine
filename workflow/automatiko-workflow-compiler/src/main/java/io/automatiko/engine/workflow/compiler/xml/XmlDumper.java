package io.automatiko.engine.workflow.compiler.xml;

public class XmlDumper {
	public static String replaceIllegalChars(final String code) {
		final StringBuilder sb = new StringBuilder();
		if (code != null) {
			final int n = code.length();
			for (int i = 0; i < n; i++) {
				final char c = code.charAt(i);
				switch (c) {
				case '<':
					sb.append("&lt;");
					break;
				case '>':
					sb.append("&gt;");
					break;
				case '&':
					sb.append("&amp;");
					break;
				default:
					sb.append(c);
					break;
				}
			}
		} else {
			sb.append("null");
		}
		return sb.toString();
	}
}
