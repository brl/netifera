package com.netifera.platform.net.http.internal.spider.daemon;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.http.internal.spider.daemon.remote.FetchURL;
import com.netifera.platform.net.http.internal.spider.daemon.remote.GetAvailableModules;
import com.netifera.platform.net.http.internal.spider.daemon.remote.GetSpiderConfiguration;
import com.netifera.platform.net.http.internal.spider.daemon.remote.GetSpiderStatus;
import com.netifera.platform.net.http.internal.spider.daemon.remote.SetSpiderConfiguration;
import com.netifera.platform.net.http.internal.spider.daemon.remote.StartSpider;
import com.netifera.platform.net.http.internal.spider.daemon.remote.StopSpider;
import com.netifera.platform.net.http.internal.spider.daemon.remote.VisitURL;

public interface IWebSpiderMessageHandler {
	void getAvailableModules(IMessenger messenger, GetAvailableModules msg) throws MessengerException;
	void getSpiderConfiguration(IMessenger messenger, GetSpiderConfiguration msg) throws MessengerException;
	void setSpiderConfiguration(IMessenger messenger, SetSpiderConfiguration msg) throws MessengerException;
	void startSpider(IMessenger messenger, StartSpider msg) throws MessengerException;
	void stopSpider(IMessenger messenger, StopSpider msg) throws MessengerException;
	void getSpiderStatus(IMessenger messenger, GetSpiderStatus msg) throws MessengerException;
	void visitURL(IMessenger messenger, VisitURL msg) throws MessengerException;
	void fetchURL(IMessenger messenger, FetchURL msg) throws MessengerException;
}
