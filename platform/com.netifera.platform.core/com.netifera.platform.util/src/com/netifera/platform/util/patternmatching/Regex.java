package com.netifera.platform.util.patternmatching;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex implements IPattern {
	private Pattern pattern;
	private final Map<Integer, String> groupNames =
		new HashMap<Integer, String>();
	private final Map<String, String> defaults = new HashMap<String, String>();
	
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
		buffer.append("\t\t<service>"+escape(defaults.get("serviceType"))+"</service>\n");
		for (String name: new String[] {"os", "distribution", "arch", "product", "version", "build", "hostname", "username", "password"}) {
			if (defaults.containsKey(name))
				buffer.append("\t\t<"+name+">"+escape(defaults.get(name))+"</"+name+">\n");
			else if (groupNames.containsValue(name)) {
				for (Integer group: groupNames.keySet())
					if (groupNames.get(group).equals(name)) {
						buffer.append("\t\t<"+name+">$regex-group-"+group+"</"+name+">\n");
						break;
					}
			}
		}
		buffer.append("\t</ServicePattern>");
		return buffer.toString();
	}
	
	public void add(final Integer index, final String name) {
		groupNames.put(index, name);
	}
	
	public void add(final Integer index, final String name, final String defaultValue) {
		add(index, name);
		add(name, defaultValue);
	}
	
	public void add(final String name, final String defaultValue) {
		if (defaultValue != null)
			defaults.put(name, defaultValue);
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
		for (String name: defaults.keySet()) {
			answer.put(name, defaults.get(name));
		}
		for (Integer groupNumber: groupNames.keySet()) {
			answer.put(groupNames.get(groupNumber), new String(matcher.group(groupNumber.intValue()).trim()));
		}
	}
}
