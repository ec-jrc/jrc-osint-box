package com.jrc.tmacc.emotionsentiment;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.ext.textprocessing.data.Document;
import org.knime.ext.textprocessing.data.DocumentValue;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
// import the Mood Classifier jar file
import moodclassifier.MoodClassifier;



/**
 * This is an example implementation of the node model of the
 * "EmotionSentiment" node. * 
 * This example node performs a classification of input strings
 * according to their sentiment
 *
 * @author me
 */
public class EmotionSentimentNodeModel extends NodeModel {

	// variables
	/**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(EmotionSentimentNodeModel.class);
	
	/**
	 * Here follows settings key to retrieve and store settings shared between 
	 * node dialog and node model.  
	 * Each KEY_... (String) identifies the name of the attribute that
	 * should be entered by the user in the dialog. 
	 * Each DEFAULT_... (types varies) defines the default value for each
	 * attribute 
	 */		
	// common definition for strings
	// due to some terminology issues, the term probability is left only in code
	// when speaking of the results, the more correct "Score" term will be used
	public static final String EMOTION = "Emotion";
	public static final String EMOTION_PROBABILITY = "Emotion score";
	public static final String SENTIMENT = "Sentiment";	
	public static final String SENTIMENT_PROBABILITY = "Sentiment score";

	//defining keys ad defaults for Emotion analysis
	private static final String KEY_EMOTION = "emotion";
	private static final Boolean DEFAULT_EMOTION = false;
	/*
	 * to keep the node simple there will be no choice on the model from the user
	// where to find the feature file for Weka
	//(more information in the documentation for MoodClassifier)
	private static final String KEY_EMOTION_FEATURE = "emotion_feature_path";	
	private static final String DEFAULT_EMOTION_FEATURE = "";	
	// where to find the model file for Weka
	//(more information in the documentation for MoodClassifier)
	private static final String KEY_EMOTION_MODEL = "emotion_model_path";	
	private static final String DEFAULT_EMOTION_MODEL = "";
	// where to find the header file for Weka
	//(more information in the documentation for MoodClassifier)
	private static final String KEY_EMOTION_HEADER = "emotion_header_path";
	private static final String DEFAULT_EMOTION_HEADER = "";
	*/
	// path for the model files
	private static final String EMOTION_MODEL = ""+File.separator+"src"+File.separator+"com"+File.separator+"jrc"+File.separator+"tmacc"+File.separator+"emotionsentiment"+File.separator+"models"+File.separator+"emotion"+File.separator+"EmotionModel.model";
	private static final String EMOTION_MODEL_FEATURES = ""+File.separator+"src"+File.separator+"com"+File.separator+"jrc"+File.separator+"tmacc"+File.separator+"emotionsentiment"+File.separator+"models"+File.separator+"emotion"+File.separator+"FeaturesModelEm.txt";
	private static final String EMOTION_MODEL_HEADER = ""+File.separator+"src"+File.separator+"com"+File.separator+"jrc"+File.separator+"tmacc"+File.separator+"emotionsentiment"+File.separator+"models"+File.separator+"emotion"+File.separator+"WekaEmotion.arff";	
	// if the result should also report some probability
	private static final String KEY_EMOTION_PROBABILITY = "emotion_probability";
	private static final Boolean DEFAULT_EMOTION_PROBABILITY = false;

	//defining keys ad defaults for sentiment analysis
	private static final String KEY_SENTIMENT = "sentiment";
	private static final Boolean DEFAULT_SENTIMENT = true;
	/*
	 * to keep the node simple there will be no choice on the model from the user
	// where to find the feature file for Weka
	//(more information in the documentation for MoodClassifier)
	private static final String KEY_SENTIMENT_FEATURE = "sentiment_feature_path";	
	private static final String DEFAULT_SENTIMENT_FEATURE = "";	
	// where to find the model file for Weka
	//(more information in the documentation for MoodClassifier)
	private static final String KEY_SENTIMENT_MODEL = "sentiment_model_path";	
	private static final String DEFAULT_SENTIMENT_MODEL = "";
	// where to find the header file for Weka
	//(more information in the documentation for MoodClassifier)
	private static final String KEY_SENTIMENT_HEADER = "sentiment_header_path";
	private static final String DEFAULT_SENTIMENT_HEADER = "";
	*/
	private static final String SENTIMENT_MODEL = File.separator+"src"+File.separator+"com"+File.separator+"jrc"+File.separator+"tmacc"+File.separator+"emotionsentiment"+File.separator+"models"+File.separator+"sentiment"+File.separator+"Senti5ang.model";
	private static final String SENTIMENT_MODEL_FEATURES = File.separator+"src"+File.separator+"com"+File.separator+"jrc"+File.separator+"tmacc"+File.separator+"emotionsentiment"+File.separator+"models"+File.separator+"sentiment"+File.separator+"FeaturesModelAll5Lang.txt";
	private static final String SENTIMENT_MODEL_HEADER = File.separator+"src"+File.separator+"com"+File.separator+"jrc"+File.separator+"tmacc"+File.separator+"emotionsentiment"+File.separator+"models"+File.separator+"sentiment"+File.separator+"WekaTrainingAll5Lang.arff";	
	// if the result should also report some details
	private static final String KEY_SENTIMENT_PROBABILITY = "sentiment_probability";
	private static final Boolean DEFAULT_SENTIMENT_PROBABILITY = false;	

	//the column to be used for the analysis
	private static final String KEY_COLUMN = "column";
	private static final String DEFAULT_COLUMN = "none";	

	// constant defining the length of the jar name
	private static final int NAME = 20;
	
	// how to handle missing inputs - default has missing values as null
	private static final String KEY_HANDLE_MISSING = "missing_value";
	private static final Boolean DEFAULT_HANDLE_MISSING_VAL = true;

