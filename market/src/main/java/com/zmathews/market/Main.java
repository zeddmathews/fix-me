package com.zmathews.market;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class Main
{
	int readCount = 0;
	public static void main( String[] args ) throws Exception {
		System.out.println("\u001b[35m----MARKET----\u001b[0m");
		try {
			AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
			SocketAddress serverAddr = new InetSocketAddress("localhost", 5001);
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
					System.out.println("\u001b[33mReceived Message: \u001b[0m" + msg);
					String returnMsg = readWrite.messageHandler(msg, dataInstance);
					if (!returnMsg.equalsIgnoreCase("Id updated")) {
						readWrite.writeToSoc(dataInstance, returnMsg);
					}
					System.out.println();
				}
				if (dataInstance.mainThread.isInterrupted()) {
					return;
				}
			}
		}
		catch(Exception e) {
			System.out.println("\u001b[31mRouter Not Available\u001b[0m");
		}
	}
}


class DataInstance {
	int id;
	AsynchronousSocketChannel channel;
	ByteBuffer buffer;
	Thread mainThread;
	boolean isRead;
}