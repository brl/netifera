package com.netifera.platform.net.ui.routes;


import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;

public interface IIP2ASService {
	AS getAS(InternetAddress address);
	AS getAS(InternetNetblock netblock);
}
