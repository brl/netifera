package com.netifera.platform.util.patternmatching;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex implements IPattern {
	private Pattern pattern;
	private final Map<String, String> information = new HashMap<String, String>();
	
	public static Regex caseInsensitive(final String pattern) {
		return new Regex(Pattern.compile(pattern, Pattern.MULTILINE|Pattern.DOTALL|Pattern.CASE_INSENSITIVE));
	}
	
	public Regex(final String pattern) {
		this(Pattern.compile(pattern, Pattern.MULTILINE|Pattern.DOTALL));
	}
	
	public Regex(final Pattern pattern) {
		this.pattern = pattern;
	}

	private String escape(String string) {
		return string.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
	
	private String escape(Pattern regex) {
		return escape(regex.toString().replaceAll("\r", "\\r").replaceAll("\n", "\\n"));
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("\t<ServicePattern regex=\""+escape(escape(pattern))+"\">\n");
		if (information.containsKey("service"))
				buffer.append("\t\t<service>"+escape(information.get("service"))+"</service>\n");
		for (String name: new String[] {"os", "distribution", "arch", "product", "version", "build", "hostname", "username", "password"}) {
			if (information.containsKey(name))
				buffer.append("\t\t<"+name+">"+escape(information.get(name))+"</"+name+">\n");
		}
		buffer.append("\t</ServicePattern>");
		return buffer.toString();
	}
	
	public void add(final String name, final String value) {
		if (value != null)
			information.put(name, value);
	}
	
	public boolean match(final Map<String, String> answer, final String data) {
		Matcher matcher = pattern.matcher(data);
		
		if (!matcher.matches()) return false;
		fillOutAnswer(answer, matcher);
		return true;
	}
	
	public Map<String, String> match(final String data) {
		Matcher matcher = pattern.matcher(data);
		
		if (!matcher.matches()) return null;
		Map<String, String> answer = new HashMap<String,String>();
		fillOutAnswer(answer, matcher);
		return answer;
	}
	
	private void fillOutAnswer(final Map<String,String> answer, final Matcher matcher) {
		for (String name: information.keySet()) {
			String value = information.get(name);
			for (int i=0; i<=matcher.groupCount(); i++) {
				if (value.contains("{$"+i+"}"))
					value = value.replaceAll("\\{\\$"+i+"\\}", matcher.group(i));
			}
			answer.put(name, value);
		}
	}
}