	/**
	 * The settings models to manage the shared settings. These models will hold the
	 * values entered by the user in the dialog and will update once the user changes
	 * the values. Furthermore, it provides methods to easily load and save the values
	 * to and from the shared settings 
	 */
	// for emotion
	private final SettingsModelBoolean emotionSettings = createEmotionSettingsModel();
	/* removed for node simplification
	private final SettingsModelString emotionFeatureSettings = createEmotionFeatureSettingsModel();
	private final SettingsModelString emotionModelSettings = createEmotionModelSettingsModel();
	private final SettingsModelString emotionHeaderSettings = createEmotionHeaderSettingsModel();
	*/
	private final SettingsModelBoolean emotionProbabilitySettings = createEmotionProbabilitySettingsModel();
	// for sentiment
	private final SettingsModelBoolean sentimentSettings = createSentimentSettingsModel();	
	/* removed for node simplification
	private final SettingsModelString sentimentFeatureSettings = createSentimentFeatureSettingsModel();
	private final SettingsModelString sentimentModelSettings = createSentimentModelSettingsModel();
	private final SettingsModelString sentimentHeaderSettings = createSentimentHeaderSettingsModel();
	*/
	private final SettingsModelBoolean sentimentProbabilitySettings = createSentimentProbabilitySettingsModel();
	// for the input column
	private final SettingsModelString inputColumnSettings = createInputColumnSettingsModel();
	// for missing values
	private final SettingsModelBoolean missingValuesSettings = createMissingValuesSettingsModel();
	// methods
	/**
	 * Constructor for the node model.
	 */
	protected EmotionSentimentNodeModel() {
		/**
		 * Here we specify how many data input and output tables the node should have.
		 * In this case its one input and one output table.
		 * The size of the table will be defined in the execute method
		 */
		super(1, 1);
	}

	/*
	 * Here we define methods to create  new settings models 
	 * These methods will also be used in the {@link EmotionSentimentNodeDialog}. 
	 * The settings model will sync via the above defined key.  
	 */
	//emotion
	static SettingsModelBoolean createEmotionSettingsModel() {
		return new SettingsModelBoolean(KEY_EMOTION, DEFAULT_EMOTION);
	}	
	/* removed for node simplification
	static SettingsModelString createEmotionFeatureSettingsModel() {
		return new SettingsModelString(KEY_EMOTION_FEATURE, DEFAULT_EMOTION_FEATURE);
	}	
	static SettingsModelString createEmotionModelSettingsModel() {
		return new SettingsModelString(KEY_EMOTION_MODEL, DEFAULT_EMOTION_MODEL);
	}	
	static SettingsModelString createEmotionHeaderSettingsModel() {
		return new SettingsModelString(KEY_EMOTION_HEADER, DEFAULT_EMOTION_HEADER);
	}
	*/	
	static SettingsModelBoolean createEmotionProbabilitySettingsModel() {
		return new SettingsModelBoolean(KEY_EMOTION_PROBABILITY, DEFAULT_EMOTION_PROBABILITY);
	}	
	//sentiment
	static SettingsModelBoolean createSentimentSettingsModel() {
		return new SettingsModelBoolean(KEY_SENTIMENT, DEFAULT_SENTIMENT);
	}	
	/* removed for node simplification
	static SettingsModelString createSentimentFeatureSettingsModel() {
		return new SettingsModelString(KEY_SENTIMENT_FEATURE, DEFAULT_SENTIMENT_FEATURE);
	}	
	static SettingsModelString createSentimentModelSettingsModel() {
		return new SettingsModelString(KEY_SENTIMENT_MODEL, DEFAULT_SENTIMENT_MODEL);
	}	
	static SettingsModelString createSentimentHeaderSettingsModel() {
		return new SettingsModelString(KEY_SENTIMENT_HEADER, DEFAULT_SENTIMENT_HEADER);
	}
	*/	
	static SettingsModelBoolean createSentimentProbabilitySettingsModel() {
		return new SettingsModelBoolean(KEY_SENTIMENT_PROBABILITY, DEFAULT_SENTIMENT_PROBABILITY);
	}
	// input column
	static SettingsModelString createInputColumnSettingsModel() {
		return new SettingsModelString(KEY_COLUMN, DEFAULT_COLUMN);
	}	
	//missing values
	static SettingsModelBoolean createMissingValuesSettingsModel() {
		return new SettingsModelBoolean(KEY_HANDLE_MISSING, DEFAULT_HANDLE_MISSING_VAL);
	}

	/**
	 * The functionality of the node is implemented in the execute method. This will
	 * call code from the Sentiment library
	 * 
	 * This implementation will label an input text String column according to the
	 * user's choice The output will differ according to the user's selections There
	 * can between 1 result column (only Sentiment with no details or only Emotions
	 * with no details) and N columns (Sentiment and Emotion with details)
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		LOGGER.info("Starting the execution.");
		/*
		 * The input data table to work with. The "inData" array will contain as many
		 * input tables as specified in the constructor. In this case it can only be one
		 * (see constructor).
		 */
		BufferedDataTable inputTable = inData[0];
		DataTableSpec tableSpec = inputTable.getDataTableSpec();		
		// we find the index of the column which had been selected by the user					
		int column = tableSpec.findColumnIndex(inputColumnSettings.getStringValue());
		// we need to know the user's choices in order to create the specifics for the
		// output
		// which type of analysis does he wants
		Boolean emotion = emotionSettings.getBooleanValue();
		Boolean sentiment = sentimentSettings.getBooleanValue();
		// and if he wants the details for the analysis
		Boolean emotionProbability = emotionProbabilitySettings.getBooleanValue();
		Boolean sentimentProbability = sentimentProbabilitySettings.getBooleanValue();		
		// see how to handle missing values
		Boolean missingValues = missingValuesSettings.getBooleanValue();
		
		
		// a tricky way to hardcode the path of the model files
		// we get the path of execution of the plugin (which is a jar)
		String path = new File(EmotionSentimentNodeModel.class.getProtectionDomain().getCodeSource().getLocation()
			    .toURI()).getPath();
		// remove the name of the jar 
		path = path.substring(0, path.length() - NAME);
		// here we have the starting path for the models
		// this path is then used to reference all the model files
		
		// we instantiate an "empty" result table
		BufferedDataTable out = null;	
		
