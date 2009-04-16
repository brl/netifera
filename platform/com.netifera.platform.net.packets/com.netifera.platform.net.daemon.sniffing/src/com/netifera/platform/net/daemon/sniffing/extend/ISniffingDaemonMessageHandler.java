package com.netifera.platform.net.daemon.sniffing.extend;

import com.netifera.platform.api.dispatcher.IMessenger;
import com.netifera.platform.api.dispatcher.MessengerException;
import com.netifera.platform.net.internal.daemon.probe.CancelCaptureFile;
import com.netifera.platform.net.internal.daemon.probe.CaptureFileValid;
import com.netifera.platform.net.internal.daemon.probe.RequestInterfaceInformation;
import com.netifera.platform.net.internal.daemon.probe.RequestModuleInformation;
import com.netifera.platform.net.internal.daemon.probe.RunCaptureFile;
import com.netifera.platform.net.internal.daemon.probe.SetInterfaceEnableState;
import com.netifera.platform.net.internal.daemon.probe.SetModuleEnableState;
import com.netifera.platform.net.internal.daemon.probe.SniffingDaemonStatus;
import com.netifera.platform.net.internal.daemon.probe.StartSniffingDaemon;
import com.netifera.platform.net.internal.daemon.probe.StopSniffingDaemon;

public interface ISniffingDaemonMessageHandler {
	void requestInterfaceInformation(IMessenger messenger, RequestInterfaceInformation msg) throws MessengerException;
	void requestModuleInformation(IMessenger messenger, RequestModuleInformation msg) throws MessengerException;
	void setInterfaceEnableState(IMessenger messenger, SetInterfaceEnableState msg) throws MessengerException;
	void setModuleEnableState(IMessenger messenger, SetModuleEnableState msg) throws MessengerException;
	void startSniffingDaemon(IMessenger messenger, StartSniffingDaemon msg) throws MessengerException;
	void stopSniffingDaemon(IMessenger messenger, StopSniffingDaemon msg) throws MessengerException;
	void captureFileValid(IMessenger messenger, CaptureFileValid msg) throws MessengerException;
	void sniffingDaemonStatus(IMessenger messenger, SniffingDaemonStatus msg) throws MessengerException;
	void runCaptureFile(IMessenger messenger, RunCaptureFile msg) throws MessengerException;
	void cancelCaptureFile(IMessenger messenger, CancelCaptureFile msg) throws MessengerException;

}
