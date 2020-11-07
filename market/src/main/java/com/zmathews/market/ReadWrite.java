package com.zmathews.market;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import java.net.*;
import org.json.*;

public class ReadWrite {
	public String	readFromSoc(DataInstance dataInstance) throws Exception {
		dataInstance.buffer.clear();
		if (dataInstance.channel.read(dataInstance.buffer).get() == -1) {
			System.out.format("Router unavailable, Shuting Down ...%n");
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

	private void clearBuffer(DataInstance dataInstance) {
		String msg = "";
		dataInstance.buffer.clear();
		Charset cs = Charset.forName("UTF-8");
		byte[] data = msg.getBytes(cs);
		dataInstance.buffer.put(data);
		dataInstance.buffer.flip();
	}

	public String messageHandler(String msg, DataInstance dataInstance) throws Exception {
		String[] shards = msg.split("\\|");
		if (shards[0].equalsIgnoreCase("ID")) {
			dataInstance.id = Integer.parseInt(shards[1]);
			System.out.println("ID: " + shards[1]);
			return("Id updated");
		}
		else {
			String returnMsg = shards[5] + "|" + dataInstance.id + "|";
			String symbolInfo = symbolInfo(shards[1]);
			JSONObject obj = new JSONObject(symbolInfo);
			try {
				String symbolPrice = obj.getJSONObject("Global Quote").getString("05. price");
				String symbolVolume = obj.getJSONObject("Global Quote").getString("06. volume");

				if (shards[2].equalsIgnoreCase("buy")) {
					if (Double.parseDouble(shards[3]) >= Double.parseDouble(symbolPrice)) {
						if (Integer.parseInt(shards[4]) <= Integer.parseInt(symbolVolume)) {
							returnMsg += "Buy Success";
						}
						else {
							returnMsg += "Buy Failed: Insuffiecient Quantity available";
						}
					}
					else {
						returnMsg += "Buy Failed: Price to low";
					}
				}
				else {
					if (Double.parseDouble(shards[3]) <= Double.parseDouble(symbolPrice)) {
						returnMsg += "Sell Success";
					}
					else {
						returnMsg += "Sell Failed: Price to High";
					}
				}
			}
			catch(JSONException e) {
				returnMsg += "Symbol Not available in Market";
			}
			int checksum = returnMsg.length() + 22;
			returnMsg += "|" + Integer.toString(checksum);
			return(returnMsg);
		}
	}

	public void	writeToSoc(DataInstance dataInstance, String msg) throws Exception {
		dataInstance.buffer.clear();
		Charset cs = Charset.forName("UTF-8");
		byte[] data = msg.getBytes(cs);
		dataInstance.buffer.put(data);
		dataInstance.buffer.flip();
		if (dataInstance.channel.write(dataInstance.buffer).get() == -1) {
			System.out.format("Router unavailable, Shuting Down ...%n");
			dataInstance.mainThread.interrupt();
			return;
		}
	}

	private String symbolInfo(String symbol) throws Exception {
		URL url = new URL("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=GSA8L5WLCNAL7YFL");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestMethod("GET");
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();
		con.disconnect();
		return(content.toString());
	}
}