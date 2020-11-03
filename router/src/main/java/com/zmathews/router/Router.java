package com.zmathews.router;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
// import java.util.concurrent.Future;
// import java.util.concurrent.TimeUnit;

import javax.xml.crypto.Data;

public class Router {
	private static int id_;
	AsynchronousServerSocketChannel marketServer;
	AsynchronousServerSocketChannel brokerServer;

	public Router() {
		try {
			id_ = 100000;
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
			DataInstance brokerData = new DataInstance();
			brokerData.id = id_;
			brokerData.server = brokerServer;
			brokerServer.accept(brokerData, new CompletionHandler<AsynchronousSocketChannel, DataInstance>(){
				public void completed(AsynchronousSocketChannel ch, DataInstance dataInstance) {
					try {
						SocketAddress cliAddress = ch.getRemoteAddress();
						System.out.format("Accepted connection from %s%n", cliAddress);
						dataInstance.server.accept(dataInstance, this);
						DataInstance newInstance = new DataInstance();
						newInstance.id = Router.incId();
						newInstance.server = dataInstance.server;
						newInstance.client = ch;
						newInstance.cliAddress = cliAddress;
						String mString = "ID: " + Integer.toString(newInstance.id);
					}
					catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
				public void failed(Throwable e, DataInstance dataInstance) {
					System.out.println("Broker failed to connect");
					e.printStackTrace();
				}
			});
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
		SocketAddress cliAddress;
		boolean isRead;
		// BrokerReadWriteHandler brokerRwHandler;
		// MarketReadWriteHandler marketRwHandler;
	}

	public static int incId() {
		id_++;
		return (id_);
	}
}