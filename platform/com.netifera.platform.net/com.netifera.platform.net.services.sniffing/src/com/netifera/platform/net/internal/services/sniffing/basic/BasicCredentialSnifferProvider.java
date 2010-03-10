package com.netifera.platform.net.internal.services.sniffing.basic;

import java.util.ArrayList;
import java.util.List;

import com.netifera.platform.net.services.sniffing.ICredentialSniffer;
import com.netifera.platform.net.services.sniffing.ICredentialSnifferProvider;
import com.netifera.platform.net.services.sniffing.RegexCredentialSniffer;
import com.netifera.platform.util.patternmatching.Regex;

public class BasicCredentialSnifferProvider implements ICredentialSnifferProvider {

	public List<ICredentialSniffer> getSniffers() {
		List<ICredentialSniffer> answer = new ArrayList<ICredentialSniffer>();
		
		Regex regex = Regex.caseInsensitive(".*USER[ ]+([^\r\n]*)[\r\n]+PASS[ ]+([^\r\n]*)[\r\n].*");
		regex.add("username", "{$1}");
		regex.add("password", "{$2}");
		answer.add(new RegexCredentialSniffer(new String[]{"FTP","POP3"},regex,null));

		regex = Regex.caseInsensitive("^[^ ]+ LOGIN ([^ \r\n]+) ([^\r\n]+)[\r\n].*");
		regex.add("username", "{$1}");
		regex.add("password", "{$2}");
		answer.add(new RegexCredentialSniffer(new String[]{"IMAP"},regex,null));

		answer.add(new HTTPCredentialSniffer());
		
		return answer;
	}
}
