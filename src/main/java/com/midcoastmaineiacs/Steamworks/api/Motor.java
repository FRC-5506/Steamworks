package com.midcoastmaineiacs.Steamworks.api;

public abstract class Motor {
	protected final Subsystem parent;

	protected Motor(Subsystem parent) {
		this.parent = parent;
	}

	public abstract void stop();

	public abstract void set(double speed);
}
