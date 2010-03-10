package com.netifera.platform.net.model;

import java.util.Map;

import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.util.PortSet;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;

public interface INetworkEntityFactory {

	InternetAddressEntity createAddress(long realm, long space, InternetAddress address);
	NetblockEntity createNetblock(long realm, long space, InternetNetblock netblock);

	void addOpenTCPPorts(long realm, long space, InternetAddress address, PortSet ports);
	void addOpenUDPPorts(long realm, long space, InternetAddress address, PortSet ports);

	ServiceEntity createService(long realm, long space, InternetSocketAddress address, String serviceType, Map<String,String> info);
	ClientEntity createClient(long realm, long space, InternetAddress address, String serviceType, Map<String,String> info, InternetSocketAddress serviceAddress);
	ClientServiceConnectionEntity createConnection(long space, ClientEntity client, ServiceEntity service, String identity);

	void setOperatingSystem(long realm, long space, InternetAddress address, String os);

	UserEntity createUser(long realm, long space, InternetAddress address, String username);

	PasswordEntity createPassword(long realm, long space, IEntity authenticable, String password);
	PasswordEntity createPassword(long realm, long space, InternetSocketAddress service, String password);
	UsernameAndPasswordEntity createUsernameAndPassword(long realm, long space, IEntity authenticable, String username, String password);
	UsernameAndPasswordEntity createUsernameAndPassword(long realm, long space, InternetSocketAddress serviceAddress, String username, String password);
}
