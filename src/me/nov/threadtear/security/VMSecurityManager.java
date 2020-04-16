package me.nov.threadtear.security;

import java.io.FileDescriptor;
import java.security.Permission;

import me.nov.threadtear.Threadtear;

public class VMSecurityManager extends SecurityManager {
	private static final String MSG = "This shouldn't happen. An execution ran code that it's not supposed to. Please open an issue on github!";

	@Override
	public void checkPermission(Permission perm) {
		if (perm.getName().equals("setSecurityManager")) {
			// check if invoked in main class
			if (!Thread.currentThread().getStackTrace()[4].getClassName().equals(Threadtear.class.getName())) {
				throw new SecurityException(MSG);
			}
		}
	}

	@Override
	public void checkPermission(Permission perm, Object context) {
	}

	@Override
	public void checkExec(String cmd) {
		throw new SecurityException(MSG);
	}

	@Override
	public void checkLink(String lib) {
		throw new SecurityException(MSG);
	}

	@Override
	public void checkWrite(FileDescriptor fd) {
		throw new SecurityException(MSG);
	}

	@Override
	public void checkWrite(String file) {
		throw new SecurityException(MSG);
	}

	@Override
	public void checkDelete(String file) {
		throw new SecurityException(MSG);
	}

	@Override
	public void checkConnect(String host, int port) {
		throw new SecurityException(MSG);
	}

	@Override
	public void checkConnect(String host, int port, Object context) {
		throw new SecurityException(MSG);
	}

	@Override
	public void checkPropertiesAccess() {
		throw new SecurityException(MSG);
	}

	@Override
	public void checkCreateClassLoader() {
		for (StackTraceElement st : Thread.currentThread().getStackTrace()) {
			if (st.getClassName().equals("sun.reflect.DelegatingClassLoader")) {
				return;
			}
			try {
				Class.forName(st.getClassName());
			} catch (ClassNotFoundException e) {
				new Throwable().printStackTrace();
				throw new SecurityException(MSG);
			}
		}
		// allow VMs
	}

	@Override
	public void checkSecurityAccess(String target) {
		throw new SecurityException(MSG);
	}
}
