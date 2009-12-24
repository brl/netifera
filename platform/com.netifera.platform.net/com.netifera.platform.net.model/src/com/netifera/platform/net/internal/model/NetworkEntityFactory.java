package com.netifera.platform.net.internal.model;

import java.util.Map;

import com.netifera.platform.api.model.AbstractEntity;
import com.netifera.platform.api.model.IEntity;
import com.netifera.platform.api.model.IModelService;
import com.netifera.platform.model.IWorkspaceEx;
import com.netifera.platform.net.model.ClientEntity;
import com.netifera.platform.net.model.ClientServiceConnectionEntity;
import com.netifera.platform.net.model.HostEntity;
import com.netifera.platform.net.model.INetworkEntityFactory;
import com.netifera.platform.net.model.InternetAddressEntity;
import com.netifera.platform.net.model.NetblockEntity;
import com.netifera.platform.net.model.PasswordEntity;
import com.netifera.platform.net.model.ServiceEntity;
import com.netifera.platform.net.model.UserEntity;
import com.netifera.platform.net.model.UsernameAndPasswordEntity;
import com.netifera.platform.util.PortSet;
import com.netifera.platform.util.addresses.inet.InternetAddress;
import com.netifera.platform.util.addresses.inet.InternetNetblock;
import com.netifera.platform.util.addresses.inet.InternetSocketAddress;
import com.netifera.platform.util.addresses.inet.TCPSocketAddress;
import com.netifera.platform.util.addresses.inet.UDPSocketAddress;

public class NetworkEntityFactory implements INetworkEntityFactory {

	private IModelService model;
	
	protected void setModelService(IModelService model) {
		this.model = model;
	}

	protected void unsetModelService(IModelService model) {
		this.model = null;
	}
	
	private IWorkspaceEx getWorkspace() {
		if(model.getCurrentWorkspace() == null) {
			throw new IllegalStateException("Cannot create entities because no workspace is currently open");
		}
		return (IWorkspaceEx) model.getCurrentWorkspace();
	}

	public synchronized InternetAddressEntity createAddress(long realm, long spaceId, InternetAddress address) {
		return InternetAddressEntity.create(getWorkspace(), realm, spaceId, address);
	}
	
	public synchronized NetblockEntity createNetblock(long realm, long spaceId, InternetNetblock netblock) {
		return NetblockEntity.create(getWorkspace(), realm, spaceId, netblock);
	}
	
	public synchronized void addOpenTCPPorts(long realm, long space, InternetAddress address, PortSet ports) {
		InternetAddressEntity addressEntity = createAddress(realm, space, address);
		addPorts(realm, space, addressEntity, ports, true, true);
	}

	public synchronized void addOpenUDPPorts(long realm, long space, InternetAddress address, PortSet ports) {
		InternetAddressEntity addressEntity = createAddress(realm, space, address);
		addPorts(realm, space, addressEntity, ports, true, false);
	}
	
	private void addPorts(long realm, long spaceId, InternetAddressEntity addressEntity, PortSet ports, boolean isOpen, boolean isTCP) {
		String attribute = isTCP ?
				(isOpen ? InternetAddressEntity.OPEN_TCP_PORTS_KEY : InternetAddressEntity.CLOSED_TCP_PORTS_KEY)
			: (isOpen ? InternetAddressEntity.OPEN_UDP_PORTS_KEY : InternetAddressEntity.CLOSED_UDP_PORTS_KEY);
		String existingPorts = addressEntity.getAttribute(attribute);
		String newPorts = null;
		
		if (existingPorts == null) {
			newPorts = ports.toString();
		} else {
			PortSet newPortSet = new PortSet(existingPorts);
			for (int port: ports)
				newPortSet.addPort(port);
			newPorts = newPortSet.toString();
		}
		addressEntity.setAttribute(attribute, newPorts);
		addressEntity.save();
		addressEntity.getHost().addToSpace(spaceId);
		addressEntity.getHost().update();
	}

	private boolean updateAttribute(String name, Map<String, String> info, AbstractEntity e) {
		if(!info.containsKey(name))
			return false;
		String value = info.get(name);
		if (value == null || value.equals(e.getAttribute(name)))
			return false;
		e.setAttribute(name, value);
		return true;
	}

	private boolean updateSystem(AbstractEntity entity, Map<String,String> info) {
		boolean changed = false;

		changed |= updateAttribute("os", info, entity);
		changed |= updateAttribute("distribution", info, entity);
		changed |= updateAttribute("arch", info, entity);

		return changed;
	}

	private boolean updateComment(AbstractEntity entity, String newComment) {
		String oldComment = entity.getAttribute("comment");
		if (oldComment != null) {
			if (!oldComment.contains(newComment)) {
				newComment = oldComment + "\n" + newComment;
				entity.setAttribute("comment", newComment);
				return true;
			}
		} else {
			entity.setAttribute("comment", newComment);
			return true;
		}
		return false;
	}
	
	public synchronized void setOperatingSystem(long realm, long spaceId, InternetAddress address, String os) {
		InternetAddressEntity addressEntity = createAddress(realm, spaceId, address);
		HostEntity hostEntity = addressEntity.getHost();
		if(os != null && !os.equals(hostEntity.getAttribute("os"))) {
			hostEntity.setAttribute("os", os);
			hostEntity.update();
		}
	}

	private ServiceEntity findService(long realm, InternetSocketAddress address) {
		return (ServiceEntity) getWorkspace().findByKey(ServiceEntity.createQueryKey(realm, address.getNetworkAddress(), address.getPort(), address.getProtocol()));
	}
	
