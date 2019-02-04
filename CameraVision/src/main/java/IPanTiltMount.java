public interface IPanTiltMount {
  void slew(int panPct, int tiltPct) throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException;
  Angles getAngles() throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException;
  void pan(int panPct) throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException;
  void tilt(int tiltPct) throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException;
  void center() throws 
      CommunicationClosedException, 
      CommunicationErrorException,
      CommunicationFailureException;
}