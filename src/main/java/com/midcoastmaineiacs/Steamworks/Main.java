package com.midcoastmaineiacs.Steamworks;

import com.sun.org.apache.bcel.internal.util.ClassPath;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

public class Main {
	@SuppressWarnings("WeakerAccess")
	public static void writeSpaced(String text, PrintStream s) {
		s.write('\n');
		s.write('|');
		for (int i = 0; i < text.length() + 2; i++) s.write('=');
		s.println('|');
		s.println("| " + text + " |");
		s.write('|');
		for (int i = 0; i < text.length() + 2; i++) s.write('=');
		s.println("|\n");
	}
	public static void main(String[] args) {
		System.out.println(ClassPath.getClassPath());
		String c;
		if (args.length == 0)
			c = "Rio";
		else
			c = args[0];
		try {
			try {
				// Make sure the Rio API is only run on the rio.
				if (c.equals("Rio")) Class.forName("edu.wpi.first.wpilibj.hal.JNIWrapper");
			} catch (UnsatisfiedLinkError e) {
				writeSpaced("Unable to load WPILib libraries.", System.err);
				System.err.println("The Rio API can only be run on the roboRio!");
				System.err.println("If this is on the rio, try re-imaging the rio.");
				throw new Exception("Rio API can only be run on the roboRio!", e);
			}
			try {
				Class.forName(Main.class.getPackage().getName() + "." + c.toLowerCase() + "." + c + "Main").getMethod("main", String[].class).invoke(null, (Object) args);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
				if (e instanceof ClassNotFoundException)
					throw new Exception("This code does not have a " + c + " API!", e);
				throw new RuntimeException(e);
			}
			writeSpaced("Robots don't quit!", System.err);
			System.exit(1);
		} catch (Exception e) {
			writeSpaced(c + " API crashed!", System.err);
			if (!c.equals("Rio")) {
				Throwable rootCause = e;
				while (rootCause.getCause() != null) rootCause = rootCause.getCause();
				StackTraceElement[] trace = rootCause.getStackTrace();
				for (StackTraceElement i : trace) {
					if (i.getClassName().startsWith("edu.wpi")) {
						int j = 0;
						//noinspection StatementWithEmptyBody
						for (; j < trace.length - 1 && !trace[j].getClassName().startsWith("com.midcoastmaineiacs"); j++);
						StackTraceElement[] newTrace = new StackTraceElement[trace.length - j];
						System.arraycopy(trace, j, newTrace, 0, trace.length - j);
						e = new Exception("Things break when you try to use WPILib without the Rio!", e);
						e.setStackTrace(newTrace);
						break;
					}
				}
			}
			try {
				System.err.write("Uncaught ".getBytes());
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			System.out.flush();
			e.printStackTrace();
			writeSpaced("Robots don't quit!", System.err);
			System.exit(e instanceof RuntimeException ? 1 : 2);
		}
	}
}
