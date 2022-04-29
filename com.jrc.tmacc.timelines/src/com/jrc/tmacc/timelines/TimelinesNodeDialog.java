package com.jrc.tmacc.timelines;

//import java.awt.FlowLayout;

import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is an example implementation of the node dialog of the
 * "Timelines" node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}. In general, one can create an
 * arbitrary complex dialog using Java Swing.
 * 
 * @author Andrea Caielli
 */
public class TimelinesNodeDialog extends DefaultNodeSettingsPane {

	private static final String ENTITIES_EXTENSION = ".txt";


	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 * The dialog pane will contain only two components:
	 * 1- one file selector for the configuration file
	 * 2- one file selector for the target entities profile definition file
	 */
	@SuppressWarnings("unchecked")
	protected TimelinesNodeDialog() {
		super();

		/*
		 * The DefaultNodeSettingsPane provides methods to add simple standard
		 * components to the dialog pane 		
		 * It is important to use the same key for the settings model here as used in
		 *  the node model implementation 
		 */
		// First, create new settings models using the create method from the node model.
		SettingsModelString entitiesProfileSettings = TimelinesNodeModel.createEntitiesProfileSettingsModel();
		SettingsModelString inputColumnSettings = TimelinesNodeModel.createInputColumnSettingsModel();
		SettingsModelBoolean missingValueSettings = TimelinesNodeModel.createMissingValuesSettingsModel();
		/*
		# locale
		# using ISO639-1 encoding  
		#
		LOCALE=en
		 */
		SettingsModelString configuration1Settings = TimelinesNodeModel.createConfiguration1Model();
		/*
		# specify whether extended matching will 
		# be used to match an expanded version of the match
		# e.g., "The Trump Organization" instead of "Trump"
		#
		ENTITY.MATCHER.EXTENDEDMATCHING=true
		 */
		SettingsModelBoolean configuration2Settings = TimelinesNodeModel.createConfiguration2Model();
		/*
		# specify whether fuzzy matching will 
		# be used to match additional mentions
		ENTITY.MATCHER.FUZZYMATCHING=true
		 */
		SettingsModelBoolean configuration3Settings = TimelinesNodeModel.createConfiguration3Model();
		/*
		# specify the string distance metric to be used
		# any of the string distance metrics in CORLEONE can be used here
		#ENTITY.MATCHER.FUZZYMATCHING.METRIC
        Levenshtein, MongeElkan, LongestCommonSubstrings, WeightedLongestCommonSubstrings
		 */
		SettingsModelString configuration4Settings = TimelinesNodeModel.createConfiguration4Model();
		/*
		# minimum distance to consider a fuzzy match as a mention of the target entity
		ENTITY.MATCHER.FUZZYMATCHING.MINDIST=0.25
		 */
		SettingsModelDoubleBounded configuration5Settings = TimelinesNodeModel.createConfiguration5Model();
		/*
		# Anaphora resolver (specify whether it is used or not)
		ANAPHORA.RESOLUTION=true
		 */
		SettingsModelBoolean configuration6Settings = TimelinesNodeModel.createConfiguration6Model();
		/*
		# for applying additional heuristics to filter out "guessed" entities
		ENTITY.RECOGNIZER.GUESSINGHEURISTICS=true
		 */
		SettingsModelBoolean configuration7Settings = TimelinesNodeModel.createConfiguration7Model();
		/*# consider trimming first item in the sentence 
        ENTITY.RECOGNIZER.GUESSINGHEURISTICS.SINGLETOKENS=true
        */
		SettingsModelBoolean configuration8Settings = TimelinesNodeModel.createConfiguration8Model();
		/*
		# four options available (description in code):
		#
		# none - no duplicate removal will be cared out!
		# simple 
		# simple-aggressive
		# moderate
		# complex
		#
		EVENT.DUPLICATE.REMOVAL.STRATEGY=complex
		 */
		SettingsModelString configuration9Settings = TimelinesNodeModel.createConfiguration9Model();
		/*
		# apply POS Tagger to discard unplausible event groups	 
		# (setting this option to false will decrease the precision, and increase the recall)
		EVENT.MATCHER.POS.TAGGER.FILTER=true
		 */
		SettingsModelBoolean configuration10Settings = TimelinesNodeModel.createConfiguration10Model();		
		
		SettingsModelBoolean includeJSONSettings = TimelinesNodeModel.createIncludeJSONSettings();
		SettingsModelBoolean includeTargetEntitySettings = TimelinesNodeModel.createIncludeTargetEntitySettings();

		// Add the new components to the dialog.	
		createNewGroup("Input/Output Configurations"); 
		DialogComponentFileChooser dialog2 = new DialogComponentFileChooser(
				entitiesProfileSettings,"",ENTITIES_EXTENSION);
		dialog2.setBorderTitle("Enter the path for the Entities Profile file:");
		addDialogComponent(dialog2);
		addDialogComponent(new DialogComponentColumnNameSelection(
				inputColumnSettings,"Select the column to be analyzed: ", 0, true, 
				StringValue.class, StringCell.class));
		addDialogComponent(new DialogComponentBoolean(missingValueSettings, "Insert missing value if no results"));
		addDialogComponent(new DialogComponentBoolean(includeJSONSettings, "Include advanced JSON output"));
		addDialogComponent(new DialogComponentBoolean(includeTargetEntitySettings, "Include Target Entity"));

		createNewGroup("Event Extraction Configurations (default value)"); 		
		addDialogComponent(new DialogComponentBoolean(configuration2Settings,
				"Specify whether extended matching will be used to match an expanded version of the match (true) "));
		addDialogComponent(new DialogComponentBoolean(configuration6Settings,
				"Anaphora resolver (true)                                                                                              "));
		addDialogComponent(new DialogComponentBoolean(configuration7Settings,
				"Specify whether applying additional heuristics to filter out \"guessed\" entities (true)                     "));
		addDialogComponent(new DialogComponentBoolean(configuration8Settings,
				"Specify whether trimming first item in the sentence (true)                                                "));
		addDialogComponent(new DialogComponentBoolean(configuration10Settings,
				"Apply POS Tagger to discard unplausible event groups (true)                                             "));
		addDialogComponent(new DialogComponentBoolean(configuration3Settings,  
				"Specify whether fuzzy matching will be used by the Entity Matcher (true)                                "));
		addDialogComponent(new DialogComponentNumber(configuration5Settings,
				"Maximum distance to consider a fuzzy match as a mention of the target entity (0.25):", 0.05));
		addDialogComponent(new DialogComponentStringSelection(configuration4Settings,
				"Specify the string distance metric to be used (WeightedLongestCommonSubstrings):",
				"Levenshtein", "MongeElkan", "LongestCommonSubstrings", "WeightedLongestCommonSubstrings"));
		addDialogComponent(new DialogComponentStringSelection(configuration9Settings,
				"Specify the event duplicate removal strategy (complex): ",
				"none", "simple", "simple-aggressive", "moderate","complex"));		
		addDialogComponent(new DialogComponentStringSelection(configuration1Settings,
				"Language:", "en", "fr", "de","es","it"));

	}
}

