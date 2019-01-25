import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.delegates.Action;

import org.opencv.core.Mat;

public class HeadsUpDisplay {
  private final ImageAnnotator imageAnnotator;
  StateMachine<State, Trigger> stateMachine;

  public HeadsUpDisplay(ImageAnnotator imageAnnotator) {
    this.imageAnnotator = imageAnnotator;
    stateMachine = new StateMachine<>(State.IdentifyingTargets, GetConfig());
  }

  public static StateMachineConfig<State, Trigger> GetConfig() {
    StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

    config.configure(State.IdentifyingTargets)
      .permit(Trigger.AButton, State.SlewingToTarget)     // should probably be permit ifs only if target is available
      .permit(Trigger.BButton, State.SlewingToTarget)
      .permit(Trigger.XButton, State.SlewingToTarget)
      .permit(Trigger.YButton, State.SlewingToTarget);
    
    config.configure(State.SlewingToTarget)
      .permit(Trigger.LockOn, State.TargetLocked)
      .permit(Trigger.FailedToLock, State.LockFailed);

    config.configure(State.TargetLocked)
      .permit(Trigger.AButton, State.DrivingToTarget)
      .permit(Trigger.BButton, State.IdentifyingTargets)
      .permit(Trigger.LoseLock, State.LockLost);

    config.configure(State.LockFailed)
      .permit(Trigger.FindTargets, State.IdentifyingTargets);

    config.configure(State.LockLost)
      .permit(Trigger.FindTargets, State.IdentifyingTargets);

    config.configure(State.DrivingToTarget)
      .permit(Trigger.LoseLock, State.LockLost)
      .permit(Trigger.AButton, State.TargetLocked)
      .permit(Trigger.BButton, State.IdentifyingTargets);

    // TODO: There will also be configurations for calibration menus

    return config;
  }

  public void aButtonPressed() {
    stateMachine.fire(Trigger.AButton);
  }

  public void bButtonPressed() {
    stateMachine.fire(Trigger.BButton);
  }

  public void xButtonPressed() {
    stateMachine.fire(Trigger.XButton);
  }

  public void yButtonPressed() {
    stateMachine.fire(Trigger.YButton);
  }

  public Mat update(Mat inputImage) {
    imageAnnotator.beginAnnotation(inputImage);
    if (stateMachine.getState() == State.IdentifyingTargets) {
      imageAnnotator.drawTargetingRectangles();
      imageAnnotator.drawHatchTargetRectangles();
      imageAnnotator.printDistanceToHatchTargetInInches();  
    }
    return imageAnnotator.completedAnnotate();
  }

  private enum State {
    IdentifyingTargets, SlewingToTarget, TargetLocked, LockFailed, LockLost, DrivingToTarget
  }

  private enum Trigger {
    AButton, BButton, XButton, YButton, LockOn, FailedToLock, LoseLock, FindTargets
  }
}