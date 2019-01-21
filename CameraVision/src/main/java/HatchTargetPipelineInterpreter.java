/**
* Interpret the result of the ball pipeline.  This abstracts out the logic
* from the pipeline class.
*
* @author Chuck Benedict, Mentor, Team 997
*/
public class HatchTargetPipelineInterpreter {

	// Processed pipeline that we will do the interpretation against
	private HatchTargetPipeline pipeline;

	/**
	* Constructor taking a processed pipeline
	*
	* @param pipeline	A processed pipeline that returns blob found results
	*/
	public HatchTargetPipelineInterpreter(HatchTargetPipeline pipeline) {
		if (pipeline == null)
		{
			throw new IllegalArgumentException("Pipline cannot be null.");
		}
		this.pipeline = pipeline;
	}

	/**
	 * Did we find at least one target on a processed frame?
	 * 
	 * @return True if at least one target was found
	 */
	public boolean targetsFound() {
		return !this.pipeline.findContoursOutput().isEmpty();
	}

	/**
	 * Get the count of the number of targets found on a processed frame.
	 *  
	 * @return The count of the number of targets found
	 */
	public long targetCount() {
		return this.pipeline.findContoursOutput().size();
	}
}