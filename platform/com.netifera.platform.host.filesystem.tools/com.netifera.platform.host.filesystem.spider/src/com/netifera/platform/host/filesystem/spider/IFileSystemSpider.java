package com.netifera.platform.host.filesystem.spider;



public interface IFileSystemSpider {
//	<A> void fetch(String path, A attachement, CompletionHandler<IFileContent,? super A> handler);
	void fetch(String path);
}
