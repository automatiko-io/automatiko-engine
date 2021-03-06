package io.automatiko.engine.workflow.base.instance.impl.util;

import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingPrintStream extends PrintStream {

	protected Logger logger;
	private StringBuilder buffer = new StringBuilder();
	protected boolean isError = false;

	public LoggingPrintStream(OutputStream outputStream, boolean isError) {
		super(outputStream);
		this.isError = isError;
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		try {
			logger = LoggerFactory.getLogger(Class.forName(className));
		} catch (ClassNotFoundException e) {
			logger = LoggerFactory.getLogger(this.getClass());
		}
	}

	protected void log(String s) {
		if (isError) {
			logger.error(s);
		} else {
			logger.info(s);
		}

	}

	private void writeString(String s) {
		synchronized (buffer) {
			buffer.append(s);
		}
	}

	private synchronized void newLine() {

		log(buffer.toString());
		buffer.delete(0, buffer.length());

	}

	@Override
	public void print(boolean b) {
		writeString(b ? "true" : "false");
	}

	@Override
	public void print(char c) {
		writeString(String.valueOf(c));
	}

	@Override
	public void print(int i) {
		writeString(String.valueOf(i));
	}

	@Override
	public void print(long l) {
		writeString(String.valueOf(l));
	}

	@Override
	public void print(float f) {
		writeString(String.valueOf(f));
	}

	@Override
	public void print(double d) {
		writeString(String.valueOf(d));
	}

	@Override
	public void print(char[] s) {
		writeString(String.valueOf(s));
	}

	@Override
	public void print(String s) {
		writeString(s == null ? "null" : s);
	}

	@Override
	public void print(Object obj) {
		writeString(String.valueOf(obj));
	}

	@Override
	public void println() {
		newLine();
	}

	@Override
	public void println(boolean x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	@Override
	public void println(char x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	@Override
	public void println(int x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	@Override
	public void println(long x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	@Override
	public void println(float x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	@Override
	public void println(double x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	@Override
	public void println(char[] x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	@Override
	public void println(String x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	@Override
	public void println(Object x) {
		synchronized (logger) {
			print(x);
			newLine();
		}
	}

	public static void interceptSysOutSysErr() {
//        System.setOut(new LoggingPrintStream(System.out, false));
//        System.setErr(new LoggingPrintStream(System.err, true));
	}

	public static void resetInterceptSysOutSysErr() {
//        System.setOut(System.out);
//        System.setErr(System.err);
	}
}