	public synchronized ServiceEntity createService(long realm, long spaceId,
			InternetSocketAddress socketAddress, String serviceType, Map<String, String> info) {
		
		InternetAddressEntity addressEntity = createAddress(realm, spaceId, socketAddress.getNetworkAddress());
		
		if(info != null) {
			if (updateSystem(addressEntity.getHost(), info))
				addressEntity.getHost().update();
		}
		
		ServiceEntity entity = ServiceEntity.create(getWorkspace(), realm, spaceId, socketAddress, serviceType);

		PortSet ports = new PortSet();
		ports.addPort(socketAddress.getPort());
		if (socketAddress instanceof UDPSocketAddress) {
			addPorts(realm, spaceId, addressEntity, ports, true, false);
		} else if (socketAddress instanceof TCPSocketAddress) {
			addPorts(realm, spaceId, addressEntity, ports, true, true);			
		}
		
		boolean changed = false;
		
		if (info != null) {
			changed |= updateAttribute(ServiceEntity.BANNER_KEY, info, entity);
			changed |= updateAttribute(ServiceEntity.PRODUCT_KEY, info, entity);
			changed |= updateAttribute(ServiceEntity.VERSION_KEY, info, entity);
			changed |= updateSystem(entity, info);
			if (info.containsKey("comment"))
				changed |= updateComment(entity, info.get("comment"));
		}
		if(changed) entity.update();
		entity.addToSpace(spaceId);
		
		if (info != null) {
			if (info.containsKey("password")) {
				if (info.containsKey("username"))
					createUsernameAndPassword(realm, spaceId, socketAddress, info.get("username"), info.get("password"));
				else
					createPassword(realm, spaceId, socketAddress, info.get("password"));
			}
			if (info.containsKey("hostname")) {
				addressEntity.addName(info.get("hostname"));
				addressEntity.update();
//				if (address.getHost().getLabel()==null)
//					address.getHost().setLabel(info.get("hostname"));
				addressEntity.getHost().update();
			}
		}
		
		return entity;
	}

	public synchronized ClientEntity createClient(long realm, long spaceId,
			InternetAddress address, String serviceType, Map<String, String> info, InternetSocketAddress serviceAddress) {

		InternetAddressEntity addressEntity = createAddress(realm, spaceId, address);
		HostEntity hostEntity = addressEntity.getHost();

		if(info != null) {
			if (updateSystem(hostEntity, info))
				hostEntity.update();
		}

		ClientEntity entity = ClientEntity.create(getWorkspace(), realm, spaceId, address, serviceType, info != null ? info.get("product") : null);
		
		String identity = null;
		
		if (info != null) {
			boolean isChanged = false;
			isChanged |= updateAttribute(ServiceEntity.BANNER_KEY, info, entity);
			isChanged |= updateAttribute(ServiceEntity.PRODUCT_KEY, info, entity);
			isChanged |= updateAttribute(ServiceEntity.VERSION_KEY, info, entity);
			isChanged |= updateSystem(entity, info);
			if(isChanged)
				entity.update();
			
			if (info.containsKey("identity"))
				identity = info.get("identity");
			else
				identity = info.get("username"); // or null
			
			if (info.containsKey("password")) {
				if (info.containsKey("username"))
					createUsernameAndPassword(realm, spaceId, serviceAddress, info.get("username"), info.get("password"));
				else
					createPassword(realm, spaceId, serviceAddress, info.get("password"));
			}
		}

		if (serviceAddress != null) {
			ServiceEntity serviceEntity = findService(realm, serviceAddress);
			if (serviceEntity == null)
				System.err.println("ERROR: connection to unknown service: "+address+" -> "+serviceAddress);
			else
				createConnection(spaceId, entity, serviceEntity, identity);
		}
		
		return entity;
	}

	public ClientServiceConnectionEntity createConnection(long spaceId, ClientEntity client, ServiceEntity service, String identity) {
		long realm = client.getRealmId();
		long clientId = client.getId();
		long serviceId = service.getId();
		
		ClientServiceConnectionEntity answer = (ClientServiceConnectionEntity) getWorkspace().findByKey(ClientServiceConnectionEntity.createQueryKey(realm, clientId, serviceId, identity));
		if(answer != null) {
			answer.addToSpace(spaceId);
			return answer;
		}
	
		answer = new ClientServiceConnectionEntity(getWorkspace(), client, service, identity);
		answer.save();
		answer.addToSpace(spaceId);
		return answer;
	}
	
	public synchronized UserEntity createUser(long realm, long spaceId, InternetAddress address, String username) {
		return UserEntity.create(getWorkspace(), realm, spaceId, address, username);
	}
	
	public synchronized PasswordEntity createPassword(long realm, long spaceId, IEntity authenticable, String password) {
		return PasswordEntity.create(getWorkspace(), realm, spaceId, authenticable, password);
	}

	public synchronized PasswordEntity createPassword(long realm, long spaceId, InternetSocketAddress serviceAddress, String password) {
		return createPassword(realm, spaceId, findService(realm, serviceAddress), password);
	}

	public synchronized UsernameAndPasswordEntity createUsernameAndPassword(long realm, long spaceId, IEntity authenticable, String username, String password) {
		return UsernameAndPasswordEntity.create(getWorkspace(), realm, spaceId, authenticable, username, password);
	}

	public synchronized UsernameAndPasswordEntity createUsernameAndPassword(long realm, long spaceId, InternetSocketAddress serviceAddress, String username, String password) {
		return createUsernameAndPassword(realm, spaceId, findService(realm, serviceAddress), username, password);
	}
}
