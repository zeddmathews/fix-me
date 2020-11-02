package com.zmathews.router;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
// import java.util.concurrent.Future;
// import java.util.concurrent.TimeUnit;

public class Router {
	AsynchronousServerSocketChannel marketServer;
	AsynchronousServerSocketChannel brokerServer;

	public Router() {
		try {
			marketServer = AsynchronousServerSocketChannel.open();
			InetSocketAddress mAddress = new InetSocketAddress("localhost", 5001);
			marketServer.bind(mAddress);
			System.out.printf("Listening for market connections at: %s%n", mAddress);

			this.initMarket();

			brokerServer = AsynchronousServerSocketChannel.open();
			InetSocketAddress bAddress = new InetSocketAddress("localhost", 5000);
			brokerServer.bind(bAddress);
			System.out.printf("Listening for broker connections at: %s%n", bAddress);

			this.initBroker();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private void initBroker() {
		try {
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private void initMarket() {

	}

	private class DataInstance {
		int id;
		AsynchronousServerSocketChannel server;
		AsynchronousSocketChannel client;
		ByteBuffer buffer;
		SocketAddress clientAddr;
		boolean isRead;
		// BrokerReadWriteHandler brokerRwHandler;
		// MarketReadWriteHandler marketRwHandler;
	}
}