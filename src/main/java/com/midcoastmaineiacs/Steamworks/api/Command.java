package com.midcoastmaineiacs.Steamworks.api;

import com.midcoastmaineiacs.Steamworks.common.Robot;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class Command {
	private boolean runWhenDisabled = false;
	private boolean canceled = false;
	private boolean running = false;
	protected boolean requireChildren = false;
	public List<Command> children = new ArrayList<>();
	protected Command parent = null;
	private long endAt = Long.MAX_VALUE;
	private long startedAt = 0;
	private long frozenAt = 0;
	boolean enabled = false;
	private boolean hasRun = false;
	private boolean stopExecuting = false;

	public final boolean willRunWhenDisabled() {
		return runWhenDisabled;
	}

	protected void setRunWhenDisabled(boolean runWhenDisabled) {
		this.runWhenDisabled = runWhenDisabled;
	}

	/**
	 * Uses the {@link Scheduler Midcoast Maineiacs Scheduler}. Passive commands are not allowed to start active
	 * commands, but any command <em>can</em> be started outside of a Command all together.
	 */
	public void start() {
		running = true;
		Scheduler.add(this);
	}

	public boolean isCanceled() {
		return canceled;
	}

	public boolean isRunning() {
		return running;
	}

	public void cancel() {
		this.canceled = true;
	}

	/**
	 * @return If the command has been timed out.
	 */
	public boolean isFinished() {
		return isTimedOut();
	}

	/**
	 *
	 * @return If the kill switch has been activated, the parent has stopped, or the command has been released and all
	 *     children have stopped.
	 */
	protected final boolean shouldCancel() {
		if (requireChildren) {
			boolean ok = false;
			for (Command i : children)
				if (i.isRunning() && !i.isCanceled()) {
					ok = true;
					break;
				}
			if (!ok) return true;
		}
		return this instanceof ActiveCommand && Robot.killSwitch() || parent != null && !parent.isRunning() && parent.isCanceled();
	}

	/**
	 * Called by scheduler whenever the command is started. Should only be called by the scheduler.
	 */
	final void _start() {
		requireChildren = false;
		running = true;
		children.clear();
		startedAt = System.currentTimeMillis();
		if (Scheduler.getCurrentCommand() instanceof ActiveCommand) {
			parent = Scheduler.getCurrentCommand();
			if (parent != null)
				parent.children.add(this);
		} else if (Scheduler.getCurrentCommand() != null)
			throw new Scheduler.IllegalPassiveCommandException("Passive command cannot start an active command!\n" +
																   "Modify the command class to extend ActiveCommand!");
		else
			parent = null;
		hasRun = false;
	}

	final void _freeze() {
		frozenAt = System.currentTimeMillis();
	}

	final void _resume() {
		endAt += System.currentTimeMillis() - frozenAt;
		frozenAt = 0;
		resume();
	}

	protected void initialize() {}

	protected void resume() {}

	protected void execute() {}

	protected void end() {}

	protected final boolean isTimedOut() {
		return frozenAt == 0 && System.currentTimeMillis() >= endAt;
	}

	protected final void setTimeout(double seconds) {
		endAt = startedAt + (long) (seconds * 1000);
	}

	final void _removed() {
		running = false;
		frozenAt = 0;
		canceled = false;
		end();
	}

	final boolean _run() {
		if(!hasRun) {
			hasRun = true;
			startedAt = System.currentTimeMillis();
			initialize();
		}
		if (!stopExecuting)
			execute();
		return isFinished() || isCanceled() || shouldCancel();
	}

	/**
	 * After called, {@link Command#shouldCancel()} will cancel the command if there are no running children commands.
	 *
	 * A stopExecuting value of true will tell the command to stop running the execute() method. If you call this method
	 * and decide later that you want to stop the execution but keep the command alive, this method may be called again
	 * to change the execution state. end() will always be called when the command ends.
	 *
	 * @param stopExecuting If true, the execute() block will stop being called.
	 */
	protected void releaseForChildren(boolean stopExecuting) {
		this.stopExecuting = stopExecuting;
		requireChildren = true;
	}

	/**
	 * Calls {@link Command#releaseForChildren(boolean)} with "true" as an argument.
	 */
	protected void releaseForChildren() {releaseForChildren(true);}
}
