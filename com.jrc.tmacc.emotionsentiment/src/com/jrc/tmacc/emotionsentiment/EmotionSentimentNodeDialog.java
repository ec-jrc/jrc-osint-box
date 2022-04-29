package com.jrc.tmacc.emotionsentiment;

import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is an example implementation of the node dialog of the
 * "ProvaSentiment" node.
 *
 * 
 * @author me
 */
public class EmotionSentimentNodeDialog extends DefaultNodeSettingsPane {
	/*
	private static final String FEATURE_EXTENSION = ".txt";
	private static final String HEADER_EXTENSION = ".arff";
	private static final String MODEL_EXTENSION = ".model";
	*/
	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    @SuppressWarnings("unchecked")
	protected EmotionSentimentNodeDialog() {
        super();
        // FOR NODE SEMPLIFICATION, MODEL FILES ARE HARDCODED IN THE MODEL FILE
        /**
		 * Here we create a set of DialogComponent objects that will allow the user
		 * to set the execution of the node with all the proper settings.
		 * The DialogComponentString has additional options to disallow empty inputs, hence
		 * we do not need to worry about that in the model implementation anymore.
		 * An additional analysis of the inputs is performed in the MoodClassifier implementation 
		 */
		// First, create an appropriate set of settings model using the create method from the node model.
        // Emotion
		SettingsModelBoolean emotionSettings = EmotionSentimentNodeModel.createEmotionSettingsModel();
		/*
		SettingsModelString emotionFeatureSettings = ProvaSentimentNodeModel.createEmotionFeatureSettingsModel();
		SettingsModelString emotionModelSettings = ProvaSentimentNodeModel.createEmotionModelSettingsModel();
		SettingsModelString emotionHeaderSettings = ProvaSentimentNodeModel.createEmotionHeaderSettingsModel();
		*/
		SettingsModelBoolean emotionProbabilitySettings = EmotionSentimentNodeModel.createEmotionProbabilitySettingsModel();
		// Sentiment
		SettingsModelBoolean sentimentSettings = EmotionSentimentNodeModel.createSentimentSettingsModel();
		/*
		SettingsModelString sentimentFeatureSettings = ProvaSentimentNodeModel.createSentimentFeatureSettingsModel();
		SettingsModelString sentimentModelSettings = ProvaSentimentNodeModel.createSentimentModelSettingsModel();
		SettingsModelString sentimentHeaderSettings = ProvaSentimentNodeModel.createSentimentHeaderSettingsModel();
		*/
		SettingsModelBoolean sentimentProbabilitySettings = EmotionSentimentNodeModel.createSentimentProbabilitySettingsModel();
		// Input column
		SettingsModelString inputColumnSettings = EmotionSentimentNodeModel.createInputColumnSettingsModel();
		SettingsModelBoolean missingValueSettings = EmotionSentimentNodeModel.createMissingValuesSettingsModel();
		
		// Now we can create the whole list of dialog components
		// The order here is the same order in which they will be shown to the user
		// create the first group for Emotion
		createNewGroup("Emotion");
		// tick input
		addDialogComponent(new DialogComponentBoolean(emotionSettings, "Perform Emotion Analysis"));
		//file selection
		/*
		 * we had to use 6 different components because I found no way to alter the settings
		 * of an existing FileChooser 
		 */
		/*
		DialogComponentFileChooser dialog1 = new DialogComponentFileChooser(
				emotionFeatureSettings,"",FEATURE_EXTENSION);
		dialog1.setBorderTitle("Enter the path for the weka feature file :");
		addDialogComponent(dialog1);		
		DialogComponentFileChooser dialog2 = new DialogComponentFileChooser(
				emotionModelSettings,"",MODEL_EXTENSION);
		dialog2.setBorderTitle("Enter the path for the weka model file :");
		addDialogComponent(dialog2);		
		DialogComponentFileChooser dialog3 = new DialogComponentFileChooser(
				emotionHeaderSettings,"",HEADER_EXTENSION);
		dialog3.setBorderTitle("Enter the path for the weka header file :");
		addDialogComponent(dialog3);		
		*/	
		// tick input
		addDialogComponent(new DialogComponentBoolean(emotionProbabilitySettings, "Show emotion score"));
		// create a group for emotion
		createNewGroup("Sentiment");
		// tick input
		addDialogComponent(new DialogComponentBoolean(sentimentSettings, "Perform Sentiment Analysis"));
		// slider menu for file selection
		//file selection
		/*
		DialogComponentFileChooser dialog4 = new DialogComponentFileChooser(
				sentimentFeatureSettings,"",FEATURE_EXTENSION);
		dialog4.setBorderTitle("Enter the path for the weka feature file :");
		addDialogComponent(dialog4);		
		DialogComponentFileChooser dialog5 = new DialogComponentFileChooser(
				sentimentModelSettings,"",MODEL_EXTENSION);
		dialog5.setBorderTitle("Enter the path for the weka model file :");
		addDialogComponent(dialog5);		
		DialogComponentFileChooser dialog6 = new DialogComponentFileChooser(
				sentimentHeaderSettings,"",HEADER_EXTENSION);
		dialog6.setBorderTitle("Enter the path for the weka header file :");
		addDialogComponent(dialog6);
		*/	
		// tick menu
		addDialogComponent(new DialogComponentBoolean(sentimentProbabilitySettings, "Show sentiment score"));
		createNewGroup("Input (only English, French, German, Italian and Spanish supported)");		
		// slide menu to select the input column. Only String columns are accepted
	    addDialogComponent(new DialogComponentColumnNameSelection(
	                inputColumnSettings,"Select the column to be analyzed: ", 0, true, 
	                StringValue.class, StringCell.class));
	    closeCurrentGroup();
	    addDialogComponent(new DialogComponentBoolean(missingValueSettings, "Insert missing value if no results"));
	    
    }
}

