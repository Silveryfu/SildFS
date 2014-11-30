package com.sildfs.server;

/**
 * A socket based heart-beat  implementation to check if a server peed
 * is still alive;
 */

import java.net.Socket;
import java.util.TimerTask;

public class SildHeartBeater extends TimerTask {

	private Socket so;
	private boolean isReplica;

	public SildHeartBeater(Socket so) {
		this.setSo(so);
	}

	public SildHeartBeater(Socket so, boolean isReplica) {
		this.setSo(so);
		this.setReplica(isReplica);
	}

	public void run() {
		heartbeat();
	}

	public void heartbeat() {
		try {
			this.getSo().getOutputStream().write(1);
		} catch (Exception e) {
			if (this.isReplica()) {
				System.out.println("--R-- Primary offline.");
				becomePrimary();
				this.cancel();
			} else {
				System.out.println("--P-- Replica offline.");
				SildHandler.isReplicated.set(false);
				this.cancel();
			}
		}
	}
	
	public void becomePrimary() {
		SildMain.getSild().stopReplicaAgent();
		SildMain.getSild().startService();
	}

	public Socket getSo() {
		return so;
	}

	public void setSo(Socket so) {
		this.so = so;
	}

	public boolean isReplica() {
		return isReplica;
	}

	public void setReplica(boolean isReplica) {
		this.isReplica = isReplica;
	}
}
