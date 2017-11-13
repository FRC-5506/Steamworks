package com.midcoastmaineiacs.Steamworks.rio;

import com.midcoastmaineiacs.Steamworks.common.Robot;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.RobotBase;

public class RioMain extends IterativeRobot {
	private static Robot robot;
	static RioMain instance;

	public RioMain() {
		super();
		instance = this;
	}

	public static void main(String... args) {
		robot = new Robot(new RioAPI());
		RobotBase.main(args);
	}

	@Override
	public void autonomousInit() {
		robot.autonomousInit();
	}

	@Override
	public void autonomousPeriodic() {
		robot.autonomousPeriodic();
	}

	@Override
	public void disabledInit() {
		robot.disabledInit();
	}

	@Override
	public void disabledPeriodic() {
		robot.disabledPeriodic();
	}

	@Override
	public void teleopInit() {
		robot.teleopInit();
	}

	@Override
	public void teleopPeriodic() {
		robot.teleopPeriodic();
	}

	@Override
	public void testInit() {
		robot.testInit();
	}

	@Override
	public void testPeriodic() {
		robot.testPeriodic();
	}

	@Override
	public void robotInit() {
		robot.robotInit();
	}

	@Override
	public void robotPeriodic() {
		robot.robotPeriodic();
	}
}
