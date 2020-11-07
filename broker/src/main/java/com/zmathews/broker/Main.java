package com.zmathews.broker;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class Main {
	public static void main( String[] args ) throws Exception {
		System.out.println("\u001b[35m----BROKER----\u001b[0m");
		try {
			AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
			SocketAddress serverAddr = new InetSocketAddress("localhost", 5000);
			Future<Void> result = channel.connect(serverAddr);
			result.get();
			System.out.println("\u001b[32mConnected to Router\u001b[0m");
			DataInstance dataInstance = new DataInstance();
			dataInstance.channel = channel;
			dataInstance.buffer = ByteBuffer.allocate(2048);
			dataInstance.isRead = true;
			dataInstance.mainThread = Thread.currentThread();
			ReadWrite readWrite = new ReadWrite();
			String msg;
			while(true) {
				msg = readWrite.readFromSoc(dataInstance);
				if(msg.length() > 0) {
					readWrite.handleMsg(msg, dataInstance);
					System.out.println();
				}
				readWrite.writeToSoc(dataInstance);
				if (dataInstance.mainThread.isInterrupted())
					return;
			}
		}
		catch(Exception e) {
			System.out.println("\u001b[31mRouter Not Available\u001b[0m");
		}
	}
}

class DataInstance
{
	int id;
	AsynchronousSocketChannel channel;
	ByteBuffer buffer;
	Thread mainThread;
	boolean isRead;
}
