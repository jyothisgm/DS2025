package ds.pa2;

public class Config {

	private static final int DEFAULT_INTERMEDIATE_CHUNCK_SIZE = 1 * 1024 * 1024; // 1 MB
	private static final int DEFAULT_OUTPUT_CHUNCK_SIZE = 1 * 1024 * 1024; // 1 MB
	
	private String inputDir;
	private String intermediateDir;
	private String outputDir;
	private int intermediateChunkSize;
	private int outputChunkSize;
	
	public int getIntermediateChunkSize() {
		return intermediateChunkSize;
	}

	public void setIntermediateChunkSize(int intermediateChunkSize) {
		this.intermediateChunkSize = intermediateChunkSize;
	}

	public int getOutputChunkSize() {
		return outputChunkSize;
	}

	public void setOutputChunkSize(int outputChunkSize) {
		this.outputChunkSize = outputChunkSize;
	}

	public String getInputDir() {
		return inputDir;
	}

	public String getIntermediateDir() {
		return intermediateDir;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public Config(String inputDir, String intermediateDir, String outputDir) {
		super();
		this.inputDir = inputDir;
		this.intermediateDir = intermediateDir;
		this.outputDir = outputDir;
		this.intermediateChunkSize = DEFAULT_INTERMEDIATE_CHUNCK_SIZE;
		this.outputChunkSize = DEFAULT_OUTPUT_CHUNCK_SIZE;
	}
}
