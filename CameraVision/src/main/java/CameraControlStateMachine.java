// TODO: Put this in common project shared between this and robot application
public class CameraControlStateMachine {
  /**
   * The valid states of the state machine.
   */
  public enum State {
    IdentifyingTargets, SlewingToTarget, TargetLocked, LockFailed, LockLost, DrivingToTarget, Slewing, Centering, Calibrating
  }

  /**
   * Triggers that cause state transitions.
   */
  public enum Trigger {
    AButton, BButton, XButton, YButton, LockOn, FailedToLock, LoseLock, IdentifyTargets, Slew, LeftThumbstickButton, LeftShoulderButton
  }
}