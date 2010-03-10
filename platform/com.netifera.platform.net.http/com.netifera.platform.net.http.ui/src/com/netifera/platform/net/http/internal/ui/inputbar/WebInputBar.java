package com.netifera.platform.net.http.internal.ui.inputbar;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;

import com.netifera.platform.api.model.ISpace;
import com.netifera.platform.net.http.internal.ui.Activator;
import com.netifera.platform.net.http.internal.ui.actions.WebSpiderActionManager;
import com.netifera.platform.net.http.spider.daemon.IWebSpiderDaemon;
import com.netifera.platform.ui.spaces.inputbar.AbstractInputBar;
import com.netifera.platform.util.patternmatching.HttpUrlMatcher;


public class WebInputBar extends AbstractInputBar {

	private final WebSpiderActionManager manager;
	
	public WebInputBar(WebSpiderActionManager manager) {
		super("web.input.bar", Activator.getDefault().getLogManager().getLogger("Web Input Bar"));
		this.manager = manager;
	}

	protected String getDefaultToolTipText() {
		return "Enter URL";
	}
	
	protected String getDefaultGreyedText() {
		return "Enter URL";
	}

	protected List<IAction> getInputActions(String content) {
		final HttpUrlMatcher matcher = new HttpUrlMatcher(content);
		if (!matcher.matches())
			return Collections.emptyList();
		
		List<IAction> answer = new ArrayList<IAction>();

		try {
			final URI url = new URI(content);
			answer.add(new Action("Visit "+url) {
				public void run() {
					final ISpace space = Activator.getDefault().getCurrentSpace();
					if(space == null) {
						return;
					}
					final IWebSpiderDaemon daemon = Activator.getDefault().getWebSpiderDaemon();
					if(daemon == null) {
						manager.setFailed("No web spider service found");
						return;
					}
					
					new Thread(new Runnable() {
						public void run() {
							if (!daemon.isRunning())
								daemon.start(space.getId());
							daemon.visit(url);
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									manager.setState();
								}
							});
						}
					}).start();
				}
			});
		} catch (URISyntaxException e) {
		}
		
		return answer;
	}
}
