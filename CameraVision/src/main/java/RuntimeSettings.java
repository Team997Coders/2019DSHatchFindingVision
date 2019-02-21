import com.beust.jcommander.*;

/*
* RuntimeSettings class.
*
* <p>Package up command line processing into a separate class.  This abstracts out the logic
*    from the main class and makes this more testable.
*
* @author Chuck Benedict
*/
public class RuntimeSettings {
    // Command line args
    private String[] argv;
    // Program name that you want to pass to command line parsing...which will be put into usage info
    private static final String programName = "CameraVision";

    // command line args
    @Parameter(names={"--team", "-t"}, required=true, description="FIRST team number")
    private int team;
    @Parameter(names={"--nthost", "-h"},  
        description="NetworkTables server host IP address (the roborio address typically)")
    private String ntHost = "";
    @Parameter(names={"--nont", "-n"},  
        description="Do not call out to network tables to write interpreted values")
    private boolean noNT = false;
    @Parameter(names={"--frontcameraurl", "-f"},  
        description="Use specified MJPEG over http streaming source for front camera")
    private String frontCameraURL = "";
    @Parameter(names={"--backcameraurl", "-b"},  
        description="Use specified MJPEG over http streaming source for back camera")
    private String backCameraURL = "";
    @Parameter(names = "--help", help = true)
    private boolean help = false;

    // Internal jCommander variable
    private JCommander jc;

    // Parse error message
    private String parseErrorMessage;

	public RuntimeSettings(String ... argv) {
		if (argv == null)
		{
			throw new IllegalArgumentException("Arguments cannot be null.");
		}
        // Init the jcommander parser
        jc = JCommander.newBuilder()
            .programName(programName)
            .addObject(this)
            .build();
        this.argv = argv;
    }
    
    public boolean parse() {
        // parse command line args
        try {
            jc.parse(argv);
            return true;
        } catch (ParameterException pe) {
            // print the parameter error, show the usage, and bail
            parseErrorMessage = pe.getMessage();
            return false;
        }
    }

    public void printUsage() {
        jc.usage();
    }

    // Define getters
    public String getParseErrorMessage() {
        return parseErrorMessage;
    }

    public int getTeam() {
        return team;
    }

    public String getNTHost() {
        return ntHost;
    }

    public boolean getNoNT() {
        return noNT;
    }

    public String getFrontCameraURL() {
        return frontCameraURL;
    }

    public String getBackCameraURL() {
        return backCameraURL;
    }

    public boolean getHelp() {
        return help;
    }
}