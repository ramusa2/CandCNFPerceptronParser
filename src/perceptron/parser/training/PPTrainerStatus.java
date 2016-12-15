package perceptron.parser.training;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;


/**
 * The PPTrainerConfig class stores the status of a 
 * {@link PPTrainer} pipeline (i.e., it indicates which 
 * steps have been performed).
 * 
 * @author ramusa2
 *
 */
public class PPTrainerStatus {

	/**
	 * Default constructor
	 */
	public PPTrainerStatus() {
		// TODO: add status keywords
	}

	/**
	 * Loads and returns a pre-defined pipeline status.
	 */
	public static PPTrainerStatus load(File file) {
		return new PPTrainerStatus();
	}

	/**
	 * Saves the status to the target file.
	 */
	public void save(File file) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if(pw != null) {
			pw.close();
		}
	}
}
