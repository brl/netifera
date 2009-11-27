package com.netifera.platform.net.ssh.internal.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.netifera.platform.api.model.IShadowEntity;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.net.services.ssh.SSH;
import com.netifera.platform.net.ssh.tools.SSHAuthBruteforcer;
import com.netifera.platform.net.ssh.tools.SSHProbeDeployer;
import com.netifera.platform.net.wordlists.IWordList;
import com.netifera.platform.net.wordlists.IWordListManager;
import com.netifera.platform.probebuild.api.IProbeBuilderService;
import com.netifera.platform.tools.options.BooleanOption;
import com.netifera.platform.tools.options.GenericOption;
import com.netifera.platform.tools.options.IntegerOption;
import com.netifera.platform.tools.options.MultipleStringOption;
import com.netifera.platform.tools.options.StringOption;
import com.netifera.platform.ui.actions.ToolAction;
import com.netifera.platform.ui.api.actions.IHoverActionProvider;
import com.netifera.platform.util.locators.TCPSocketLocator;

public class HoverActionProvider implements IHoverActionProvider {
	private IProbeBuilderService probeBuilder;
	private IWordListManager wordListManager;

	public List<IAction> getActions(Object o) {
		if (!(o instanceof IShadowEntity)) return Collections.emptyList();
		IShadowEntity entity = (IShadowEntity) o;
		
		List<IAction> answer = new ArrayList<IAction>();
		
		SSH ssh = (SSH) entity.getAdapter(SSH.class);
		if (ssh != null) {
			ToolAction bruteforcer = new ToolAction("Bruteforce Authentication", SSHAuthBruteforcer.class.getName());
			bruteforcer.addFixedOption(new GenericOption(TCPSocketLocator.class, "target", "Target", "Target SSH service", ssh.getLocator()));
//			bruteforcer.addOption(new IterableOption(UsernameAndPassword.class, "credentials", "Credentials", "List of credentials to try", null));
			bruteforcer.addOption(new StringOption("usernames", "Usernames", "List of usernames to try, separated by space or comma", "Usernames", "", true));
			bruteforcer.addOption(new MultipleStringOption("usernames_wordlists", "Usernames Wordlists", "Wordlists to try as usernames", "Usernames", getAvailableWordLists(new String[] {IWordList.CATEGORY_USERNAMES, IWordList.CATEGORY_NAMES})));
			bruteforcer.addOption(new StringOption("passwords", "Passwords", "List of passwords to try, separated by space or comma", "Passwords", "", true));
			bruteforcer.addOption(new MultipleStringOption("passwords_wordlists", "Passwords Wordlists", "Wordlists to try as passwords", "Passwords", getAvailableWordLists(new String[] {IWordList.CATEGORY_PASSWORDS, IWordList.CATEGORY_NAMES})));
			bruteforcer.addOption(new BooleanOption("tryNullPassword", "Try null password", "Try null password", true));
			bruteforcer.addOption(new BooleanOption("tryUsernameAsPassword", "Try username as password", "Try username as password", true));
			bruteforcer.addOption(new BooleanOption("singleMode", "Single mode", "Stop after one credential is found", false));
			bruteforcer.addOption(new IntegerOption("maximumConnections", "Maximum connections", "Maximum number of simultaneous connections", 5));
			answer.add(bruteforcer);
			
			ToolAction deployer = new ToolAction("Deploy Probe", SSHProbeDeployer.class.getName());
			deployer.addFixedOption(new GenericOption(TCPSocketLocator.class, "target", "Target", "Target SSH service", ssh.getLocator()));
			deployer.addOption(new StringOption("username", "Username", "Username to login to SSH", "root", true));
			deployer.addOption(new StringOption("password", "Password", "Password to login to SSH", "", true));
			deployer.addOption(new StringOption("probeConfig", "Probe Configuration", "Probe configuration to be deployed", probeBuilder.listProbeConfigurations()));
			deployer.addOption(new StringOption("probeName", "Probe Name", "Name to use as label of the probe that will be deployed", "", true));
			answer.add(deployer);
		}

		if (entity instanceof UsernameAndPasswordEntity) {
			UsernameAndPasswordEntity credentialEntity = (UsernameAndPasswordEntity) entity;
			
			ssh = (SSH) credentialEntity.getAuthenticable().getAdapter(SSH.class);
			if (ssh != null) {
				ToolAction deployer = new ToolAction("Deploy Probe", SSHProbeDeployer.class.getName());
				deployer.addFixedOption(new GenericOption(TCPSocketLocator.class, "target", "Target", "Target SSH service", ssh.getLocator()));
				deployer.addFixedOption(new StringOption("username", "Username", "Username to login to SSH", credentialEntity.getUsername(), true));
				deployer.addFixedOption(new StringOption("password", "Password", "Password to login to SSH", credentialEntity.getPassword(), true));
				deployer.addOption(new StringOption("probeConfig", "Probe Configuration", "Probe configuration to be deployed", probeBuilder.listProbeConfigurations()));
				deployer.addOption(new StringOption("probeName", "Probe Name", "Name to use as label of the probe that will be deployed", "", true));
				answer.add(deployer);
			}
		}
		return answer;
	}

	public List<IAction> getQuickActions(Object o) {
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

	protected void setProbeBuilder(IProbeBuilderService probeBuilder) {
		this.probeBuilder = probeBuilder;
	}
	
	protected void unsetProbeBuilder(IProbeBuilderService probeBuilder) {
		this.probeBuilder = null;
	}
}
