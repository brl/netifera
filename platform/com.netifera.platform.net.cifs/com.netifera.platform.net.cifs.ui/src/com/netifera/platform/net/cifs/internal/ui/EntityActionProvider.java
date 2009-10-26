package com.netifera.platform.net.cifs.internal.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.net.cifs.tools.LMAuthBruteforcer;
import com.netifera.platform.net.cifs.tools.NTLMAuthBruteforcer;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.net.wordlists.IWordList;
import com.netifera.platform.net.wordlists.IWordListManager;
import com.netifera.platform.tools.options.BooleanOption;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.tools.options.IntegerOption;
import com.netifera.platform.tools.options.MultipleStringOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.actions.IEntityActionProvider;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class EntityActionProvider implements IEntityActionProvider {
	
	private IWordListManager wordListManager;
	
	public List<IAction> getActions(IShadowEntity entity) {
		List<IAction> answer = new ArrayList<IAction>();
		
		if (entity instanceof ServiceEntity) {
			ServiceEntity serviceEntity = (ServiceEntity) entity;
			TCPSocketLocator locator = (TCPSocketLocator) serviceEntity.getAdapter(TCPSocketLocator.class);
			if (locator != null && serviceEntity.getServiceType().equals("NetBIOS-SSN")) {
				ToolAction bruteforcer = new ToolAction("Bruteforce LM Authentication", LMAuthBruteforcer.class.getName());
				bruteforcer.addFixedOption(new GenericOption(TCPSocketLocator.class, "target", "Target", "Target SMB service", locator));
//				bruteforcer.addOption(new IterableOption(UsernameAndPassword.class, "credentials", "Credentials", "List of credentials to try", null));
				bruteforcer.addOption(new StringOption("usernames", "Usernames", "List of usernames to try, separated by space or comma", "Usernames", "", true));
				bruteforcer.addOption(new MultipleStringOption("usernames_wordlists", "Usernames Wordlists", "Wordlists to try as usernames", "Usernames", getAvailableWordLists(new String[] {IWordList.CATEGORY_USERNAMES, IWordList.CATEGORY_NAMES})));
				bruteforcer.addOption(new StringOption("passwords", "Passwords", "List of passwords to try, separated by space or comma", "Passwords", "", true));
				bruteforcer.addOption(new MultipleStringOption("passwords_wordlists", "Passwords Wordlists", "Wordlists to try as passwords", "Passwords", getAvailableWordLists(new String[] {IWordList.CATEGORY_PASSWORDS, IWordList.CATEGORY_NAMES})));
				bruteforcer.addOption(new BooleanOption("tryNullPassword", "Try null password", "Try null password", true));
				bruteforcer.addOption(new BooleanOption("tryUsernameAsPassword", "Try username as password", "Try username as password", true));
				bruteforcer.addOption(new BooleanOption("singleMode", "Single mode", "Stop after one credential is found", false));
				bruteforcer.addOption(new IntegerOption("maximumConnections", "Maximum connections", "Maximum number of simultaneous connections", 5));
//				bruteforcer.addOption(new StringOption("remoteName", "Remote Name", "Target host NetBIOS name", "*SMBSERVER", true));
//				bruteforcer.addOption(new StringOption("localName", "Local Name", "Local host NetBIOS name", "", true));
				answer.add(bruteforcer);
			
				bruteforcer = new ToolAction("Bruteforce NTLM Authentication", NTLMAuthBruteforcer.class.getName());
				bruteforcer.addFixedOption(new GenericOption(TCPSocketLocator.class, "target", "Target", "Target SMB service", locator));
//				bruteforcer.addOption(new IterableOption(UsernameAndPassword.class, "credentials", "Credentials", "List of credentials to try", null));
				bruteforcer.addOption(new StringOption("usernames", "Usernames", "List of usernames to try, separated by space or comma", "Usernames", "", true));
				bruteforcer.addOption(new MultipleStringOption("usernames_wordlists", "Usernames Wordlists", "Wordlists to try as usernames", "Usernames", getAvailableWordLists(new String[] {IWordList.CATEGORY_USERNAMES, IWordList.CATEGORY_NAMES})));
				bruteforcer.addOption(new StringOption("passwords", "Passwords", "List of passwords to try, separated by space or comma", "Passwords", "", true));
				bruteforcer.addOption(new MultipleStringOption("passwords_wordlists", "Passwords Wordlists", "Wordlists to try as passwords", "Passwords", getAvailableWordLists(new String[] {IWordList.CATEGORY_PASSWORDS, IWordList.CATEGORY_NAMES})));
				bruteforcer.addOption(new BooleanOption("tryNullPassword", "Try null password", "Try null password", true));
				bruteforcer.addOption(new BooleanOption("tryUsernameAsPassword", "Try username as password", "Try username as password", true));
				bruteforcer.addOption(new BooleanOption("singleMode", "Single mode", "Stop after one credential is found", false));
				bruteforcer.addOption(new IntegerOption("maximumConnections", "Maximum connections", "Maximum number of simultaneous connections", 1));
//				bruteforcer.addOption(new StringOption("remoteName", "Remote Name", "Target host NetBIOS name", "*SMBSERVER", true));
//				bruteforcer.addOption(new StringOption("localName", "Local Name", "Local host NetBIOS name", "", true));
				bruteforcer.addOption(new BooleanOption("checkLocal", "Check Local Accounts", "Check credentials against local accounts on the target host", true));
				bruteforcer.addOption(new BooleanOption("checkDomain", "Check Domain Accounts", "Check credentials against the host primary domain controller via the target host", true));
				answer.add(bruteforcer);
			}

			if (locator != null && serviceEntity.getServiceType().equals("Microsoft-DS")) {
				ToolAction bruteforcer = new ToolAction("Bruteforce NTLM Authentication", NTLMAuthBruteforcer.class.getName());
				bruteforcer.addFixedOption(new GenericOption(TCPSocketLocator.class, "target", "Target", "Target SMB service", locator));
//				bruteforcer.addOption(new IterableOption(UsernameAndPassword.class, "credentials", "Credentials", "List of credentials to try", null));
				bruteforcer.addOption(new StringOption("usernames", "Usernames", "List of usernames to try, separated by space or comma", "Usernames", "", true));
				bruteforcer.addOption(new MultipleStringOption("usernames_wordlists", "Usernames Wordlists", "Wordlists to try as usernames", "Usernames", getAvailableWordLists(new String[] {IWordList.CATEGORY_USERNAMES, IWordList.CATEGORY_NAMES})));
				bruteforcer.addOption(new StringOption("passwords", "Passwords", "List of passwords to try, separated by space or comma", "Passwords", "", true));
				bruteforcer.addOption(new MultipleStringOption("passwords_wordlists", "Passwords Wordlists", "Wordlists to try as passwords", "Passwords", getAvailableWordLists(new String[] {IWordList.CATEGORY_PASSWORDS, IWordList.CATEGORY_NAMES})));
				bruteforcer.addOption(new BooleanOption("tryNullPassword", "Try null password", "Try null password", true));
				bruteforcer.addOption(new BooleanOption("tryUsernameAsPassword", "Try username as password", "Try username as password", true));
				bruteforcer.addOption(new BooleanOption("singleMode", "Single mode", "Stop after one credential is found", false));
				bruteforcer.addOption(new IntegerOption("maximumConnections", "Maximum connections", "Maximum number of simultaneous connections", 1));
//				bruteforcer.addOption(new StringOption("remoteName", "Remote Name", "Target host NetBIOS name", "*SMBSERVER", true));
//				bruteforcer.addOption(new StringOption("localName", "Local Name", "Local host NetBIOS name", "", true));
				bruteforcer.addOption(new BooleanOption("checkLocal", "Check Local Accounts", "Check credentials against local accounts on the target host", true));
				bruteforcer.addOption(new BooleanOption("checkDomain", "Check Domain Accounts", "Check credentials against the host primary domain controller via the target host", true));
				answer.add(bruteforcer);
			}
		}

		return answer;
	}

	public List<IAction> getQuickActions(IShadowEntity entity) {
		return Collections.emptyList();
	}

	private String[] getAvailableWordLists(String[] categories) {
		List<String> names = new ArrayList<String>();
		for (IWordList wordlist: wordListManager.getWordListsByCategories(categories)) {
			names.add(wordlist.getName());
		}
		return names.toArray(new String[names.size()]);
	}

	protected void setWordListManager(IWordListManager wordListManager) {
		this.wordListManager = wordListManager;
	}
	
	protected void unsetWordListManager(IWordListManager wordListManager) {
		this.wordListManager = null;
	}
}
