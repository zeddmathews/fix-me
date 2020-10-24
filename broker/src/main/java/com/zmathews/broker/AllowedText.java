package com.zmathews.broker;

public class AllowedText {

	public static String validArgument(String args) {
		String error;
		if (args.equalsIgnoreCase("skeleton") || args.equalsIgnoreCase("more_skeleton")) {
			error = "";
		}
		else {
			error = "Invalid argument";
		}
		return (error);
	}
}