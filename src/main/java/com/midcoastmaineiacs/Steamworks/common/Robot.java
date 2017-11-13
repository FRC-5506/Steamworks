package com.midcoastmaineiacs.Steamworks.common;

import com.midcoastmaineiacs.Steamworks.api.*;
import com.midcoastmaineiacs.Steamworks.common.auto.Auto;
import com.midcoastmaineiacs.Steamworks.common.auto.Gear;
import com.midcoastmaineiacs.Steamworks.common.auto.VisionServer;
import com.midcoastmaineiacs.Steamworks.rio.RioAPI;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Joystick;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

@SuppressWarnings("WeakerAccess")
public class Robot {
	public static RobotAPI api;
	public static boolean isRio;

	/** How long is the end game period (in seconds)? Currently used to notify the driver */
	public static final int ENDGAME = 30;

	/** ONLY FOR TESTING PURPOSES */
	public static final boolean FORCE_COMPETITION = false;
	/** If true, competition mode will be enabled when practice mode is enabled */
	public static final boolean PRACTICE_IS_COMPETITION = true;
	/** will update based on FMS status, updates on robot init, and when teleop and auto modes are entered */
	public static boolean competition = false;

	// 1 = left; 2 = center; 3 = right
	public static byte starting = 2;
	public static VisionServer vision;

	public static DriveTrain driveTrain;
	public static Climber climber;
	public static List<Subsystem> subsystems;

	public static final Joystick joystick = new Joystick(0);

	public static Timer clock;
	public static Thread mainThread;
	public static DashboardServer dashboard;

	public Robot(RobotAPI api) {
		Robot.api = api;
		isRio = api instanceof RioAPI;
	}

	public void robotInit() {
		mainThread = Thread.currentThread();

		driveTrain = new DriveTrain();
		climber = new Climber();
		subsystems = new ArrayList<>();
		subsystems.add(driveTrain);
		subsystems.add(climber);

		if (isRio)
			driveTrain.gyro.calibrate();

		dashboard = new DashboardServer();

		if (FORCE_COMPETITION) { // FORCE_COMPETITION should always be off except when testing competition-only features!
			api.reportWarning("Force-competition mode is activated, MAKE SURE A PROGRAMMER KNOWS ABOUT THIS!");
			competition = true;
		}

		//Vision.init();
		vision = new VisionServer();
		if (isRio) {
			// front camera (rope climber end)
			CameraServer.getInstance().startAutomaticCapture(0).setExposureManual(40);
			// back camera (gear end)
			CameraServer.getInstance().startAutomaticCapture(1).setExposureManual(50);
		}
		starting = isRio ? (byte) DriverStation.getInstance().getLocation() : 2;
		dashboard.setDouble("pos", starting);

		dashboard.addIndicator("vision", "Iz gud", "No tape", "red", vision.izgud());
		dashboard.addIndicator("power", "Full speed", "Half speed", "orange", fullPower);
		dashboard.addIndicator("competition", "Competition", "Practice", "orange", competition);
		dashboard.addReadout("dt_state", "Unknown drive train state");
		dashboard.addIndicator("pi", "Pi is ALIVE", "Pi is DEAD", "red", vision.isAlive());
		dashboard.applyLayout(2);

		// good to go, start the scheduler
		clock = new Timer(true);
		clock.scheduleAtFixedRate(new Scheduler(), 0, 20);
		System.out.println("All systems go!");
	}

