package com.zmathews.router;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;

public class Router {
	private static int id_;
	private AsynchronousServerSocketChannel marketServer;
	private AsynchronousServerSocketChannel brokerServer;
	private Map<Integer, DataInstance> markets;
	private Map<Integer, DataInstance> brokers;

	public Router() {
		markets = new HashMap<Integer, DataInstance>();
		brokers = new HashMap<Integer, DataInstance>();
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
						newInstance.buffer = ByteBuffer.allocate(2048);
						newInstance.isRead = true;
						newInstance.cliAddress = cliAddress;
						Charset cs = Charset.forName("UTF-8");
						String mString = "ID: " + Integer.toString(newInstance.id);
						newInstance.buffer.clear();
						byte[] bytes = mString.getBytes(cs);
						newInstance.buffer.put(bytes);
						newInstance.buffer.flip();
						brokers.put(newInstance.id, newInstance);
						newInstance.client.write(newInstance.buffer);
						Router.clearBuffer(newInstance);
						newInstance.client.read(newInstance.buffer, newInstance, new CompletionHandler<Integer, DataInstance>() {
							public void completed(Integer result, DataInstance dataInstance) {
								if (result == -1) {
									try {
										dataInstance.client.close();
										System.out.format("\u001b[31mStopped listening to the Broker %s ID %s%n\u001b[0m", dataInstance.cliAddress, dataInstance.id);
									}
									catch (IOException ex) {
										ex.printStackTrace();
									}
									return ;
								}
								if (dataInstance.isRead) {
									dataInstance.buffer.flip();
									int limits = dataInstance.buffer.limit();
									byte bytes[] = new byte[limits];
									dataInstance.buffer.get(bytes, 0, limits);
									Charset cs = Charset.forName("UTF-8");
									String msg = new String(bytes, cs);
									System.out.format("\u001b[33mBroker(%s) ID(%s): %s%n\u001b[0m", dataInstance.cliAddress, dataInstance.id, msg);
									String[] shards = msg.split("\\|");
									if (shards.length == 7) {
										int marketID = Integer.parseInt(shards[0]);
										int price = Integer.parseInt(shards[3]);
										int checksum = Integer.parseInt(shards[6]);
										int msglen = shards[0].length() + shards[1].length() + shards[2].length() + shards[3].length() + shards[4].length() + shards[5].length() + 5;
										if (checksum - price == msglen) {
											if (writeToMarket(msg, marketID) == 0) {
												String newMsg = "Market: " + shards[0] + " Does not exist";
												writeToBroker(newMsg, Integer.parseInt(shards[5]));
												return ;
											}
										}
										dataInstance.buffer.clear();
										byte[] data = msg.getBytes(cs);
										dataInstance.buffer.put(data);
										dataInstance.buffer.flip();
										dataInstance.isRead = false; // It is a write
										Router.clearBuffer(dataInstance);
									}
								}
								else {
									dataInstance.isRead = true;
									dataInstance.buffer.clear();
									dataInstance.client.read(dataInstance.buffer, dataInstance, this);
								}
							}

							public void failed(Throwable e, DataInstance dataInstance) {
								e.printStackTrace();
							}
						});
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
		try {
			DataInstance marketData = new DataInstance();
			marketData.id = id_;
			marketData.server = marketServer;
			marketServer.accept(marketData, new CompletionHandler<AsynchronousSocketChannel, DataInstance>(){
				public void completed(AsynchronousSocketChannel client, DataInstance dataInstance) {
					try {
						SocketAddress clientAddr = client.getRemoteAddress();
						System.out.format("\u001b[32mAccepted a connection from %s%n\u001b[0m", clientAddr);
						dataInstance.server.accept(dataInstance, this);
						DataInstance newInstance = new DataInstance();
						newInstance.id = Router.incId();
						newInstance.server = dataInstance.server;
						newInstance.client = client;
						newInstance.buffer = ByteBuffer.allocate(2048);
						newInstance.isRead = false;
						newInstance.cliAddress = clientAddr;
						Charset cs = Charset.forName("UTF-8");
						String msg = "ID|" + Integer.toString(newInstance.id);
						newInstance.buffer.clear();
						byte[] data = msg.getBytes(cs);
						newInstance.buffer.put(data);
						newInstance.buffer.flip();
						markets.put(newInstance.id, newInstance);
						newInstance.client.write(newInstance.buffer);
						Router.clearBuffer(newInstance);
						newInstance.client.read(newInstance.buffer, newInstance, new CompletionHandler<Integer, DataInstance>() {
							public void completed(Integer result, DataInstance dataInstance) {
								if (result == -1) {
									try {
										dataInstance.client.close();
										System.out.format("\u001b[31mStopped listening to the Market %s ID %s%n\u001b[0m", dataInstance.cliAddress, dataInstance.id);
									}
									catch (IOException ex) {
										ex.printStackTrace();
									}
									return ;
								}
								if (dataInstance.isRead) {
									dataInstance.buffer.flip();
									int limits = dataInstance.buffer.limit();
									byte bytes[] = new byte[limits];
									dataInstance.buffer.get(bytes, 0, limits);
									Charset cs = Charset.forName("UTF-8");
									String msg = new String(bytes, cs);
									System.out.format("\u001b[36mMarket(%s) ID(%s): 10000|%s%n\u001b[0m", dataInstance.cliAddress, dataInstance.id, msg);
									String[] shards = msg.split("\\|");
									if (shards.length == 4) {
										int brokerID = Integer.parseInt(shards[0]);
										int checksum = Integer.parseInt(shards[3]);
										int msglen = shards[0].length() + shards[1].length() + shards[2].length() + 2;
										if (checksum - 22 == msglen) {
											if (writeToBroker(msg, brokerID) == 0) {
												String newMsg = "Broker: " + shards[0] + " Does not exist";
												writeToMarket(newMsg, Integer.parseInt(shards[1]));
											}
										}
										dataInstance.buffer.clear();
										byte[] data = msg.getBytes(cs);
										dataInstance.buffer.put(data);
										dataInstance.buffer.flip();
										dataInstance.isRead = false;
										Router.clearBuffer(dataInstance);
									}
								}
								else {
									dataInstance.isRead = true;
									dataInstance.buffer.clear();
									dataInstance.client.read(dataInstance.buffer, dataInstance, this);
								}
							}

							public void failed(Throwable e, DataInstance dataInstance) {
								e.printStackTrace();
							}
						});
					}
					catch(IOException e) {
						e.printStackTrace();
					}
				}
				public void failed(Throwable e, DataInstance dataInstance)
				{
					System.out.println("Failed to accept connection!");
					e.printStackTrace();
				}
			});
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private class DataInstance {
		int id;
		AsynchronousServerSocketChannel server;
		AsynchronousSocketChannel client;
		ByteBuffer buffer;
		SocketAddress cliAddress;
		boolean isRead;
	}

	public static int incId() {
		id_++;
		return (id_);
	}

	public static void clearBuffer(DataInstance dataInstance) {
		String msg = "";
		dataInstance.buffer.clear();
		Charset cs = Charset.forName("UTF-8");
		byte[] data = msg.getBytes(cs);
		dataInstance.buffer.put(data);
	}

	public int writeToMarket(String msg, int marketID) {
		try {
			DataInstance market = markets.get(marketID);
			Charset cs = Charset.forName("UTF-8");
			byte[] data = msg.getBytes(cs);
			market.buffer.clear();
			market.buffer.put(data);
			market.buffer.flip();
			market.client.write(market.buffer);
			clearBuffer(market);
			market.isRead = true;
			try {
				market.client.read(market.buffer, market, new CompletionHandler<Integer,DataInstance>(){
					public void completed(Integer result, DataInstance dataInstance) {
						if (result == -1) {
							try {
								dataInstance.client.close();
								System.out.format("\u001b[31mStopped listening to the Market %s ID %s%n\u001b[0m", dataInstance.cliAddress, dataInstance.id);
							}
							catch (IOException ex) {
								ex.printStackTrace();
							}
							return ;
						}
						if (dataInstance.isRead) {
							dataInstance.buffer.flip();
							int limits = dataInstance.buffer.limit();
							byte bytes[] = new byte[limits];
							dataInstance.buffer.get(bytes, 0, limits);
							Charset cs = Charset.forName("UTF-8");
							String msg = new String(bytes, cs);
							System.out.format("\u001b[36mMarket(%s) ID(%s): 10000|%s%n\u001b[0m", dataInstance.cliAddress, dataInstance.id, msg);
							String[] shards = msg.split("\\|");
							if (shards.length == 4) {
								int brokerID = Integer.parseInt(shards[0]);
								int checksum = Integer.parseInt(shards[3]);
								int msglen = shards[0].length() + shards[1].length() + shards[2].length() + 2;
								if (checksum - 22 == msglen) {
									if (writeToBroker(msg, brokerID) == 0) {
										String newMsg = "Broker: " + shards[0] + " Does not exist";
										writeToMarket(newMsg, Integer.parseInt(shards[1]));
									}
								}
								dataInstance.buffer.clear();
								byte[] data = msg.getBytes(cs);
								dataInstance.buffer.put(data);
								dataInstance.buffer.flip();
								dataInstance.isRead = false;
								Router.clearBuffer(dataInstance);
							}
						}
						else {
							dataInstance.isRead = true;
							dataInstance.buffer.clear();
							dataInstance.client.read(dataInstance.buffer, dataInstance, this);
						}
					}

					public void failed(Throwable e, DataInstance dataInstance) {
						e.printStackTrace();
					}
				});
			}
			catch(ReadPendingException e) {
				System.out.println(e.getMessage());
			}
		}
		catch(NullPointerException e) {
			return(0);
		}
		return(1);
	}

	public int writeToBroker(String msg, int brokerID) {
		try {
			DataInstance broker = brokers.get(brokerID);
			Charset cs = Charset.forName("UTF-8");
			byte[] data = msg.getBytes(cs);
			broker.buffer.clear();
			broker.buffer.put(data);
			broker.buffer.flip();
			broker.client.write(broker.buffer);
			clearBuffer(broker);
			broker.isRead = true;
			try {
				broker.client.read(broker.buffer, broker, new CompletionHandler<Integer, DataInstance>(){
					public void completed(Integer result, DataInstance dataInstance) {
						if (result == -1) {
							try {
								dataInstance.client.close();
								System.out.format("\u001b[31mStopped listening to the Broker %s ID %s%n\u001b[0m", dataInstance.cliAddress, dataInstance.id);
							}
							catch (IOException ex) {
								ex.printStackTrace();
							}
							return ;
						}
						if (dataInstance.isRead) {
							dataInstance.buffer.flip();
							int limits = dataInstance.buffer.limit();
							byte bytes[] = new byte[limits];
							dataInstance.buffer.get(bytes, 0, limits);
							Charset cs = Charset.forName("UTF-8");
							String msg = new String(bytes, cs);
							System.out.format("\u001b[33mBroker(%s) ID(%s): %s%n\u001b[0m", dataInstance.cliAddress, dataInstance.id, msg);
							String[] shards = msg.split("\\|");
							if (shards.length == 7) {
								int marketID = Integer.parseInt(shards[0]);
								int price = Integer.parseInt(shards[3]);
								int checksum = Integer.parseInt(shards[6]);
								int msglen = shards[0].length() + shards[1].length() + shards[2].length() + shards[3].length() + shards[4].length() + shards[5].length() + 5;
								if (checksum - price == msglen) {
									if (writeToMarket(msg, marketID) == 0) {
										String newMsg = "Market: " + shards[0] + " Does not exist";
										writeToBroker(newMsg, Integer.parseInt(shards[5]));
										return ;
									}
								}
								dataInstance.buffer.clear();
								byte[] data = msg.getBytes(cs);
								dataInstance.buffer.put(data);
								dataInstance.buffer.flip();
								dataInstance.isRead = false; // It is a write
								Router.clearBuffer(dataInstance);
							}
						}
						else {
							dataInstance.isRead = true;
							dataInstance.buffer.clear();
							dataInstance.client.read(dataInstance.buffer, dataInstance, this);
						}
					}

					public void failed(Throwable e, DataInstance dataInstance) {
						e.printStackTrace();
					}
				});
			}
			catch(ReadPendingException e) {
				System.out.println("readPending");
			}
		}
		catch(NullPointerException e) {
			return(0);
		}
		return(1);
	}
}