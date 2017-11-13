package com.midcoastmaineiacs.Steamworks.api;

@SuppressWarnings({"SameParameterValue", "unused"})
public abstract class RobotAPI {
	public abstract Motor getMotor(Subsystem parent, Device device, boolean inverted);

	public abstract boolean isDisabled();

	public abstract boolean isEnabled();

	public abstract boolean isAutonomous();

	public abstract boolean isTest();

	public abstract boolean isOperatorControl();

	public abstract void reportWarning(String message);

	public abstract void reportError(String message);
}