	/**
	 * Updates MMDashboard values, updates competition mode status, and verifies status of rPi
	 */
	public void robotPeriodic() {
		if (joystick.getRawButton(7)) {
			Scheduler.enabled = false;
			if (Scheduler.teleop)
				Scheduler.enableTeleop(false);
		} else if (joystick.getRawButton(8) && !api.isDisabled()) {
			Scheduler.enabled = true;
			if (api.isOperatorControl())
				Scheduler.enableTeleop(true);
		}

		if (!rbWasPressed && joystick.getRawButton(5)) {
			rbWasPressed = true;
			//Vision.requestCapture();
			vision.requestCapture();
			notifyDriver();
		} else if (rbWasPressed && !joystick.getRawButton(5))
			rbWasPressed = false;

		starting = (byte) dashboard.getDouble("pos");

		// if Pi hasn't responded for a second, it's probably dead
		// Pi "responds" by setting "true" to "Pi" every time it processes a frame
		/*if (vision.hasRecentUpdate())
			time = 0;
		else
			time++;
		if (time > 50)
			vision.setDead();
		if (vision.izgud() && !vision.isAlive())
			// clearly the Pi isn't on to target the peg
			vision.setBlind();*/

		dashboard.setBoolean("pi", vision.isAlive());
		dashboard.setBoolean("vision", vision.izgud());
		dashboard.setBoolean("competition", competition);
		dashboard.setBoolean("power", fullPower);
		dashboard.setBoolean("enabled", Scheduler.enabled);
		dashboard.setDouble("heading", driveTrain.getGyroMod() - 180);
		switch(driveTrain.getState()) {
			case DISABLED:
				dashboard.setColoredString("dt_state", "Drive train is DORMANT", "red");
				break;
			case COMMAND:
				dashboard.setColoredString("dt_state", "Drive train in COMMAND control", "orange");
				break;
			case AUTOPILOT:
				dashboard.setColoredString("dt_state", "Drive train in AUTOPILOT", "orange");
				break;
			case TELEOP:
				dashboard.setColoredString("dt_state", "Drive train in TELEOP", "green");
				break;
			default:
				dashboard.setString("dt_state", "Unknown drive train state");
		}

		// debug values
		dashboard.setDouble("calc angle", vision.getTurningAngle());
		dashboard.setDouble("calc distance", vision.getDistance());
		dashboard.setDouble("cam angle", vision.getCameraAngle());
		dashboard.setDouble("cam distance", vision.getCameraDistance());
		dashboard.setBoolean("endgame", endgamePassed);

		if (!FORCE_COMPETITION && isRio) {
			//                              detect whether or not we're at a competition
			boolean willEnableCompetition = DriverStation.getInstance().isFMSAttached() || PRACTICE_IS_COMPETITION &&
																							   // detect practice mode
																							   DriverStation.getInstance().getMatchTime() > 0.0;
			if (!competition && willEnableCompetition)
				api.reportWarning("Competition mode activated");
			competition = willEnableCompetition;
		}
	}

	public void disabledInit() {
		Scheduler.enabled = false;
		Scheduler.enableTeleop(false);
		for (Subsystem i: subsystems)
			i.stop();
		if (competition)
			api.reportWarning("Competition mode activated, commands not cancelled. " +
											"If you are at a competition or practice match, this is normal. " +
											"Otherwise, tell a programmer.");
		else
			Scheduler.cancelAllCommands();
		endgamePassed = false;
	}

	//////////
	// Auto //
	//////////

	public static Command auto;

	/**
	 * Picks auto routine from autochooser and starts it
	 */
	public void autonomousInit() {
		if (isRio)
			driveTrain.gyro.reset();
		Scheduler.enabled = true;
		Scheduler.enableTeleop(false);
		auto = new Auto(dashboard.getString("auto").equals("gear") ? Auto.Mode.GEAR : dashboard.getString("auto").equals("mobility") ? Auto.Mode.SURGE : Auto.Mode.PLAY_DEAD);
		auto.start();
	}

	public static boolean killSwitch() {
		return joystick.getRawButton(1); // A
	}

	////////////
	// Teleop //
	////////////
	/*
	 * Current mappings:
	 *
	 * Sticks         Drive
	 * LB             Toggle speed (100% or 50%, reflected by "Power" light in SmartDashboard, green = 100%)
	 * A              Force control to be taken from auto routine in competition
	 * Y              Run auto routine (defined by SmartDashboard controls)
	 *
	 * POV            Drive arcade (temporarily disables tank drive and trigger-based climber controls)
	 * Right stick X  Additional turning control while using POV arcade drive
	 * Right trigger  Throttle for POV arcade drive
	 * Left trigger   Throttle for POV arcade drive, half speed
	 *
	 * RB             Climb (100%)
	 * B              Climb (50%, use when at top of touch pad to hold position, just tap the button repeatedly)
	 * Left trigger   Climb down
	 * Right trigger  Climb up
	 *
	 * Back           Disable robot
	 * Start          Enable robot (if driver station allows)
	 *
	 * The following will always be mapped, but aren't likely to be used during comp
	 *
	 * X  Reverse climber (50%, to be used during demonstrations and testing, not during comp)
	 *
	 * Notifiers:
	 *
	 * Endgame
	 * DriveTrain state change (from/to DISABLED or TELEOP)
	 */

	private static boolean lbWasPressed = false;
	private static boolean rbWasPressed = false;
	/** "true" = 100% power (competition mode), "false" = 50% power (demonstration/small space mode) */
	private static boolean fullPower = true;
	/** "true" adds a 15% dead zone in the middle of the controller to ensure joysticks rest in non-motor-moving position */
	private static final boolean DEAD_ZONE = true;
	/** true = teleop is enabled but the driver hasn't gotten control yet */
	private static boolean waitingForTeleop = true;
	/** Have we notified the driver of endgame since last disable? */
	private static boolean endgamePassed = false;