		if (emotion && sentiment) {
			if (emotionProbability && sentimentProbability) {
				/*
				 * here we have to add 4 columns: 
				 * one containing the Emotion label 
				 * one containing the Sentiment label
				 * one containing the probability for emotion
				 * one containing the probability for sentiment
				 */
				// this will be option 1
				LOGGER.info("preparing output...");
				DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), 1);				
				// Create a data container to which we will add rows sequentially.
				BufferedDataContainer container = exec.createDataContainer(outputSpec);
				// create an iterator
				CloseableRowIterator rowIterator = inputTable.iterator();
				// instantiate the correct classifier
				/*
				MoodClassifier emotionClassifier = new MoodClassifier(emotionFeatureSettings.getStringValue(),
						emotionHeaderSettings.getStringValue(), emotionModelSettings.getStringValue());				
				MoodClassifier sentimentClassifier = new MoodClassifier(sentimentFeatureSettings.getStringValue(),
						sentimentHeaderSettings.getStringValue(), sentimentModelSettings.getStringValue());
				*/
				MoodClassifier emotionClassifier = new MoodClassifier(path+EMOTION_MODEL_FEATURES,
						path+EMOTION_MODEL_HEADER, path+EMOTION_MODEL);				
				MoodClassifier sentimentClassifier = new MoodClassifier(path+SENTIMENT_MODEL_FEATURES,
						path+SENTIMENT_MODEL_HEADER, path+SENTIMENT_MODEL);
				/*
				 * A counter for how many rows have already been processed. This is used to
				 * calculate the progress of the node, which is displayed as a loading bar under
				 * the node icon.
				 */
				int currentRowCounter = 0;
				// Iterate over the rows of the input table.				
				LOGGER.info("Starting Analysis...");
				while (rowIterator.hasNext()) {
					List<DataCell> cells = new ArrayList<>();
					DataRow currentRow = rowIterator.next();
					// copy the input table
					for(DataCell cell:currentRow) {
						cells.add(cell);
					}
					// we know which column holds the correct text to analyze
					// some conversions are needed
					DataCell cell = currentRow.getCell(column);
					String argument = "";
					// handle the Document input
					try {				
						DocumentValue dv = (DocumentValue) cell;
			        	Document d = dv.getDocument();
			        	
			        	if(d != null) { 		        		
			        		argument = d.getDocumentBodyText();	 
			        	}
					}
					catch (Exception e) {
						System.out.print(e.getMessage());
					}		        	
					//handle the string input
					try {
						StringCell argumentCell = (StringCell) cell;						
						argument = argumentCell.getStringValue();
					} catch (Exception e) {
						System.out.print(e.getMessage());
					}
					
					if(argument.isEmpty()) {
						if(missingValues) {
							// here we add the emotion label
							cells.add(new MissingCell(""));
							// here we add emotion probability
							cells.add(new MissingCell(""));
							// here we add the sentiment label
							cells.add(new MissingCell(""));
							// here we add probability
							cells.add(new MissingCell(""));
						}
						else {
							// here we add the emotion label
							cells.add(new StringCell(""));
							// here we add emotion probability
							cells.add(new DoubleCell(0.0));
							// here we add the sentiment label
							cells.add(new StringCell(""));
							// here we add probability
							cells.add(new DoubleCell(0.0));
						}
						
					}
					else {
						// here we add the emotion label
						cells.add(new StringCell(emotionClassifier.classify(argument)));
						// here we add emotion probability
						cells.add(new DoubleCell(format(emotionClassifier.probability(argument))));
						// here we add the sentiment label
						cells.add(new StringCell(sentimentClassifier.classify(argument)));
						// here we add probability
						cells.add(new DoubleCell(format(sentimentClassifier.probability(argument))));
					}
					// Add the new row to the output data container
					DataRow row = new DefaultRow(currentRow.getKey(), cells);
					container.addRowToTable(row);
					// We finished processing one row, hence increase the counter
					currentRowCounter++;
					/*
					 * Here we check if a user triggered a cancel of the node. If so, this call will
					 * throw an exception and the execution will stop. This should be done
					 * frequently during execution, e.g. after the processing of one row if
					 * possible.
					 */
					exec.checkCanceled();
					/*
					 * Calculate the percentage of execution progress and inform the
					 * ExecutionMonitor. This will be shown in a bar below the node
					 */
					exec.setProgress(currentRowCounter / (double) inputTable.size(),
							"Analysing row " + currentRowCounter);
				}
				//here we close the container and get the output data
				container.close();
				out = container.getTable();

			} 
			else {
				if(emotionProbability) {
					/*
					 * here we have to add 3 columns: 
					 * one containing the Emotion label 
					 * one containing the Sentiment label
					 * one containing the probability for emotion
					 */
					// this will be option 2
					LOGGER.info("preparing output...");
					DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), 2);
					// Create a data container to which we will add rows sequentially.
					BufferedDataContainer container = exec.createDataContainer(outputSpec);
					// create an iterator
					CloseableRowIterator rowIterator = inputTable.iterator();
					// instantiate the correct classifier
					/*
					MoodClassifier emotionClassifier = new MoodClassifier(emotionFeatureSettings.getStringValue(),
							emotionHeaderSettings.getStringValue(), emotionModelSettings.getStringValue());				
					MoodClassifier sentimentClassifier = new MoodClassifier(sentimentFeatureSettings.getStringValue(),
							sentimentHeaderSettings.getStringValue(), sentimentModelSettings.getStringValue());
					*/
					MoodClassifier emotionClassifier = new MoodClassifier(path+EMOTION_MODEL_FEATURES,
							path+EMOTION_MODEL_HEADER, path+EMOTION_MODEL);				
					MoodClassifier sentimentClassifier = new MoodClassifier(path+SENTIMENT_MODEL_FEATURES,
							path+SENTIMENT_MODEL_HEADER, path+SENTIMENT_MODEL);
					/*
					 * A counter for how many rows have already been processed. This is used to
					 * calculate the progress of the node, which is displayed as a loading bar under
					 * the node icon.
					 */
					int currentRowCounter = 0;
					// Iterate over the rows of the input table.					
					LOGGER.info("Starting Analysis...");
					while (rowIterator.hasNext()) {
						List<DataCell> cells = new ArrayList<>();
						DataRow currentRow = rowIterator.next();
						// copy the input table
						for(DataCell cell:currentRow) {
							cells.add(cell);
						}
						// we know which column holds the correct text to analyze
						// some conversions are needed
						DataCell cell = currentRow.getCell(column);
						String argument = "";
						// handle the Document input
						try {				
							DocumentValue dv = (DocumentValue) cell;
				        	Document d = dv.getDocument();
				        	
				        	if(d != null) { 		        		
				        		argument = d.getDocumentBodyText();	 
				        	}
						}
						catch (Exception e) {
							System.out.print(e.getMessage());
						}		        	
						//handle the string input
						try {
							StringCell argumentCell = (StringCell) cell;						
							argument = argumentCell.getStringValue();
						} catch (Exception e) {
							System.out.print(e.getMessage());
						}
						if(argument.isEmpty()) {
							if(missingValues) {
								// here we add the emotion label
								cells.add(new MissingCell(""));
								// here we add emotion probability
								cells.add(new MissingCell(""));
								// here we add the sentiment label
								cells.add(new MissingCell(""));								
							}
							else {
								// here we add the emotion label
								cells.add(new StringCell(""));
								// here we add emotion probability
								cells.add(new DoubleCell(0.0));
								// here we add the sentiment label
								cells.add(new StringCell(""));								
							}
							
						}
						else {
							// here we add the emotion label
							cells.add(new StringCell(emotionClassifier.classify(argument)));
							// here we add emotion probability
							cells.add(new DoubleCell(format(emotionClassifier.probability(argument))));
							// here we add the sentiment label
							cells.add(new StringCell(sentimentClassifier.classify(argument)));
						}			
						// Add the new row to the output data container
						DataRow row = new DefaultRow(currentRow.getKey(), cells);
						container.addRowToTable(row);
						// We finished processing one row, hence increase the counter
						currentRowCounter++;
						/*
						 * Here we check if a user triggered a cancel of the node. If so, this call will
						 * throw an exception and the execution will stop. This should be done
						 * frequently during execution, e.g. after the processing of one row if
						 * possible.
						 */
						exec.checkCanceled();
						/*
						 * Calculate the percentage of execution progress and inform the
						 * ExecutionMonitor. This will be shown in a bar below the node
						 */
						exec.setProgress(currentRowCounter / (double) inputTable.size(),
								"Analysing row " + currentRowCounter);
					}
					//here we close the container and get the output data
					container.close();
					out = container.getTable();
				}
				else {
					if(sentimentProbability) {
						/*
						 * here we have to add 3 columns: 
						 * one containing the Emotion label 
						 * one containing the Sentiment label
						 * one containing the probability for sentiment
						 */
						// this will be option 3
						LOGGER.info("preparing output...");
						DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), 3);
						// Create a data container to which we will add rows sequentially.
						BufferedDataContainer container = exec.createDataContainer(outputSpec);
						// create an iterator
						CloseableRowIterator rowIterator = inputTable.iterator();
						// instantiate the correct classifier
						/*
						MoodClassifier emotionClassifier = new MoodClassifier(emotionFeatureSettings.getStringValue(),
								emotionHeaderSettings.getStringValue(), emotionModelSettings.getStringValue());				
						MoodClassifier sentimentClassifier = new MoodClassifier(sentimentFeatureSettings.getStringValue(),
								sentimentHeaderSettings.getStringValue(), sentimentModelSettings.getStringValue());
						*/
						MoodClassifier emotionClassifier = new MoodClassifier(path+EMOTION_MODEL_FEATURES,
								path+EMOTION_MODEL_HEADER, path+EMOTION_MODEL);				
						MoodClassifier sentimentClassifier = new MoodClassifier(path+SENTIMENT_MODEL_FEATURES,
								path+SENTIMENT_MODEL_HEADER, path+SENTIMENT_MODEL);
						/*
						 * A counter for how many rows have already been processed. This is used to
						 * calculate the progress of the node, which is displayed as a loading bar under
						 * the node icon.
						 */
						int currentRowCounter = 0;
						// Iterate over the rows of the input table.						
						LOGGER.info("Starting Analysis...");
						while (rowIterator.hasNext()) {
							List<DataCell> cells = new ArrayList<>();
							DataRow currentRow = rowIterator.next();
							// copy the input table
							for(DataCell cell:currentRow) {
								cells.add(cell);
							}
							// we know which column holds the correct text to analyze
							// some conversions are needed
							DataCell cell = currentRow.getCell(column);
							String argument = "";
							// handle the Document input
							try {				
								DocumentValue dv = (DocumentValue) cell;
					        	Document d = dv.getDocument();
					        	
					        	if(d != null) { 		        		
					        		argument = d.getDocumentBodyText();	 
					        	}
							}
							catch (Exception e) {
								System.out.print(e.getMessage());
							}		        	
							//handle the string input
							try {
								StringCell argumentCell = (StringCell) cell;						
								argument = argumentCell.getStringValue();
							} catch (Exception e) {
								System.out.print(e.getMessage());
							}
							
							if(argument.isEmpty()) {
								if(missingValues) {
									// here we add the emotion label
									cells.add(new MissingCell(""));									
									// here we add the sentiment label
									cells.add(new MissingCell(""));
									// here we add probability
									cells.add(new MissingCell(""));
								}
								else {
									// here we add the emotion label
									cells.add(new StringCell(""));									
									// here we add the sentiment label
									cells.add(new StringCell(""));
									// here we add probability
									cells.add(new DoubleCell(0.0));
								}
								
							}
							else {
								// here we add the emotion label
								cells.add(new StringCell(emotionClassifier.classify(argument)));								
								// here we add the sentiment label
								cells.add(new StringCell(sentimentClassifier.classify(argument)));
								// here we add probability
								cells.add(new DoubleCell(format(sentimentClassifier.probability(argument))));
							}
							DataRow row = new DefaultRow(currentRow.getKey(), cells);
							container.addRowToTable(row);
							// We finished processing one row, hence increase the counter
							currentRowCounter++;
							/*
							 * Here we check if a user triggered a cancel of the node. If so, this call will
							 * throw an exception and the execution will stop. This should be done
							 * frequently during execution, e.g. after the processing of one row if
							 * possible.
							 */
							exec.checkCanceled();
							/*
							 * Calculate the percentage of execution progress and inform the
							 * ExecutionMonitor. This will be shown in a bar below the node
							 */
							exec.setProgress(currentRowCounter / (double) inputTable.size(),
									"Analysing row " + currentRowCounter);
						}
						//here we close the container and get the output data
						container.close();
						out = container.getTable();

					}
					else {
						/*
						 * here we have to add 2 columns: 
						 * one containing the Emotion label 
						 * one containing the Sentiment label
						 */
						// this will be option 4
						LOGGER.info("preparing output...");
						DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), 4);
						// Create a data container to which we will add rows sequentially.
						BufferedDataContainer container = exec.createDataContainer(outputSpec);
						// create an iterator
						CloseableRowIterator rowIterator = inputTable.iterator();
						// instantiate the correct classifier
						/*
						MoodClassifier emotionClassifier = new MoodClassifier(emotionFeatureSettings.getStringValue(),
								emotionHeaderSettings.getStringValue(), emotionModelSettings.getStringValue());				
						MoodClassifier sentimentClassifier = new MoodClassifier(sentimentFeatureSettings.getStringValue(),
								sentimentHeaderSettings.getStringValue(), sentimentModelSettings.getStringValue());
						*/
						MoodClassifier emotionClassifier = new MoodClassifier(path+EMOTION_MODEL_FEATURES,
								path+EMOTION_MODEL_HEADER, path+EMOTION_MODEL);				
						MoodClassifier sentimentClassifier = new MoodClassifier(path+SENTIMENT_MODEL_FEATURES,
								path+SENTIMENT_MODEL_HEADER, path+SENTIMENT_MODEL);
						/*
						 * A counter for how many rows have already been processed. This is used to
						 * calculate the progress of the node, which is displayed as a loading bar under
						 * the node icon.
						 */
						int currentRowCounter = 0;
						// Iterate over the rows of the input table.						
						LOGGER.info("Starting Analysis...");
						while (rowIterator.hasNext()) {
							List<DataCell> cells = new ArrayList<>();
							DataRow currentRow = rowIterator.next();
							// copy the input table
							for(DataCell cell:currentRow) {
								cells.add(cell);
							}
							// we know which column holds the correct text to analyze
							// some conversions are needed
							DataCell cell = currentRow.getCell(column);
							String argument = "";
							// handle the Document input
							try {				
								DocumentValue dv = (DocumentValue) cell;
					        	Document d = dv.getDocument();
					        	
					        	if(d != null) { 		        		
					        		argument = d.getDocumentBodyText();	 
					        	}
							}
							catch (Exception e) {
								System.out.print(e.getMessage());
							}		        	
							//handle the string input
							try {
								StringCell argumentCell = (StringCell) cell;						
								argument = argumentCell.getStringValue();
							} catch (Exception e) {
								System.out.print(e.getMessage());
							}
							if(argument.isEmpty()) {
								if(missingValues) {
									// here we add the emotion label
									cells.add(new MissingCell(""));									
									// here we add the sentiment label
									cells.add(new MissingCell(""));									
								}
								else {
									// here we add the emotion label
									cells.add(new StringCell(""));									
									// here we add the sentiment label
									cells.add(new StringCell(""));
								}
								
							}
							else {
								// here we add the emotion label
								cells.add(new StringCell(emotionClassifier.classify(argument)));								
								// here we add the sentiment label
								cells.add(new StringCell(sentimentClassifier.classify(argument)));
							}
							DataRow row = new DefaultRow(currentRow.getKey(), cells);
							container.addRowToTable(row);
							// We finished processing one row, hence increase the counter
							currentRowCounter++;
							/*
							 * Here we check if a user triggered a cancel of the node. If so, this call will
							 * throw an exception and the execution will stop. This should be done
							 * frequently during execution, e.g. after the processing of one row if
							 * possible.
							 */
							exec.checkCanceled();
							/*
							 * Calculate the percentage of execution progress and inform the
							 * ExecutionMonitor. This will be shown in a bar below the node
							 */
							exec.setProgress(currentRowCounter / (double) inputTable.size(),
									"Analysing row " + currentRowCounter);
						}
						//here we close the container and get the output data
						container.close();
						out = container.getTable();

					}

				}
			}
		} 
		else {
			if (emotion) {
				if (emotionProbability) {
					/*
					 * here we have to add 2 columns: 
					 * one containing the Emotion label 
					 * one containing the emotion score
					 */
					// this will be option 5
					LOGGER.info("preparing output...");
					DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), 5);
					// Create a data container to which we will add rows sequentially.
					BufferedDataContainer container = exec.createDataContainer(outputSpec);
					// create an iterator
					CloseableRowIterator rowIterator = inputTable.iterator();
					// instantiate the correct classifier
					/*
					MoodClassifier classifier = new MoodClassifier(emotionFeatureSettings.getStringValue(),
							emotionHeaderSettings.getStringValue(), emotionModelSettings.getStringValue());
					*/					
					MoodClassifier classifier = new MoodClassifier(path+EMOTION_MODEL_FEATURES,
							path+EMOTION_MODEL_HEADER, path+EMOTION_MODEL);						
					/*
					 * A counter for how many rows have already been processed. This is used to
					 * calculate the progress of the node, which is displayed as a loading bar under
					 * the node icon.
					 */
					int currentRowCounter = 0;
					// Iterate over the rows of the input table.					
					LOGGER.info("Starting Analysis...");
					while (rowIterator.hasNext()) {
						List<DataCell> cells = new ArrayList<>();
						DataRow currentRow = rowIterator.next();
						// copy the input table
						for(DataCell cell:currentRow) {
							cells.add(cell);
						}
						// we know which column holds the correct text to analyze
						// some conversions are needed
						DataCell cell = currentRow.getCell(column);
						String argument = "";
						// handle the Document input
						try {				
							DocumentValue dv = (DocumentValue) cell;
				        	Document d = dv.getDocument();
				        	
				        	if(d != null) { 		        		
				        		argument = d.getDocumentBodyText();	 
				        	}
						}
						catch (Exception e) {
							System.out.print(e.getMessage());
						}		        	
						//handle the string input
						try {
							StringCell argumentCell = (StringCell) cell;						
							argument = argumentCell.getStringValue();
						} catch (Exception e) {
							System.out.print(e.getMessage());
						}
						
						if(argument.isEmpty()) {
							if(missingValues) {
								// here we add the emotion label
								cells.add(new MissingCell(""));
								// here we add emotion probability
								cells.add(new MissingCell(""));								
							}
							else {
								// here we add the emotion label
								cells.add(new StringCell(""));
								// here we add emotion probability
								cells.add(new DoubleCell(0.0));								
							}
							
						}
						else {
							// here we add the emotion label
							cells.add(new StringCell(classifier.classify(argument)));
							// here we add emotion probability
							cells.add(new DoubleCell(format(classifier.probability(argument))));							
						}
						// Add the new row to the output data container
						DataRow row = new DefaultRow(currentRow.getKey(), cells);
						container.addRowToTable(row);
						// We finished processing one row, hence increase the counter
						currentRowCounter++;
						/*
						 * Here we check if a user triggered a cancel of the node. If so, this call will
						 * throw an exception and the execution will stop. This should be done
						 * frequently during execution, e.g. after the processing of one row if
						 * possible.
						 */
						exec.checkCanceled();
						/*
						 * Calculate the percentage of execution progress and inform the
						 * ExecutionMonitor. This will be shown in a bar below the node
						 */
						exec.setProgress(currentRowCounter / (double) inputTable.size(),
								"Analysing row " + currentRowCounter);
					}
					//here we close the container and get the output data
					container.close();
					out = container.getTable();
				} 
				else {
					// here we have to add one column containing the Emotion label
					// this will be option 6
					LOGGER.info("preparing output...");
					DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), 6);
					// Create a data container to which we will add rows sequentially.
					BufferedDataContainer container = exec.createDataContainer(outputSpec);
					// create an iterator
					CloseableRowIterator rowIterator = inputTable.iterator();
					// instantiate the correct classifier
					/*
					MoodClassifier classifier = new MoodClassifier(emotionFeatureSettings.getStringValue(),
							emotionHeaderSettings.getStringValue(), emotionModelSettings.getStringValue());
					*/					
					MoodClassifier classifier = new MoodClassifier(path+EMOTION_MODEL_FEATURES,
							path+EMOTION_MODEL_HEADER, path+EMOTION_MODEL);
					/*
					 * A counter for how many rows have already been processed. This is used to
					 * calculate the progress of the node, which is displayed as a loading bar under
					 * the node icon.
					 */
					int currentRowCounter = 0;
					// Iterate over the rows of the input table.					
					LOGGER.info("Starting Analysis...");
					while (rowIterator.hasNext()) {
						List<DataCell> cells = new ArrayList<>();
						DataRow currentRow = rowIterator.next();
						// copy the input table
						for(DataCell cell:currentRow) {
							cells.add(cell);
						}
						// we know which column holds the correct text to analyze
						// some conversions are needed
						DataCell cell = currentRow.getCell(column);
						String argument = "";
						// handle the Document input
						try {				
							DocumentValue dv = (DocumentValue) cell;
				        	Document d = dv.getDocument();
				        	
				        	if(d != null) { 		        		
				        		argument = d.getDocumentBodyText();	 
				        	}
						}
						catch (Exception e) {
							System.out.print(e.getMessage());
						}		        	
						//handle the string input
						try {
							StringCell argumentCell = (StringCell) cell;						
							argument = argumentCell.getStringValue();
						} catch (Exception e) {
							System.out.print(e.getMessage());
						}
						
						if(argument.isEmpty()) {
							if(missingValues) {
								// here we add the emotion label
								cells.add(new MissingCell(""));
							}
							else {
								// here we add the emotion label
								cells.add(new StringCell(""));
							}
							
						}
						else {
							// here we add the emotion label
							cells.add(new StringCell(classifier.classify(argument)));
						}
						// Add the new row to the output data container
						DataRow row = new DefaultRow(currentRow.getKey(), cells);
						container.addRowToTable(row);
						// We finished processing one row, hence increase the counter
						currentRowCounter++;
						/*
						 * Here we check if a user triggered a cancel of the node. If so, this call will
						 * throw an exception and the execution will stop. This should be done
						 * frequently during execution, e.g. after the processing of one row if
						 * possible.
						 */
						exec.checkCanceled();
						/*
						 * Calculate the percentage of execution progress and inform the
						 * ExecutionMonitor. This will be shown in a bar below the node
						 */
						exec.setProgress(currentRowCounter / (double) inputTable.size(),
								"Analysing row " + currentRowCounter);
					}
					//here we close the container and get the output data
					container.close();
					out = container.getTable();
				}
			}
			else{
				if (sentiment) {
					if (sentimentProbability) {
						/*
						 * here we have to add 2 columns: 
						 * one containing the Sentiment label 
						 * one containing the sentiment score
						 */
						// this will be option 7
						LOGGER.info("preparing output...");
						DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), 7);
						// Create a data container to which we will add rows sequentially.
						BufferedDataContainer container = exec.createDataContainer(outputSpec);
						// create an iterator
						CloseableRowIterator rowIterator = inputTable.iterator();
						// instantiate the correct classifier						
						MoodClassifier classifier = new MoodClassifier(path+SENTIMENT_MODEL_FEATURES,
								path+SENTIMENT_MODEL_HEADER, path+SENTIMENT_MODEL);
						/*
						 * A counter for how many rows have already been processed. This is used to
						 * calculate the progress of the node, which is displayed as a loading bar under
						 * the node icon.
						 */
						int currentRowCounter = 0;
						// Iterate over the rows of the input table.
						
						LOGGER.info("Starting Analysis...");
						while (rowIterator.hasNext()) {
							List<DataCell> cells = new ArrayList<>();
							DataRow currentRow = rowIterator.next();
							// copy the input table
							for(DataCell cell:currentRow) {
								cells.add(cell);
							}
							// we know which column holds the correct text to analyze
							// some conversions are needed
							DataCell cell = currentRow.getCell(column);
							String argument = "";
							// handle the Document input
							try {				
								DocumentValue dv = (DocumentValue) cell;
					        	Document d = dv.getDocument();
					        	
					        	if(d != null) { 		        		
					        		argument = d.getDocumentBodyText();	 
					        	}
							}
							catch (Exception e) {
								System.out.print(e.getMessage());
							}		        	
							//handle the string input
							try {
								StringCell argumentCell = (StringCell) cell;						
								argument = argumentCell.getStringValue();
							} catch (Exception e) {
								System.out.print(e.getMessage());
							}
							
							if(argument.isEmpty()) {
								if(missingValues) {
									// here we add the sentiment label
									cells.add(new MissingCell(""));
									// here we add probability
									cells.add(new MissingCell(""));
								}
								else {
									// here we add the sentiment label
									cells.add(new StringCell(""));
									// here we add probability
									cells.add(new DoubleCell(0.0));
								}
								
							}
							else {
								// here we add the sentiment label
								cells.add(new StringCell(classifier.classify(argument)));
								// here we add probability
								cells.add(new DoubleCell(format(classifier.probability(argument))));
							}
							// Add the new row to the output data container
							DataRow row = new DefaultRow(currentRow.getKey(), cells);
							container.addRowToTable(row);
							// We finished processing one row, hence increase the counter
							currentRowCounter++;
							/*
							 * Here we check if a user triggered a cancel of the node. If so, this call will
							 * throw an exception and the execution will stop. This should be done
							 * frequently during execution, e.g. after the processing of one row if
							 * possible.
							 */
							exec.checkCanceled();
							/*
							 * Calculate the percentage of execution progress and inform the
							 * ExecutionMonitor. This will be shown in a bar below the node
							 */
							exec.setProgress(currentRowCounter / (double) inputTable.size(),
									"Analysing row " + currentRowCounter);
						}
						//here we close the container and get the output data
						container.close();
						out = container.getTable();
					} else {
						// here we have to add one column containing the Sentiment label
						// this will be option 8
						LOGGER.info("preparing output...");						
						DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), 8);						
						// Create a data container to which we will add rows sequentially.
						BufferedDataContainer container = exec.createDataContainer(outputSpec);						
						// create an iterator
						CloseableRowIterator rowIterator = inputTable.iterator();
						// instantiate the correct classifier						
						MoodClassifier classifier = new MoodClassifier(path+SENTIMENT_MODEL_FEATURES,
								path+SENTIMENT_MODEL_HEADER,path+SENTIMENT_MODEL);
						/*
						 * A counter for how many rows have already been processed. This is used to
						 * calculate the progress of the node, which is displayed as a loading bar under
						 * the node icon.
						 */
						int currentRowCounter = 1;
						// Iterate over the rows of the input table.
						
						LOGGER.info("Starting Analysis...");
						
						while (rowIterator.hasNext()) {
							List<DataCell> cells = new ArrayList<>();
							DataRow currentRow = rowIterator.next();
							// copy the input table
							for(DataCell cell:currentRow) {
								cells.add(cell);
							}
							// we know which column holds the correct text to analyze
							// some conversions are needed								
							DataCell cell = currentRow.getCell(column);
							String argument = "";
							// handle the Document input
							try {				
								DocumentValue dv = (DocumentValue) cell;
					        	Document d = dv.getDocument();
					        	
					        	if(d != null) { 		        		
					        		argument = d.getDocumentBodyText();	 
					        	}
							}
							catch (Exception e) {
								System.out.print(e.getMessage());
							}		        	
							//handle the string input
							try {
								StringCell argumentCell = (StringCell) cell;						
								argument = argumentCell.getStringValue();
							} catch (Exception e) {
								System.out.print(e.getMessage());
							}
							
							if(argument.isEmpty()) {
								if(missingValues) {
									// here we add the sentiment label
									cells.add(new MissingCell(""));
								}
								else {
									// here we add the sentiment label
									cells.add(new StringCell(""));
								}
								
							}
							else {
								// here we add the sentiment label
								cells.add(new StringCell(classifier.classify(argument)));
							}
							// Add the new row to the output data container
							DataRow row = new DefaultRow(currentRow.getKey(), cells);
							container.addRowToTable(row);
							
							// We finished processing one row, hence increase the counter							
							/*
							 * Here we check if a user triggered a cancel of the node. If so, this call will
							 * throw an exception and the execution will stop. This should be done
							 * frequently during execution, e.g. after the processing of one row if
							 * possible.
							 */
							exec.checkCanceled();
							/*
							 * Calculate the percentage of execution progress and inform the
							 * ExecutionMonitor. This will be shown in a bar below the node
							 */
							exec.setProgress(currentRowCounter / (double) inputTable.size(),
									"Analysing row " + currentRowCounter);							
							currentRowCounter++;
						}						
						//here we close the container and get the output data						
						container.close();						
						out = container.getTable();
						
					}
				}
			}
		}

		// once here, we will have the output table for the specific result
		// Once we are done, we close the container and return its table.		
		return new BufferedDataTable[] { out };
	}


	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		/*
		 * Check if the node is executable, e.g. all required user settings are
		 * available and valid, or the incoming types are feasible for the node to
		 * execute. In case the node can execute in its current configuration with the
		 * current input, calculate and return the table spec that would result of the
		 * execution of this node. I.e. this method precalculates the table spec of the
		 * output table.
		 * 
		 * Here we perform a sanity check on the entered number format String. In this
		 * case we just try to apply it to some dummy double number. If there is a
		 * problem, an IllegalFormatException will be thrown. We catch the exception and
		 * wrap it in a InvalidSettingsException with an informative message for the
		 * user. The message should make clear what the problem is and how it can be
		 * fixed if this information is available. This will be displayed in the KNIME
		 * console and printed to the KNIME log. The log will also contain the stack
		 * trace.
		 */
		/*String format = m_numberFormatSettings.getStringValue();
		try {
			//String.format(format, 0.0123456789);
		} catch (IllegalFormatException e) {
			throw new InvalidSettingsException(
					"The entered format is not a valid pattern String! Reason: " + e.getMessage(), e);
		}

		/*
		 * Similar to the return type of the execute method, we need to return an array
		 * of DataTableSpecs with the length of the number of outputs ports of the node
		 * (as specified in the constructor). The resulting table created in the execute
		 * methods must match the spec created in this method. As we will need to
		 * calculate the output table spec again in the execute method in order to
		 * create a new data container, we create a new method to do that.
		 */
		/*
		DataTableSpec inputTableSpec = inSpecs[0];
		return new DataTableSpec[] { createOutputSpec(inputTableSpec,1)};
		*/
		return new DataTableSpec[] { null};
	}

	/**
	 * Creates the output table spec from the input spec. 
	 * The output table has String columns relative to the appropriate user's choice
	 * 
	 * @param inputTableSpec
	 * @param option
	 * 
	 * The option represents the user choice, and allows to add the proper number of
	 * columns with the proper labels
	 * 1. Emotion + Sentiment + Emotion probability + Sentiment Probability
	 * 2. Emotion + Sentiment + Emotion probability
	 * 3. Emotion + Sentiment + Sentiment Probability
	 * 4. Emotion + Sentiment 
	 * 5. Emotion + Emotion probability 
	 * 6. Emotion 
	 * 7. Sentiment + Sentiment Probability
	 * 8. Sentiment
	 * @return
	 */
	private DataTableSpec createOutputSpec(DataTableSpec inputTableSpec,int option) {
		List<DataColumnSpec> newColumnSpecs = new ArrayList<>();	
		
		//save all the inputs
		for(int i = 0; i < inputTableSpec.getNumColumns();i++) {
			newColumnSpecs.add(inputTableSpec.getColumnSpec(i));
		}
		
		// now add the appropriate number of columns
		switch(option) {
		case 1:{//Emotion + Sentiment + Emotion probability + Sentiment Probability			
			DataColumnSpecCreator specCreator = new DataColumnSpecCreator(EMOTION, StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());			
			specCreator.setName(EMOTION_PROBABILITY);			
			specCreator.setType(DoubleCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			specCreator.setName(SENTIMENT);
			specCreator.setType(StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			specCreator.setName(SENTIMENT_PROBABILITY);			
			specCreator.setType(DoubleCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			break;
		}
		case 2:{//Emotion + Sentiment + Emotion probability			
			DataColumnSpecCreator specCreator = new DataColumnSpecCreator(EMOTION, StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());
			specCreator.setName(EMOTION_PROBABILITY);			
			specCreator.setType(DoubleCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			specCreator.setName(SENTIMENT);
			specCreator.setType(StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());
			break;
		}
		case 3:{//Emotion + Sentiment + Sentiment Probability
			DataColumnSpecCreator specCreator = new DataColumnSpecCreator(EMOTION, StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());			
			specCreator.setName(SENTIMENT);
			specCreator.setType(StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			specCreator.setName(SENTIMENT_PROBABILITY);			
			specCreator.setType(DoubleCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			break;
		}
		case 4:{//Emotion + Sentiment 
			DataColumnSpecCreator specCreator = new DataColumnSpecCreator(EMOTION, StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());			
			specCreator.setName(SENTIMENT);
			specCreator.setType(StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			break;
		}
		case 5:{// Emotion + Emotion probability 
			DataColumnSpecCreator specCreator = new DataColumnSpecCreator(EMOTION, StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());			
			specCreator.setName(EMOTION_PROBABILITY);			
			specCreator.setType(DoubleCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			break;
		}
		case 6:{// Emotion 			
			DataColumnSpecCreator specCreator1 = new DataColumnSpecCreator(EMOTION, StringCell.TYPE);
			newColumnSpecs.add(specCreator1.createSpec());				
			break;
		}
		case 7:{//Sentiment + Sentiment Probability
			DataColumnSpecCreator specCreator = new DataColumnSpecCreator(SENTIMENT, StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());			
			specCreator.setName(SENTIMENT_PROBABILITY);			
			specCreator.setType(DoubleCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());
			break;
		}
		case 8:{// Sentiment
			DataColumnSpecCreator specCreator = new DataColumnSpecCreator(SENTIMENT, StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			break;
		}
		default:
			LOGGER.info("Something very wrong happened here");
		}
		// Create and return a new DataTableSpec from the list of DataColumnSpecs.
		DataColumnSpec[] newColumnSpecsArray = newColumnSpecs.toArray(new DataColumnSpec[newColumnSpecs.size()]);
		return new DataTableSpec(newColumnSpecsArray);
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		/*
		 * Save user settings to the NodeSettings object. SettingsModels already know how to
		 * save them self to a NodeSettings object by calling the below method. In general,
		 * the NodeSettings object is just a key-value store and has methods to write
		 * all common data types. Hence, you can easily write your settings manually.
		 * See the methods of the NodeSettingsWO.
		 */
		emotionSettings.saveSettingsTo(settings);
		/*
		emotionFeatureSettings.saveSettingsTo(settings);
		emotionModelSettings.saveSettingsTo(settings);
		emotionHeaderSettings.saveSettingsTo(settings);
		*/
		emotionProbabilitySettings.saveSettingsTo(settings);

		sentimentSettings.saveSettingsTo(settings);
		/*
		sentimentFeatureSettings.saveSettingsTo(settings);
		sentimentModelSettings.saveSettingsTo(settings);
		sentimentHeaderSettings.saveSettingsTo(settings);
		*/
		sentimentProbabilitySettings.saveSettingsTo(settings);

		inputColumnSettings.saveSettingsTo(settings);
		
		missingValuesSettings.saveSettingsTo(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Load (valid) settings from the NodeSettings object. It can be safely assumed that
		 * the settings are validated by the method below.
		 * 
		 * The SettingsModel will handle the loading. After this call, the current value
		 * (from the view) can be retrieved from the settings model.
		 */		
		emotionSettings.loadSettingsFrom(settings);
		/*
		emotionFeatureSettings.loadSettingsFrom(settings);
		emotionModelSettings.loadSettingsFrom(settings);
		emotionHeaderSettings.loadSettingsFrom(settings);
		*/
		emotionProbabilitySettings.loadSettingsFrom(settings);

		sentimentSettings.loadSettingsFrom(settings);
		/*
		sentimentFeatureSettings.loadSettingsFrom(settings);
		sentimentModelSettings.loadSettingsFrom(settings);
		sentimentHeaderSettings.loadSettingsFrom(settings);
		*/
		sentimentProbabilitySettings.loadSettingsFrom(settings);

		inputColumnSettings.loadSettingsFrom(settings);
		
		missingValuesSettings.loadSettingsFrom(settings);

	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Check if the settings could be applied to our model e.g. if the user provided
		 * format String is empty. In this case we do not need to check as this is
		 * already handled in the dialog. Do not actually set any values of any member
		 * variables.
		 */
		emotionSettings.validateSettings(settings);
		/*
		emotionFeatureSettings.validateSettings(settings);
		emotionModelSettings.validateSettings(settings);
		emotionHeaderSettings.validateSettings(settings);
		*/
		emotionProbabilitySettings.validateSettings(settings);

		sentimentSettings.validateSettings(settings);
		/*
		sentimentFeatureSettings.validateSettings(settings);
		sentimentModelSettings.validateSettings(settings);
		sentimentHeaderSettings.validateSettings(settings);
		*/
		sentimentProbabilitySettings.validateSettings(settings);

		inputColumnSettings.validateSettings(settings);
		
		missingValuesSettings.validateSettings(settings);


	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything that is
		 * handed to the output ports is loaded automatically (data returned by the execute
		 * method, models loaded in loadModelContent, and user settings set through
		 * loadSettingsFrom - is all taken care of). Only load the internals
		 * that need to be restored (e.g. data used by the views).
		 */
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything
		 * written to the output ports is saved automatically (data returned by the execute
		 * method, models saved in the saveModelContent, and user settings saved through
		 * saveSettingsTo - is all taken care of). Save only the internals
		 * that need to be preserved (e.g. data used by the views).
		 */
	}

	@Override
	protected void reset() {
		/*
		 * Code executed on a reset of the node. Models built during execute are cleared
		 * and the data handled in loadInternals/saveInternals will be erased.
		 */
	}

	
	
	/*
	 * simple method that formats the output of the probability analysis
	 * @param Object [] the list of objects returned by the probability method
	 * @return String a well formatted string of probability label
	 */
	private Double format(Object s[]) {
		Double result = (Double)s[0];
		return result;
	}
	
	/*
	 * simple method that formats the output of the probability analysis
	 * @param Object [] the list of objects returned by the probability method
	 * @return String a well formatted string of probability label
	 *
	private String format(Object s[]) {
		String result = "";
		for(int i=0; i < s.length; i++) {
			result = result+" "+s[i].toString();
		}		
		return result;
	}
	*/
	
	
}



