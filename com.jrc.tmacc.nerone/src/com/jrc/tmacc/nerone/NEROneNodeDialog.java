package com.jrc.tmacc.nerone;

//import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
//import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import org.knime.ext.textprocessing.data.DocumentValue;



/**
 * This is an example implementation of the node dialog of the
 * "Nerone" node.
 * 
 * @author TMACC
 */
public class NEROneNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    @SuppressWarnings("unchecked")
	protected NEROneNodeDialog() {
        super();
        
        /*
		 * The DefaultNodeSettingsPane provides methods to add simple standard
		 * components to the dialog pane via the addDialogComponent(...) method. This
		 * method expects a new DialogComponet object that should be added to the dialog
		 * pane. There are many already predefined components for the most commonly used
		 * configuration needs like a text box (DialogComponentString) to enter some
		 * String or a number spinner (DialogComponentNumber) to enter some number in a
		 * specific range and step size.
		 * 
		 * The dialog components are connected to the node model via settings model
		 * objects that can easily load and save their settings to the node settings.
		 * Depending on the type of input the dialog component should receive, the
		 * constructor of the component requires a suitable settings model object. E.g.
		 * the DialogComponentString requires a SettingsModelString. Additionally,
		 * dialog components sometimes allow to further configure the behavior of the
		 * component in the constructor. E.g. to disallow empty inputs (like below).
		 * Here, the loading/saving in the dialog is already taken care of by the
		 * DefaultNodeSettingsPane. It is important to use the same key for the settings
		 * model here as used in the node model implementation (it does not need to be
		 * the same object). One best practice is to use package private static methods
		 * to create the settings model as we did in the node model implementation (see
		 * createNumberFormatSettingsModel() in the NodeModel class).
		 * 
		 * Here we create a simple String DialogComponent that will display a label
		 * String besides a text box in which the use can enter a value. The
		 * DialogComponentString has additional options to disallow empty inputs, hence
		 * we do not need to worry about that in the model implementation anymore.
		 * 
		 */

        
        SettingsModelString inputOption = NEROneNodeModel.createInputOptionSettingsModel();	
        SettingsModelColumnName docColumn = NEROneNodeModel.createSettingsModelDocColumnSelection();
        SettingsModelColumnName textColumn = NEROneNodeModel.createSettingsModelTextColumnSelection();
        SettingsModelColumnName titleColumn = NEROneNodeModel.createSettingsModelTitleColumnSelection();
        SettingsModelColumnName langColumn = NEROneNodeModel.createSettingsModelLangColumnSelection();
        SettingsModelBoolean missingValueSettings = NEROneNodeModel.createMissingValuesSettings();
        SettingsModelBoolean includeJSONSettings = NEROneNodeModel.createIncludeJSONSettings();
        
        //Add event listeners
        inputOption.addChangeListener(new ChangeListener() {
		    public void stateChanged(final ChangeEvent e) {
		        // if enabled is true, the parameter field should be enabled
		        //parameter.setEnabled(enabled.getBooleanValue());
		    	String val = inputOption.getStringValue();
		    	if(val.equalsIgnoreCase(NEROneNodeModel.USE_INPUT_COLUMNS)) {
		    		docColumn.setEnabled(false);
		    		textColumn.setEnabled(true);
		    		titleColumn.setEnabled(true);
		    	}else {
		    		docColumn.setEnabled(true);
		    		textColumn.setEnabled(false);
		    		titleColumn.setEnabled(false);
		    	}
		    }
		});
        
        //initial state        
        String val = inputOption.getStringValue();
    	if(val.equalsIgnoreCase(NEROneNodeModel.INPUT_OPTION_DEFAULT)) {
    		docColumn.setEnabled(false);
    		textColumn.setEnabled(true);
    		titleColumn.setEnabled(true);
    	}else {
    		docColumn.setEnabled(true);
    		textColumn.setEnabled(false);
    		titleColumn.setEnabled(false);
    	}
        
        
        createNewGroup("Input option");
        DialogComponentButtonGroup rbg = new DialogComponentButtonGroup(
        		inputOption, 
        		false, "Select input type", NEROneNodeModel.USE_INPUT_COLUMNS, NEROneNodeModel.USE_DOCUMENT);                               
        addDialogComponent(rbg);
                	        
        createNewGroup("Document input");
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		docColumn,
                "Document column", 0, StringValue.class,DocumentValue.class));

        createNewGroup("String input");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		textColumn,
                "Text column", 0, StringValue.class));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
                titleColumn,
                "Title column", 0, StringValue.class));
        
        createNewGroup("Language");
        addDialogComponent(new DialogComponentColumnNameSelection(
                langColumn,
                "Language column", 0, StringValue.class));
        closeCurrentGroup();
        createNewGroup("Output");
        addDialogComponent(new DialogComponentBoolean(missingValueSettings, "Insert missing value if no results"));  	
        addDialogComponent(new DialogComponentBoolean(includeJSONSettings, "Include advanced JSON output"));  	
       
        /*createNewGroup("Resources");
		SettingsModelString resourceDirSettings = NEROneNodeModel.createResourceDirSettingsModel();		
		DialogComponentFileChooser dcfc = new DialogComponentFileChooser(resourceDirSettings,
				"spark.file.writer", JFileChooser.OPEN_DIALOG, true);				
		dcfc.setBorderTitle("Resources directory");
		// text file
		addDialogComponent(dcfc);
		*/
		
    }
}

