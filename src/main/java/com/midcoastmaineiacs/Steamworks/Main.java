package com.midcoastmaineiacs.Steamworks;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Main {
	public static void main(String[] args) {
		String c;
		if (args.length == 0)
			c = "Rio";
		else
			c = args[0];
		try {
			try {
				Class.forName(Main.class.getPackage().getName() + "." + c.toLowerCase() + "." + c + "Main").getMethod("main", String[].class).invoke(null, (Object) args);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
				if (e instanceof ClassNotFoundException)
					throw new RuntimeException("This code does not have a " + c + " API!", e);
				throw new RuntimeException(e);
			}
			System.err.println("\n==================");
			System.err.println("Robots don't quit!");
			System.err.println("==================\n");
			System.exit(1);
		} catch (RuntimeException e) {
			System.err.println(c + " API crashed!");
			try {
				System.err.write("Uncaught ".getBytes());
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			System.out.flush();
			e.printStackTrace();
			System.err.println("\n==================");
			System.err.println("Robots don't quit!");
			System.err.println("==================\n");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
			System.exit(1);
		}
	}
}
