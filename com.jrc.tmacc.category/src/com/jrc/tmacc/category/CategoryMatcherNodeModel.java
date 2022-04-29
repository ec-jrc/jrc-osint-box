package com.jrc.tmacc.category;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
//import org.knime.core.data.xml.XMLCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.ext.textprocessing.data.Document;
//import org.knime.ext.textprocessing.data.DocumentCell;
import org.knime.ext.textprocessing.data.DocumentValue;

import com.google.gson.Gson;

import com.jrc.tmacc.category.vo.Category;
import com.jrc.tmacc.category.vo.CategoryMatcherResult;
import com.jrc.tmacc.category.vo.Trigger;

import it.jrc.alert.AlertManager;
import it.jrc.alert.ProcessRSS;

import it.jrc.rss.RSSItem;
import it.jrc.rss.SimpleElement;

/**
 * This is an example implementation of the node model of the "Nerone" node.
 * 
 * This example node performs simple number formatting
 * ({@link String#format(String, Object...)}) using a user defined format string
 * on all double columns of its input table.
 *
 * @author TMACC
 */
public class CategoryMatcherNodeModel extends NodeModel {

	
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CategoryMatcherNodeModel.class);

	/**
	 * The settings key to retrieve and store settings shared between node dialog
	 * and node model. In this case, the key for the number format String that
	 * should be entered by the user in the dialog.
	 */	
	
	private static final String KEY_RESOURCE_DIR = "resource_dir";
	private static final String DEFAULT_RESOURCE_DIR = "";
	private final SettingsModelString m_resourceDirSettings = createResourceDirSettingsModel();

	public static final String USE_DOCUMENT = "Use document";
	public static final String USE_INPUT_COLUMNS = "Use string input columns";

	final static String INPUT_OPTION_PROPERTY = "use_string_columns";
	final static String INPUT_OPTION_DEFAULT = "true";
	final SettingsModelString m_InputOptionSettings = createInputOptionSettingsModel();

	final static String DOC_COL_PROPERTY = "document";
	final static String DOC_COL_DEFAULT = "document";
	final SettingsModelColumnName m_DocColSettings = createSettingsModelDocColumnSelection();

	final static String LANG_COL_PROPERTY = "language";
	final static String LANG_COL_DEFAULT = "language";
	final SettingsModelColumnName m_LanguageColSettings = createSettingsModelLangColumnSelection();

	final static String TEXT_COL_PROPERTY = "text";
	final static String TEXT_COL_DEFAULT = "text";
	final SettingsModelColumnName m_TextColSettings = createSettingsModelTextColumnSelection();

	final static String TITLE_COL_PROPERTY = "title";
	final static String TITLE_COL_DEFAULT = "title";
	final SettingsModelColumnName m_TitleColSettings = createSettingsModelTitleColumnSelection();

	private static final String KEY_TRIGGER = "trigger";
	private static final Boolean DEFAULT_TRIGGER = true;
	private final SettingsModelBoolean m_trigger = createTriggerSettings();
	
	private static final String KEY_MISSING = "missing";
	private static final Boolean DEFAULT_MISSING = true;
	private final SettingsModelBoolean m_missing = createMissingValuesSettings();
	
	

	private boolean isXMLInput = false;
	private boolean isStringsInput = true;
	private boolean isDocumentInput = false;

	private AlertManager aman = null;
	private ProcessRSS prss = null;

	private String COL_ID = "id";
	protected static String COL_LANGUAGE = "language";
	// private String COL_TITLE = "title";
	// private String COL_TEXT = "text";
	// private String COL_NUM_ENTITES = "numberOfEntities";
	// private String COL_ENTITIES = "entities";
	// private String COL_RESULT = "CategoryMatcher result";

	/**
	 * Constructor for the node model.
	 */
	protected CategoryMatcherNodeModel() {
		/**
		 * Here we specify how many data input and output tables the node should have.
		 * In this case its one input and one output table.
		 */
		super(1, 1);

		// initial state
		String val = m_InputOptionSettings.getStringValue();
		if (val.equalsIgnoreCase(CategoryMatcherNodeModel.INPUT_OPTION_DEFAULT)) {
			m_DocColSettings.setEnabled(false);
			m_TextColSettings.setEnabled(true);
			m_TitleColSettings.setEnabled(true);
		} else {
			m_DocColSettings.setEnabled(true);
			m_TextColSettings.setEnabled(false);
			m_TitleColSettings.setEnabled(false);
		}

	}

	/**
	 * A convenience method to create a new settings model used for the number
	 * format String. This method will also be used in the {@link NeroneNodeDialog}.
	 * The settings model will sync via the above defined key.
	 * 
	 * @return a new SettingsModelString with the key for the number format String
	 */
	/*
	 * static SettingsModelString createNumberFormatSettingsModel() { return new
	 * SettingsModelString(KEY_NUMBER_FOMAT, DEFAULT_NUMBER_FORMAT); }
	 */
	/**
	 * A convenience method to create a new settings model used for the number
	 * format String. This method will also be used in the {@link NeroneNodeDialog}.
	 * The settings model will sync via the above defined key.
	 * 
	 * @return a new SettingsModelString with the key for the number format String
	 */
	static SettingsModelString createResourceDirSettingsModel() {
		return new SettingsModelString(KEY_RESOURCE_DIR, DEFAULT_RESOURCE_DIR);
	}

	static SettingsModelString createInputOptionSettingsModel() {
		return new SettingsModelString(INPUT_OPTION_PROPERTY, INPUT_OPTION_DEFAULT);
	}

	static SettingsModelColumnName createSettingsModelLangColumnSelection() {
		return new SettingsModelColumnName(CategoryMatcherNodeModel.LANG_COL_PROPERTY,
				CategoryMatcherNodeModel.LANG_COL_DEFAULT);
	}

	static SettingsModelColumnName createSettingsModelTextColumnSelection() {
		return new SettingsModelColumnName(CategoryMatcherNodeModel.TEXT_COL_PROPERTY,
				CategoryMatcherNodeModel.TEXT_COL_DEFAULT);
	}

	static SettingsModelColumnName createSettingsModelTitleColumnSelection() {
		return new SettingsModelColumnName(CategoryMatcherNodeModel.TITLE_COL_PROPERTY,
				CategoryMatcherNodeModel.TITLE_COL_DEFAULT);
	}

	static SettingsModelColumnName createSettingsModelDocColumnSelection() {
		return new SettingsModelColumnName(CategoryMatcherNodeModel.DOC_COL_PROPERTY,
				CategoryMatcherNodeModel.DOC_COL_DEFAULT);
	}

	// trigger words
	static SettingsModelBoolean createTriggerSettings() {
		return new SettingsModelBoolean(KEY_TRIGGER, DEFAULT_TRIGGER);
	}
	// handle missing values
	public static SettingsModelBoolean createMissingValuesSettings() {
		return new SettingsModelBoolean(KEY_MISSING, DEFAULT_MISSING);
	}


	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		// The functionality of the node is implemented in the execute method.

		// The input data table to work with.
		BufferedDataTable inputTable = inData[0];
		//use trigger words
		Boolean useTrigger = m_trigger.getBooleanValue();
		
		// Create the spec of the output table
		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(), useTrigger);

		/*
		 * The execution context provides storage capacity, in this case a data
		 * container to which we will add rows sequentially.
		 */
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		/*
		 * Get the row iterator over the input table which returns each row one-by-one
		 * from the input table.
		 */
		CloseableRowIterator rowIterator = inputTable.iterator();

		DataTableSpec inputSpec = inputTable.getDataTableSpec();

		String[] colNames = inputSpec.getColumnNames();
		
		LOGGER.info("Starting...");

		if (!isXMLInput) {
			// Check for expected columns
			// TODO maybe use
			// validateColumns(colNames);
		}

		if (!this.isXMLInput) {
			String resourcesDir = getFilePathFromUrlPath(m_resourceDirSettings.getStringValue(),true);
			System.out.println("Resources Directory = " + resourcesDir);
			this.aman = new AlertManager();
			aman.alertInit(resourcesDir);
			System.out.println("Working Directory = " + System.getProperty("user.dir"));
			prss = new ProcessRSS("", aman, null, null, false);
		}

		int docColIndex = -1;
		if (this.isDocumentInput) {
			String colName = m_DocColSettings.getStringValue(); // "Document";
			docColIndex = inputSpec.findColumnIndex(colName);
		}

		String rDir = m_resourceDirSettings.getStringValue();
		System.out.println("USER rDir: " + rDir);
		String inputOption = m_InputOptionSettings.getStringValue();
		System.out.println("USER inputOption: " + inputOption);
		String docColName = m_DocColSettings.getColumnName();
		System.out.println("docColName: " + docColName);
		String langColName = m_LanguageColSettings.getColumnName();
		System.out.println("langColName: " + langColName);
		String textColName = m_TextColSettings.getColumnName();
		System.out.println("textColName: " + textColName);		
		String titleColName = m_TitleColSettings.getColumnName();
		System.out.println("titleColName: " + titleColName);

		int lanColIndex = inputSpec.findColumnIndex(this.m_LanguageColSettings.getStringValue());
		System.out.println("lanColIndex: " + lanColIndex);

		Gson gson = new Gson();

		/*
		 * A counter for how many rows have already been processed. This is used to
		 * calculate the progress of the node, which is displayed as a loading bar under
		 * the node icon.
		 */
		int currentRowCounter = 0;
		// Iterate over the rows of the input table.
		while (rowIterator.hasNext()) {
			DataRow currentRow = rowIterator.next();			
			int numberOfCells = currentRow.getNumCells();
			/*
			 * A list to collect the cells to output for the current row. The type and
			 * amount of cells must match the DataTableSpec we used when creating the
			 * DataContainer.
			 */
			List<DataCell> cells = new ArrayList<>();

			RSSItem rssItem = new RSSItem();

			// Iterate over the cells of the current row.
			for (int i = 0; i < numberOfCells; i++) {
				DataCell cell = currentRow.getCell(i);
				cells.add(cell);

				if (!cell.isMissing()) {

					String colName = colNames[i];

					if (this.isDocumentInput) {						
						if (cell != null) {

							if (docColIndex >= 0) {
								if (i == docColIndex) {
									DocumentValue dv = (DocumentValue) cell;
									Document d = dv.getDocument();

									if (d != null) {
										String title = d.getTitle();
										rssItem.setTitle(title);
										String text = d.getDocumentBodyText();
										rssItem.setText(text);
										UUID docId = d.getUUID();
										rssItem.setAttribute("guid", docId.toString());
									}
									// Add document
									//////// cells.add(cell);

								} else if (cell.getType().getCellClass().equals((StringCell.class))) {
									// lang must still be passed in - can use Tika Language Detector to get it
									// from a Document

									if (colName.equalsIgnoreCase(langColName)) { // COL_LANGUAGE
										StringCell sc = (StringCell) cell;
										String val = sc.getStringValue();
										System.out.println("CELL VALUE: " + val);
										rssItem.setAttribute("iso:language", val);
										rssItem.setLanguage(val);
										/////// cells.add(cell);
									}
								}
							}

							if (cell.getType().getCellClass().equals((StringCell.class))) {
								System.out.println("STRING CELL CLASS NAME: " + cell.getType().getCellClass());
							} else {
								System.out.println("DOCUMENT CELL CLASS NAME: " + cell.getType());
								System.out.println("DOCUMENT CELL CLASS NAME: " + cell.getType().getCellClass());

								if (cell.getType().getCellClass().equals((StringCell.class))) {
									//lang must still be passed in - can use Tika Language Detector to get it
									// from a Document
									if (colName.equalsIgnoreCase(COL_LANGUAGE)) {
										StringCell sc = (StringCell) cell;
										String val = sc.getStringValue();
										System.out.println("CELL VALUE: " + val);
										rssItem.setAttribute("iso:language", val);
										rssItem.setLanguage(val);
									}
								}

							}
						}

					} else if (this.isStringsInput) {					
						if (cell.getType().getCellClass().equals((StringCell.class))) {
							StringCell sc = (StringCell) cell;
							String val = sc.getStringValue();
							System.out.println("CELL VALUE: " + val);

							if (colName.equalsIgnoreCase(COL_ID)) {
								rssItem.setAttribute("guid", val);
							} else if (colName.equalsIgnoreCase(langColName)) {
								rssItem.setAttribute("iso:language", val);
								rssItem.setLanguage(val);
							} else if (colName.equalsIgnoreCase(titleColName)) {
								rssItem.setTitle(val);
							} else if (colName.equalsIgnoreCase(textColName)) {
								rssItem.setText(val);
							}
						}
					}
				}
			}			
			
			LOGGER.info("Extracting Categories");
			prss.processItem(rssItem);

			CategoryMatcherResult catMatcherResult = new CategoryMatcherResult();

			catMatcherResult.setLanguage(rssItem.getLanguage());
			catMatcherResult.setPubDate(rssItem.getPubDate());
			catMatcherResult.setWordCount(rssItem.getWordCount());
			catMatcherResult.setDate(rssItem.getDate().toString());
			catMatcherResult.setName(rssItem.getName());
			catMatcherResult.setText(rssItem.getText());
			
			ArrayList<Category> catObs = new ArrayList<Category>();

			System.out.println("======================================");
			System.out.println("RESULT: " + rssItem);

			System.out.println("LANGUAGE: " + rssItem.getLanguage());
			System.out.println("PUB DATE: " + rssItem.getPubDate());
			System.out.println("DATE: " + rssItem.getDate());
			System.out.println("TITLE: " + rssItem.getTitle());
			System.out.println("DESC: " + rssItem.getDescription());
			System.out.println("WORD COUNT: " + rssItem.getWordCount());
			System.out.println("TEXT: " + rssItem.getText());

			String cats = rssItem.getCategories().toString();
			System.out.println("cats: " + cats);

			
			ArrayList<SimpleElement> catEls = rssItem.getCategoryElements();
			int count = 0;
			for (SimpleElement cat : catEls) {
				count++;
				System.out.println("================CAT======================");
				System.out.println("CAT NAME: " + cat.getName());
				System.out.println("CAT VALUE: " + cat.getValue());
				String rank = cat.getAttributeValue("emm:rank");
				String score = cat.getAttributeValue("emm:score");
				String triggers = cat.getAttributeValue("emm:trigger");

				System.out.println("rank: " + rank);
				System.out.println("score: " + score);
				System.out.println("triggers: " + triggers);

				ArrayList<Trigger> trigList = new ArrayList<Trigger>();

				if (triggers != null) {
					if (triggers.indexOf(";") != -1) {

						String arr[] = triggers.split(";");
						for (int i = 0; i < arr.length; i++) {
							System.out.println("trigger: " + arr[i]);
							String trig = arr[i];
							if (trig.indexOf("[") != -1) {
								if (trig.endsWith("]")) {
									trig = trig.substring(0, trig.length() - 1);
									String tarr[] = trig.split("\\[");
									Trigger t = new Trigger();
									t.setText(tarr[0]);
									t.setCount(tarr[1]);
									System.out.println("text: " + tarr[0] + " count: " + tarr[1]);
									trigList.add(t);									
								}
							}
						}
					}
				}

				Category category = new Category();
				category.setRank(rank);
				category.setScore(score);
				category.setValue(cat.getValue());
				
				category.setTriggers(trigList);
				catObs.add(category);
				// we need to create a new row for each category found
				// add the category
				cells.add(new StringCell(cat.getValue()));
				// add trigger words
				if (useTrigger) {					
					String[] content = new String[trigList.size()];
					for(int i = 0; i < content.length; i++) {
						content[i]= trigList.get(i).getText(); 						
					}
					int[] cols = new int[trigList.size()];
					for(int i = 0; i < cols.length; i++) {
						cols[i]= i; 
					}
					DataRow row = new DefaultRow("tmp", content);
					cells.add(CollectionCellFactory.createListCell(row, cols));
				}
				// save the new row			
				DataRow row = new DefaultRow(currentRow.getKey()+"-"+count, cells);						
				container.addRowToTable(row);
				//remove the last cells
				cells.remove(cells.size()-1);
				if(useTrigger) {
					cells.remove(cells.size()-1);	
				}
			} // endfor
			
			if (catEls.size() == 0) {	
				if(m_missing.getBooleanValue()) {
					cells.add(new MissingCell(""));
					if(useTrigger) {
						cells.add(new MissingCell(""));
					}
				}
				else {
					cells.add(new StringCell(""));
					if(useTrigger) {
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
					}
				}
				DataRow row = new DefaultRow(currentRow.getKey(), cells);				
				container.addRowToTable(row);
			}

			

			catMatcherResult.setCategories(catObs);

			String resultJson = gson.toJson(catMatcherResult);
			System.out.println("RESULT : " + resultJson);
			// }

			/*
			 * //Add JSON entities cells.add(new StringCell(resultJson));
			 * 
			 * //NEW
			 * 
			 * 
			 * String jsonEntities = gson.toJson(entityObs);
			 * System.out.println("ENTITIES JSON: " + jsonEntities);
			 * 
			 * //Add JSON entities cells.add(new StringCell(jsonEntities));
			 * 
			 * 
			 * // Add the new row to the output data container DataRow row = new
			 * DefaultRow(currentRow.getKey(), cells); 
			 * container.addRowToTable(row);
			 */

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
			 * ExecutionMonitor. Additionally, we can set a message what the node is
			 * currently doing (the message will be displayed as a tooltip when hovering
			 * over the progress bar of the node). This is especially useful to inform the
			 * user about the execution status for long running nodes.
			 */
			exec.setProgress(currentRowCounter / (double) inputTable.size(), "Formatting row " + currentRowCounter);
		}

		/*
		 * Once we are done, we close the container and return its table. Here we need
		 * to return as many tables as we specified in the constructor. This node has
		 * one output, hence return one table (wrapped in an array of tables).
		 */
		container.close();
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		/*
		 * Check if the node is executable, e.g. all required user settings are
		 * available and valid, or the incoming types are feasible for the node to
		 * execute. In case the node can execute in its current configuration with the
		 * current input, calculate and return the table spec that would result of the
		 * execution of this node. I.e. this method precalculates the table spec of the
		 * output table.
		 */

		String resourcesDir = m_resourceDirSettings.getStringValue();
		System.out.println("USER resourcesDir: " + resourcesDir);

		String inOpts = m_InputOptionSettings.getStringValue();
		System.out.println("USER inOpts: " + inOpts);

		if (inOpts.equalsIgnoreCase(CategoryMatcherNodeModel.USE_DOCUMENT)) {
			this.isDocumentInput = true;
			this.isStringsInput = false;
		} else {
			this.isDocumentInput = false;
			this.isStringsInput = true;
		}

		String docCol = m_DocColSettings.getStringValue();
		System.out.println("Document column: " + docCol);		

		String lang = m_LanguageColSettings.getStringValue();
		System.out.println("Language column: " + lang);

		String text = m_TextColSettings.getStringValue();
		System.out.println("Text column: " + text);

		String title = m_TitleColSettings.getStringValue();
		System.out.println("Title column: " + title);

		/*
		 * Similar to the return type of the execute method, we need to return an array
		 * of DataTableSpecs with the length of the number of outputs ports of the node
		 * (as specified in the constructor). The resulting table created in the execute
		 * methods must match the spec created in this method. As we will need to
		 * calculate the output table spec again in the execute method in order to
		 * create a new data container, we create a new method to do that.
		 */
		DataTableSpec inputTableSpec = inSpecs[0];
		return new DataTableSpec[] { createOutputSpec(inputTableSpec, m_trigger.getBooleanValue()) };
	}

	/**
	 * Creates the output table spec from the input spec. For each double column in
	 * the input, one String column will be created containing the formatted double
	 * value as String.
	 * 
	 * @param inputTableSpec
	 * @return
	 */
	private DataTableSpec createOutputSpec(DataTableSpec inputTableSpec, Boolean option) {

		List<DataColumnSpec> newColumnSpecs = new ArrayList<>();

		// save all the inputs
		for (int i = 0; i < inputTableSpec.getNumColumns(); i++) {
			newColumnSpecs.add(inputTableSpec.getColumnSpec(i));
		}
		// add the category column
		DataColumnSpecCreator specCreator = new DataColumnSpecCreator("Categories", StringCell.TYPE);
		newColumnSpecs.add(specCreator.createSpec());
		// if triggers word are required, add a new column
		if (option) {
			specCreator.setName("Trigger words");
			specCreator.setType(ListCell.getCollectionType(StringCell.TYPE));
			newColumnSpecs.add(specCreator.createSpec());
		}		
		// Create and return a new DataTableSpec from the list of DataColumnSpecs.
		DataColumnSpec[] newColumnSpecsArray = newColumnSpecs.toArray(new DataColumnSpec[newColumnSpecs.size()]);
		return new DataTableSpec(newColumnSpecsArray);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		/*
		 * Save user settings to the NodeSettings object. SettingsModels already know
		 * how to save them self to a NodeSettings object by calling the below method.
		 * In general, the NodeSettings object is just a key-value store and has methods
		 * to write all common data types. Hence, you can easily write your settings
		 * manually. See the methods of the NodeSettingsWO.
		 */
		// m_numberFormatSettings.saveSettingsTo(settings);
		m_LanguageColSettings.saveSettingsTo(settings);
		m_TextColSettings.saveSettingsTo(settings);
		m_TitleColSettings.saveSettingsTo(settings);
		m_resourceDirSettings.saveSettingsTo(settings);
		m_InputOptionSettings.saveSettingsTo(settings);
		m_DocColSettings.saveSettingsTo(settings);
		m_trigger.saveSettingsTo(settings);
		m_missing.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Load (valid) settings from the NodeSettings object. It can be safely assumed
		 * that the settings are validated by the method below.
		 * 
		 * The SettingsModel will handle the loading. After this call, the current value
		 * (from the view) can be retrieved from the settings model.
		 */
		// m_numberFormatSettings.loadSettingsFrom(settings);
		m_InputOptionSettings.loadSettingsFrom(settings);
		m_LanguageColSettings.loadSettingsFrom(settings);
		m_TextColSettings.loadSettingsFrom(settings);
		m_TitleColSettings.loadSettingsFrom(settings);
		m_resourceDirSettings.loadSettingsFrom(settings);
		m_DocColSettings.loadSettingsFrom(settings);
		m_trigger.loadSettingsFrom(settings);
		m_missing.loadSettingsFrom(settings);

		// initial state
		String val = m_InputOptionSettings.getStringValue();
		if (val.equalsIgnoreCase(CategoryMatcherNodeModel.USE_INPUT_COLUMNS)) {
			m_DocColSettings.setEnabled(false);
			m_TextColSettings.setEnabled(true);
			m_TitleColSettings.setEnabled(true);
		} else {
			m_DocColSettings.setEnabled(true);
			m_TextColSettings.setEnabled(false);
			m_TitleColSettings.setEnabled(false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Check if the settings could be applied to our model e.g. if the user provided
		 * format String is empty. In this case we do not need to check as this is
		 * already handled in the dialog. Do not actually set any values of any member
		 * variables.
		 */
		// m_numberFormatSettings.validateSettings(settings);

		m_LanguageColSettings.validateSettings(settings);
		m_TextColSettings.validateSettings(settings);
		m_TitleColSettings.loadSettingsFrom(settings);
		m_resourceDirSettings.validateSettings(settings);
		m_InputOptionSettings.validateSettings(settings);
		m_DocColSettings.validateSettings(settings);
		m_trigger.validateSettings(settings);
		m_missing.validateSettings(settings);
		/*
		 * SettingsModelString classifyColumn =
		 * m_LanguageColSettings.createCloneWithValidatedValue(settings); String langVal
		 * = classifyColumn.getStringValue(); if (langVal == null || langVal.equals(""))
		 * { throw new InvalidSettingsException("Language column not set."); }
		 */

	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything that is handed to the output
		 * ports is loaded automatically (data returned by the execute method, models
		 * loaded in loadModelContent, and user settings set through loadSettingsFrom -
		 * is all taken care of). Only load the internals that need to be restored (e.g.
		 * data used by the views).
		 */
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		/*
		 * Advanced method, usually left empty. Everything written to the output ports
		 * is saved automatically (data returned by the execute method, models saved in
		 * the saveModelContent, and user settings saved through saveSettingsTo - is all
		 * taken care of). Save only the internals that need to be preserved (e.g. data
		 * used by the views).
		 */
	}

	@Override
	protected void reset() {
		/*
		 * Code executed on a reset of the node. Models built during execute are cleared
		 * and the data handled in loadInternals/saveInternals will be erased.
		 */
	}
	
	/**
	 * Get the file path from the knime url path.
	 * 
	 * @param urlPath the url path
	 * @param mustExist if the file on the url path must exist
	 * @return the file path
	 * @throws Exception 
	 */
	static String getFilePathFromUrlPath(String urlPath, boolean mustExist) throws Exception {
		if (urlPath.trim().isEmpty()) {
			throw new Exception("No file selected");
		}
        
        try {
        	Path filePath = FileUtil.resolveToPath(FileUtil.toURL(urlPath));
        	if (mustExist || filePath.toFile().exists()) {
            	//CheckUtils.checkSourceFile(filePath.toString());
        		CheckUtils.checkDestinationDirectory(filePath.toString());
            } else {
            	CheckUtils.checkDestinationDirectory(filePath.getParent().toString());
            }
            
            return filePath.toString();
            
        } catch(Exception e) {
        	throw new Exception(e.getLocalizedMessage());
        }
	}

	
}
