package com.zmathews.router;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class Router {
	public Router() {
		try {
			InetAddress host = InetAddress.getByName("localhost");
			Selector selector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}