	public void teleopInit() {
		Scheduler.enabled = true;
		Scheduler.enableTeleop(true);
		waitingForTeleop = !driveTrain.controlledByTeleop();

		if (!competition) {
			// commands should've been cancelled during disabledInit, but just to be safe
			Scheduler.cancelAllCommands();
		}
		/*if (driveTrain.controlledByTeleop()) {
			notifyDriver();
		}*/

		/*SmartDashboard.putBoolean("Endgame", false);
		(new Timer()).schedule(new TimerTask() {
			@Override
			public void run() {
				// endgame has arrived
				if (Scheduler.enabled) {
					notifyDriver();
					SmartDashboard.putBoolean("Endgame", true);
				}
			}
			// using edu.wpilib.first.wpilibj.Timer is based on how long teleop has been enabled, not the match
			// configuration, so this will alert the driver at the right time even when not in practice or FMS mode
		}, (long) ((edu.wpi.first.wpilibj.Timer.getMatchTime() + 150 - ENDGAME) * 1000));*/
	}

	public void teleopPeriodic() {
		// Endgame notification
		if (isRio && !endgamePassed && DriverStation.getInstance().getMatchTime() > 0 && DriverStation.getInstance().getMatchTime() <= ENDGAME) {
			endgamePassed = true;
			notifyDriver();
		}

		if (!Scheduler.enabled) return;
		// DriveTrain control
		if (driveTrain.controlledByTeleop()) {
			waitingForTeleop = false;
			if (joystick.getPOV() != -1) { // TODO: test POV driving
				double forward = Math.cos(Math.toRadians(joystick.getPOV()));
				double turn = -Math.sin(Math.toRadians(joystick.getPOV())) + joystick.getRawAxis(4); // 4 = right X
				double throttle = joystick.getRawAxis(2) / 2 + joystick.getRawAxis(3);
				driveTrain.driveArcade(forward * throttle, turn * throttle);
				if (climber.controlledByTeleop())
					climber.stop();
			} else {
				// left axis = 1, right axis = 5
				double leftSpeed = -joystick.getRawAxis(1);
				double rightSpeed = -joystick.getRawAxis(5);
				//noinspection ConstantConditions
				driveTrain.driveCurved(!DEAD_ZONE || Math.abs(leftSpeed) > 0.15 ? leftSpeed * (fullPower ? 1 : 0.5) : 0,
					!DEAD_ZONE || Math.abs(rightSpeed) > 0.15 ? rightSpeed * (fullPower ? 1 : 0.5) : 0);
			}
			if (joystick.getRawButton(4)) {
				//autochooser.getSelected().start();
				ActiveCommand command = new Gear(Gear.ScanMode.STATION);
				driveTrain.takeControl(command);
				command.start();
			}
		} else

		// Auto take-over
		if (waitingForTeleop /*|| killSwitch() should be handled by commands themselves */) {
			// this means that the auto period has just ended, teleop has just started, but the auto routine is still
			// running, waiting for the driver to manually take control, or the driver is holding down A, so autonomous
			// commands started during teleop need to be killed.
			if (Math.abs(joystick.getRawAxis(1)) >= 0.9 && Math.abs(joystick.getRawAxis(5)) >= 0.9 /*|| killSwitch()*/) {
				Scheduler.cancelAllCommands();
				waitingForTeleop = false;
			}
		}

		// Climber control
		if (climber.controlledByTeleop()) {
			if (joystick.getRawButton(6)) // RB
				climber.set(1);
			else if (joystick.getRawButton(2)) // B
				climber.set(0.5);
			else if (joystick.getRawButton(3)) // X
				climber.set(-0.5);
			else if (joystick.getPOV() == -1)
				climber.set(joystick.getRawAxis(3) - joystick.getRawAxis(2));
		}
	}

	public void disabledPeriodic() {
		if (!lbWasPressed && joystick.getRawButton(5)) { // LB
			lbWasPressed = true;
			fullPower = !fullPower;
		} else if (lbWasPressed && !joystick.getRawButton(5)) {
			lbWasPressed = false;
		}
		if (killSwitch())
			Scheduler.cancelAllCommands();
	}

	/**
	 * Notifies the driver of some sort of event (by rumbling the controller).
	 */
	public static void notifyDriver() {
		(new Notifier()).start();
	}

	public void testPeriodic() {}

	public void autonomousPeriodic() {}

	public void testInit() {}
}
