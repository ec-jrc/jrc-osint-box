package com.jrc.tmacc.category;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is an example implementation of the node dialog of the
 * "CategoryMatcher" node.
 *
 * 
 * @author J.Psikoski and A.Caielli
 */
public class CategoryMatcherNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New dialog pane for configuring the node. The dialog created here
	 * will show up when double clicking on a node in KNIME Analytics Platform.
	 */
    @SuppressWarnings("unchecked")
	protected CategoryMatcherNodeDialog() {
        super();
        
        /*
		 * The DefaultNodeSettingsPane provides methods to add simple standard
		 * components to the dialog pane  
		 * The dialog components are connected to the node model via settings model
		 * objects that can easily load and save their settings to the node settings.
		*/      
        
        SettingsModelString inputOption = CategoryMatcherNodeModel.createInputOptionSettingsModel();	
        SettingsModelColumnName docColumn = CategoryMatcherNodeModel.createSettingsModelDocColumnSelection();
        SettingsModelColumnName textColumn = CategoryMatcherNodeModel.createSettingsModelTextColumnSelection();
        SettingsModelColumnName titleColumn = CategoryMatcherNodeModel.createSettingsModelTitleColumnSelection();
        SettingsModelColumnName langColumn = CategoryMatcherNodeModel.createSettingsModelLangColumnSelection();
        // triggers words
        SettingsModelBoolean triggersSettings = CategoryMatcherNodeModel.createTriggerSettings();
        // handle missing values
        SettingsModelBoolean missingValueSettings = CategoryMatcherNodeModel.createMissingValuesSettings();
        //Add event listeners
        inputOption.addChangeListener(new ChangeListener() {
		    public void stateChanged(final ChangeEvent e) {
		        // if enabled is true, the parameter field should be enabled
		        //parameter.setEnabled(enabled.getBooleanValue());
		    	String val = inputOption.getStringValue();
		    	if(val.equalsIgnoreCase(CategoryMatcherNodeModel.USE_INPUT_COLUMNS)) {
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
    	if(val.equalsIgnoreCase(CategoryMatcherNodeModel.INPUT_OPTION_DEFAULT)) {
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
        		false, "Select input type", CategoryMatcherNodeModel.USE_INPUT_COLUMNS, CategoryMatcherNodeModel.USE_DOCUMENT);                               
        addDialogComponent(rbg);
                	        
        createNewGroup("Document input");
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		docColumn,
                "Document column", 0, StringValue.class));

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
        
        createNewGroup("Triggers");
        addDialogComponent(new DialogComponentBoolean(triggersSettings, "Show trigger words"));
        closeCurrentGroup();
        addDialogComponent(new DialogComponentBoolean(missingValueSettings, "Insert missing value if no results"));      
        createNewGroup("Resources");
		SettingsModelString resourceDirSettings = CategoryMatcherNodeModel.createResourceDirSettingsModel();		
		DialogComponentFileChooser dcfc = new DialogComponentFileChooser(resourceDirSettings,
				"spark.file.writer", JFileChooser.OPEN_DIALOG, true);				
		dcfc.setBorderTitle("Resources directory");
		// text file
		addDialogComponent(dcfc);
		
		
		
		
    }

	@Override
	public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
			throws NotConfigurableException {
		// TODO Auto-generated method stub
		super.loadAdditionalSettingsFrom(settings, specs);
	}
}

