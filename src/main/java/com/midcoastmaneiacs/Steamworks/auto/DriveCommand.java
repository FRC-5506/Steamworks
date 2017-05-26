package com.midcoastmaneiacs.Steamworks.auto;

import com.midcoastmaneiacs.Steamworks.Robot;

import com.midcoastmaneiacs.Steamworks.Scheduler;
import edu.wpi.first.wpilibj.command.Command;

public class DriveCommand extends Command {
	private boolean autopilot;
	private double left;
	private double right;
	private double speed;
	private double time;

	/**
	 * Drives the robot straight, using the autopilot gyro stabilization.
	 * @param speed desired speed
	 * @param time  timeout, in seconds
	 */
	public DriveCommand(double speed, double time) {
		autopilot = true;
		this.speed = speed;
		this.time = time;
		requires(Robot.driveTrain);
	}

	/**
	 * Drives the robot, running the left and right motors independently. Gyro stabilization and acceleration curve
	 * aren't utilized.
	 * @param left  left motor speed
	 * @param right right motor speed
	 * @param time  timeout, in seconds
	 */
	public DriveCommand(double left, double right, double time) {
		autopilot = false;
		this.left = left;
		this.right = right;
		this.time = time;
		requires(Robot.driveTrain);
	}

	// Called just before this Command runs the first time
	protected void initialize() {
		/*if (gear) {
			setTimeout(0.25);
			Robot.driveTrain.setAutopilot(0.4);
		} else {
			setTimeout(2);
			Robot.driveTrain.setAutopilot(-0.6);
		}*/
		setTimeout(time);
		if (autopilot)
			Robot.driveTrain.setAutopilot(speed);
		else {
			Robot.driveTrain.takeControl(this);
			Robot.driveTrain.drive(left, right);
		}
	}

	protected boolean isFinished() {
		return isTimedOut() || !Robot.driveTrain.controlledBy(this);
	}

	protected void end() {
		Robot.driveTrain.relinguishControl(this);
		Robot.driveTrain.drive(0, 0);
	}

	@Override
	public void start() {
		Scheduler.add(this);
	}
}