package com.netifera.platform.host.filesystem;

import java.net.URI;

import com.netifera.platform.api.probe.IProbe;

public interface IFileSystemFactory {
	@Deprecated
	IFileSystem createForProbe(IProbe probe);
	
	IFileSystem create(URI url);
	IFileSystem create(URI url, IProbe probe);
}
