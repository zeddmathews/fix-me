package com.zmathews.broker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class ReadWrite {
	public String red = (char)27 + "[31m";
	public String blue = (char)27 + "[34m";
	public String defaultCl = (char)27 + "[39;49m";

	public String	readFromSoc(DataInstance dataInstance) throws Exception
	{
		dataInstance.buffer.clear();
		if (dataInstance.channel.read(dataInstance.buffer).get() == -1)
		{
			System.out.format("\u001b[31mRouter unavailable, Shuting Down ...%n\u001b[0m");
			dataInstance.mainThread.interrupt();
			return("");
		}
		dataInstance.buffer.flip();
		Charset cs = Charset.forName("UTF-8");
		int limits = dataInstance.buffer.limit();
		byte bytes[] = new byte[limits];
		dataInstance.buffer.get(bytes, 0, limits);
		String msg = new String(bytes, cs);
		clearBuffer(dataInstance);
		return(msg);
	}

	private void clearBuffer(DataInstance dataInstance)
	{
		String msg = "";
		dataInstance.buffer.clear();
		Charset cs = Charset.forName("UTF-8");
		byte[] data = msg.getBytes(cs);
		dataInstance.buffer.put(data);
	}

	public void	writeToSoc(DataInstance dataInstance) throws Exception
	{
		String msg = getTextFromUser(dataInstance.id);
		dataInstance.buffer.clear();
		Charset cs = Charset.forName("UTF-8");
		byte[] data = msg.getBytes(cs);
		dataInstance.buffer.put(data);
		dataInstance.buffer.flip();
		if (dataInstance.channel.write(dataInstance.buffer).get() == -1)
		{
			System.out.format("\u001b[31mRouter unavailable, Shuting Down ...%n\u001b[0m");
			dataInstance.mainThread.interrupt();
			return;
		}
	}

	public String handleMsg(String msg, DataInstance dataInstance)
	{
		String[] shards = msg.split("\\|");
		if (shards[0].equalsIgnoreCase("ID"))
		{
			dataInstance.id = Integer.parseInt(shards[1]);
			System.out.println("ID: " + shards[1]);
			return("Id updated");
		}
		else
		{
			if (shards.length == 4)
			{
				System.out.println("\u001b[33mMarket " + shards[1] + ": \u001b[0m" + shards[2] + defaultCl);
			}
			else if (shards.length == 1)
			{
				System.out.println(red + shards[0] + defaultCl);
			}
			return(msg);
		}
	}

	private String getTextFromUser(int id) throws Exception
	{
		String marketId = this.getMarketIdFromUser();
		String symbol = this.getSymbolFromUser();
		String transaction = this.getTransactionFromUser();
		String price = this.getPriceFromUser();
		String qty = this.getQtyFromUser();
		String msg = marketId + "|" + symbol + "|" + transaction + "|" + price + "|" + qty + "|" + Integer.toString(id);
		int checksum = msg.length() + Integer.parseInt(price);
		msg = msg + "|" + Integer.toString(checksum);
		return msg;
	}

	private String getMarketIdFromUser() throws Exception
	{
		System.out.println("Please enter a market ID:");
		BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
		String msg = consoleReader.readLine();
		if (msg.length() == 0)
			msg = this.getMarketIdFromUser();
		else
		{
			try
			{
				Integer.parseInt(msg);
			}
			catch(NumberFormatException e)
			{
				System.out.println(red + "Invalid ID format:" + defaultCl);
				msg = this.getMarketIdFromUser();
			}
		}
		return msg;
	}

	private String getSymbolFromUser() throws Exception
	{
		System.out.println("Please enter a Stock Symbol:");
		BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
		String msg = consoleReader.readLine();
		if (msg.length() == 0)
			msg = this.getSymbolFromUser();
		return msg;
	}

	private String getTransactionFromUser() throws Exception
	{
		System.out.println("Please Select:\n1.Buy Stock\n2.Sell Stock");
		BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
		String msg = consoleReader.readLine();
		if (msg.length() == 0)
			msg = this.getTransactionFromUser();
		try
		{
			int option = Integer.parseInt(msg);
			switch (option){
				case 1:
					msg = "Buy";
					break;
				case 2:
					msg = "Sell";
					break;
				default:
					System.out.println(red + "Invalid Option:" + defaultCl);
					msg = this.getTransactionFromUser();
					break;
			}
		}
		catch(NumberFormatException exc)
		{
			System.out.println(red + "Invalid Option:" + defaultCl);
			msg = this.getTransactionFromUser();
		}

		return msg;
	}

	private String getPriceFromUser() throws Exception
	{
		System.out.println("Price of stock($):");
		BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
		String msg = consoleReader.readLine();
		if (msg.length() == 0)
			msg = this.getPriceFromUser();
		try
		{
			Integer price = Integer.parseInt(msg);
			if (price <= 0)
			{
				System.out.println(red + "Invalid Price:" + defaultCl);
				msg = this.getPriceFromUser();
			}
		}
		catch(NumberFormatException exc)
		{
			System.out.println(red + "Invalid Price:" + defaultCl);
			msg = this.getPriceFromUser();
		}
		return msg;
	}

	private String getQtyFromUser() throws Exception
	{
		System.out.println("Quantity of stock:");
		BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
		String msg = consoleReader.readLine();
		if (msg.length() == 0)
			msg = this.getQtyFromUser();
		try
		{
			Integer qty = Integer.parseInt(msg);
			if (qty <= 0)
			{
				System.out.println(red + "Invalid Quantity:" + defaultCl);
				msg = this.getQtyFromUser();
			}
		}
		catch(NumberFormatException exc)
		{
			System.out.println(red + "Invalid Quantity:" + defaultCl);
			msg = this.getQtyFromUser();
		}
		return msg;
	}
}
