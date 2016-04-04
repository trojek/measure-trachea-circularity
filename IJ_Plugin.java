import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.Opener;
import ij.measure.ResultsTable;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;
import ij.gui.*;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class IJ_Plugin implements PlugIn {

	static String main_folder = "/home/tomasz/Documents/Projects/Nauka/Bronchoskopia/screen_shot/";
	static String input_folder = "norma_ke/";
	static int num_of_photos = 144;
	
	public void run(String arg) {
	
		// String[] ThresholdMethods = {"MaxEntropy", "Minimum", "RenyiEntropy", "Yen"};
		String[] ThresholdMethods = {"Yen"};
								
		// output into the Log window
		IJ.log("Hello, World! works fine");

		String file_extension = ".jpg";
		String file_extra = ".png";

		//Write to file
		try{
    	// Create file 
    	FileWriter fstream = new FileWriter(main_folder + input_folder + System.currentTimeMillis() + "out.txt");
        BufferedWriter out = new BufferedWriter(fstream);

		for (String method : ThresholdMethods) {
			//IJ.log("Theshold for:" + method);
			createDirectory(method);

			// variables needed to count average circularity
			int counter = 0;
			double circularity = 0.0;

				
			for (int i = 1; i < num_of_photos; i++) {

				Opener opener = new Opener();
				ImagePlus imp = opener.openImage(main_folder + input_folder + i	+ file_extra + file_extension);

				// show image
				//imp.show();

				// --------------------------- Begin of Set ROI ------------------------- //
	
				int[] xpoints = {9,9,46,230,265,267,233,44};
				int[] ypoints = {261,44,6,8,43,256,291,293};
				imp.setRoi(new PolygonRoi(xpoints,ypoints,8,Roi.POLYGON));
				// imp.setRoi(new OvalRoi(48,55,186,186));

				// ----------------- Begin of prepare image to analyze ------------------ //
				
				IJ.setBackgroundColor(255, 255, 255);
				IJ.run(imp, "Clear Outside", "");

				// Filter
				IJ.run(imp, "Median...", "radius=3");

				// Threshold
				IJ.run(imp, "HSB Stack", "");
				// Remove first and second slide in order to choose third slice.
				IJ.run(imp, "Delete Slice", "");
				IJ.run(imp, "Delete Slice", "");

				IJ.setAutoThreshold(imp, method);
				Prefs.blackBackground = true;
				IJ.run(imp, "Convert to Mask", "only");

				// Open operation
				for (int j = 0; j < 3; j++) {
					 IJ.run(imp, "Open", "");
				}

				// Filter
				IJ.run(imp, "Median...", "radius=15");

				IJ.saveAs(imp, "Jpeg", main_folder + input_folder + method	+ "/" + i + "_outlined_" + method + file_extension);

				// -------------------- Begin of image to analyze --------------------- //

				// What to measure...
				IJ.run("Set Measurements...", "area mean min centroid shape feret's redirect=None decimal=3");
				IJ.run(imp, "Analyze Particles...", "show=Nothing clear");

	
				// -------------------- Begin of prepare human-readable (easy to analysis) image. --------------------- //
				
				IJ.run(imp, "Outline", "");
				ImagePlus imp1 = IJ.openImage(main_folder + input_folder + i + file_extra + file_extension);
				ImagePlus imp2 = IJ.openImage(main_folder + input_folder + method	+ "/" + i + "_outlined_" + method + file_extension);
   				ImageCalculator ic = new ImageCalculator();
  				ImagePlus imp3 = ic.run("OR create", imp1, imp2);
  				IJ.saveAs(imp3, "Jpeg", main_folder + input_folder + method	+ "/" + i + "_final_" + method + file_extension);		

				ResultsTable rt = Analyzer.getResultsTable();
				if (rt == null) {
						rt = new ResultsTable();
						Analyzer.setResultsTable(rt);
				}

				int num_of_objects = rt.size();

				
				if(num_of_objects == 1){
					// save
					// IJ.saveAs(imp3, "Jpeg", main_folder + input_folder + method	+ "/" + i + "_outlined_" + method + file_extension);
					// IJ.log("Circ: " + rt.getValueAsDouble(18, 0));
					counter++;
					circularity += rt.getValueAsDouble(18, 0);
					out.write(rt.getValueAsDouble(18, 0) + ";");
					
					/*
					// Shows columns names
					for (String heading: rt.getHeadings()) {
						IJ.log("heading: " + heading + " col: " + rt.getColumnIndex(heading));
					}
					
					for(int j = 0; j < num_of_objects + 1; j++) {
						IJ.log("row: " + j + " Area: " + rt.getValueAsDouble(0, j) + " Circ: " + rt.getValueAsDouble(18, j) + "Round: " + rt.getValueAsDouble(34, j));
					}
					*/
						
				}
				
			}
			
			double avg_circ = circularity / counter;
			IJ.log("Avarage circularity = " + avg_circ + ", " + main_folder + input_folder);
		}
		out.close();
    	}catch (Exception e){//Catch exception if any
      	System.err.println("Error: " + e.getMessage());
    	}
	}

	static void createDirectory(String name) {
		File file = new File(main_folder + input_folder + name);
		if (!file.exists()) {
			if (file.mkdir()) {
				System.out.println("Directory " + name + " was created!");
			} else {
				System.out.println("Failed to create directory!");
			}
		}
	}

}
