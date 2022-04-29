package com.jrc.tmacc.nerone;


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
import org.knime.ext.textprocessing.data.DocumentValue;

import com.google.gson.Gson;
import com.jrc.tmacc.nerone.vo.Entity;
import com.jrc.tmacc.nerone.vo.FullGeo;
import com.jrc.tmacc.nerone.vo.GeoRSS;
import com.jrc.tmacc.nerone.vo.Guess;
import com.jrc.tmacc.nerone.vo.NEROResult;

import it.jrc.nerone.NEROne;
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
public class NEROneNodeModel extends NodeModel {

	/**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(NEROneNodeModel.class);	
	/**
	 * The settings model to manage the shared settings. This model will hold the
	 * value entered by the user in the dialog and will update once the user changes
	 * the value. Furthermore, it provides methods to easily load and save the value
	 * to and from the shared settings (see: <br>
	 * {@link #loadValidatedSettingsFrom(NodeSettingsRO)},
	 * {@link #saveSettingsTo(NodeSettingsWO)}). <br>
	 * Here, we use a SettingsModelString as the number format is a String. There
	 * are models for all common data types. Also have a look at the comments in the
	 * constructor of the {@link NeroneNodeDialog} as the settings models are also
	 * used to create simple dialogs.
	 */
	//private final SettingsModelString m_numberFormatSettings = createNumberFormatSettingsModel();
	
	/*
	private static final String KEY_RESOURCE_DIR = "resource_dir";	
	private static final String DEFAULT_RESOURCE_DIR = "";	
	private final SettingsModelString m_resourceDirSettings = createResourceDirSettingsModel();
	*/
	public static final String USE_DOCUMENT = "Use document";
	public static final String USE_INPUT_COLUMNS = "Use string input columns";
	
	final static String INPUT_OPTION_PROPERTY="use_string_columns"; 
	final static String INPUT_OPTION_DEFAULT="true";
	final SettingsModelString m_InputOptionSettings = createInputOptionSettingsModel();
	
	
	final static String DOC_COL_PROPERTY="document"; 
	final static String DOC_COL_DEFAULT="document";
	final SettingsModelColumnName m_DocColSettings = createSettingsModelDocColumnSelection();
	
	final static String LANG_COL_PROPERTY="language"; 
	final static String LANG_COL_DEFAULT="language";
	final SettingsModelColumnName m_LanguageColSettings = createSettingsModelLangColumnSelection();
			
	final static String TEXT_COL_PROPERTY="text"; 
	final static String TEXT_COL_DEFAULT="text";
	final SettingsModelColumnName m_TextColSettings = createSettingsModelTextColumnSelection();
	
	final static String TITLE_COL_PROPERTY="title"; 
	final static String TITLE_COL_DEFAULT="title";
	final SettingsModelColumnName m_TitleColSettings = createSettingsModelTitleColumnSelection();
	
	private static final String KEY_MISSING = "missing";
	private static final Boolean DEFAULT_MISSING = true;
	private final SettingsModelBoolean m_missing = createMissingValuesSettings();
	
	private static final String KEY_INCLUDE_JSON = "include";
	private static final Boolean DEFAULT_INCLUDE_JSON = false;
	private final SettingsModelBoolean m_include = createIncludeJSONSettings();

	// constant defining the length of the jar name
	private static final int NAME = 10;
	// constant internal path
	private final static String IN_PATH = "src"+File.separator+"com"+File.separator+"jrc"+File.separator+"tmacc"+File.separator+"nerone"+File.separator+"resources";
			
	private boolean isXMLInput = false;
	private boolean isStringsInput = true;
	private boolean isDocumentInput = false;
	
	private NEROne one = null;
	
	private String COL_ID = "id";
	protected static String COL_LANGUAGE = "language";
	//private String COL_TITLE = "title";
	//private String COL_TEXT = "text";	
	//private String COL_NUM_ENTITES = "numberOfEntities";
	//private String COL_ENTITIES = "entities";
	//private String COL_RESULT = "NEROne result";
	
	
	
	/**
	 * Constructor for the node model.
	 */
	protected NEROneNodeModel() {
		/**
		 * Here we specify how many data input and output tables the node should have.
		 * In this case its one input and one output table.
		 */
		super(1, 1);
		
		
		//initial state
        String val = m_InputOptionSettings.getStringValue();
    	if(val.equalsIgnoreCase(NEROneNodeModel.INPUT_OPTION_DEFAULT)) {
    		m_DocColSettings.setEnabled(false);
    		m_TextColSettings.setEnabled(true);
    		m_TitleColSettings.setEnabled(true);
    	}else {
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
	/*
	static SettingsModelString createResourceDirSettingsModel() {
		return new SettingsModelString(KEY_RESOURCE_DIR, DEFAULT_RESOURCE_DIR);
	}
	*/
	
	
	static SettingsModelString createInputOptionSettingsModel() {
		return new SettingsModelString(INPUT_OPTION_PROPERTY, INPUT_OPTION_DEFAULT);
	}
	
	static SettingsModelColumnName createSettingsModelLangColumnSelection() {
		return new SettingsModelColumnName( NEROneNodeModel.LANG_COL_PROPERTY, NEROneNodeModel.LANG_COL_DEFAULT);
	}
	
	static SettingsModelColumnName createSettingsModelTextColumnSelection() {
		return new SettingsModelColumnName( NEROneNodeModel.TEXT_COL_PROPERTY, NEROneNodeModel.TEXT_COL_DEFAULT);
	}
	
	static SettingsModelColumnName createSettingsModelTitleColumnSelection() {
		return new SettingsModelColumnName( NEROneNodeModel.TITLE_COL_PROPERTY, NEROneNodeModel.TITLE_COL_DEFAULT);
	}
	
	static SettingsModelColumnName createSettingsModelDocColumnSelection() {
		return new SettingsModelColumnName( NEROneNodeModel.DOC_COL_PROPERTY, NEROneNodeModel.DOC_COL_DEFAULT);
	}
	// handle missing values
	static SettingsModelBoolean createMissingValuesSettings() {
		return new SettingsModelBoolean(KEY_MISSING, DEFAULT_MISSING);
	}

	public static SettingsModelBoolean createIncludeJSONSettings() {
		return new SettingsModelBoolean(KEY_INCLUDE_JSON, DEFAULT_INCLUDE_JSON);
	}
	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		
		/*
		 * The input data table to work with. The "inData" array will contain as many
		 * input tables as specified in the constructor. In this case it can only be one
		 * (see constructor).
		 */
		BufferedDataTable inputTable = inData[0];

		/*
		 * Create the spec of the output table, for each double column of the input
		 * table we will create one formatted String column in the output. See the
		 * javadoc of the "createOutputSpec(...)" for more information.
		 */
		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(),m_include.getBooleanValue());

		/*
		 * The execution context provides storage capacity, in this case a data
		 * container to which we will add rows sequentially. Note, this container can
		 * handle arbitrary big data tables, it will buffer to disc if necessary. The
		 * execution context is provided as an argument to the execute method by the
		 * framework. Have a look at the methods of the "exec". There is a lot of
		 * functionality to create and change data tables.
		 */
		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		/*
		 * Get the row iterator over the input table which returns each row one-by-one
		 * from the input table.
		 */
		CloseableRowIterator rowIterator = inputTable.iterator();

		// doNerone("C:\\temp\\nerone\\finder.xml", "C:\\temp\\nerone\\resources",
		// "C:\\temp\\nerone");				
		
		DataTableSpec inputSpec = inputTable.getDataTableSpec();		
		
		String[] colNames = inputSpec.getColumnNames();
		
		if(!isXMLInput) {
			//Check for expected columns
			//TODO maybe use
			//validateColumns(colNames);
		}
		
		
		if(!this.isXMLInput) {
			String path = new File(NEROneNodeModel.class.getProtectionDomain().getCodeSource().getLocation()
				    .toURI()).getPath();
			// remove the name of the jar 
			path = path.substring(0, path.length() - NAME);
			path = path + IN_PATH;			
			String resourcesDir = path;//getFilePathFromUrlPath(m_resourceDirSettings.getStringValue(),true);
			System.out.println("Resources Directory = " + resourcesDir);
			this.one = new NEROne();
			System.out.println("Working Directory = " + System.getProperty("user.dir"));

			this.one.initOutsideTheServlet("EntityMatcher EntityGuesser VariantIfs GeoMatcher", resourcesDir, null);
		}
		
		
		int docColIndex = -1;
		if(this.isDocumentInput) {
			String colName = m_DocColSettings.getStringValue(); //"Document"; 
			docColIndex = inputSpec.findColumnIndex(colName);
		}		
		/*
		String rDir = m_resourceDirSettings.getStringValue();		
		System.out.println("USER rDir: " + rDir);		
		*/
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
		LOGGER.info("Starting operations");
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
					
					System.out.println("COLUMN NAME: " + colName);
					//System.out.println("CELL TYPE: " + cell.getType());
					//System.out.println("CELL CLASS: " + cell.getType().getCellClass());
					
					//TODO temp test for RUNTIME use of DocumentBufferedFileStoreCell
					//System.out.println("CELL TYPE NAME: " + cell.getType());
					
					 //System.out.println("CLASS NAME: " + cell.getType().getCellClass());
					 
					/*
					 * if(cell.getType().getCellClass().equals((DocumentBufferedFileStoreCell.class)
					 * )){ System.out.println("DocumentBufferedFileStoreCell CLASS NAME: " +
					 * cell.getType()); }
					 */
					 
					
					if(this.isDocumentInput) {
						if(cell != null) {
							
							if(docColIndex >= 0) {
								if(i == docColIndex) {
									DocumentValue dv = (DocumentValue) cell;
						        	Document d = dv.getDocument();
						        	
						        	if(d != null) { 
						        		String title = d.getTitle(); 
						        		rssItem.setTitle(title);
						        		String text = d.getDocumentBodyText();
						        		rssItem.setText(text);
									 
						        		//TODO should use GUID  
						        		UUID docId = d.getUUID(); 
						        		rssItem.setAttribute("guid", docId.toString()); 
									 }
						        	//Add document
						        	//cells.add(cell);
						        	
								}else if (cell.getType().getCellClass().equals((StringCell.class))) {
									//TODO lang must still be passed in - can use Tika Language Detector to get it from a Document
									
									if(colName.equalsIgnoreCase(langColName)) { //COL_LANGUAGE
										StringCell sc = (StringCell) cell;
										String val = sc.getStringValue();
										System.out.println("CELL VALUE: " + val);
										rssItem.setAttribute("iso:language", val);
										rssItem.setLanguage(val);
										//cells.add(cell);
									}
								}
							}
							
							
							if (cell.getType().getCellClass().equals((StringCell.class))) {
								System.out.println("STRING CELL CLASS NAME: " + cell.getType().getCellClass());
							}else {
								System.out.println("DOCUMENT CELL CLASS NAME: " + cell.getType());
								System.out.println("DOCUMENT CELL CLASS NAME: " + cell.getType().getCellClass());	
								
								/*
								 * if(cell.getType().getCellClass().equals((DocumentBufferedFileStoreCell.class)
								 * )){ System.out.println("DocumentBufferedFileStoreCell CLASS NAME: " +
								 * cell.getType()); DocumentBufferedFileStoreCell dc =
								 * (DocumentBufferedFileStoreCell) cell; Document d = dc.getDocument();
								 * 
								 * if(d != null) { String title = d.getTitle(); rssItem.setTitle(title);
								 * 
								 * String text = d.getText(); rssItem.setText(text);
								 * 
								 * //TODO should use GUID UUID docId = d.getUUID(); rssItem.setAttribute("guid",
								 * docId.toString()); } } else
								 */
								if (cell.getType().getCellClass().equals((StringCell.class))) {
									//TODO lang must still be passed in - can use Tika Language Detector to get it from a Document
									if(colName.equalsIgnoreCase(COL_LANGUAGE)) {
										StringCell sc = (StringCell) cell;
										String val = sc.getStringValue();
										System.out.println("CELL VALUE: " + val);
										rssItem.setAttribute("iso:language", val);
										rssItem.setLanguage(val);
									}
								}
								 
							}
						}
						
					}else if(this.isStringsInput) {
						if (cell.getType().getCellClass().equals((StringCell.class))) {
							StringCell sc = (StringCell) cell;
							String val = sc.getStringValue();
							System.out.println("CELL VALUE: " + val);
							
							if(colName.equalsIgnoreCase(COL_ID)) {
								rssItem.setAttribute("guid", val);
							}else if(colName.equalsIgnoreCase(langColName)) {
								rssItem.setAttribute("iso:language", val);
								rssItem.setLanguage(val);
							}else if(colName.equalsIgnoreCase(titleColName)) {
								rssItem.setTitle(val);
							}else if(colName.equalsIgnoreCase(textColName)) {
								rssItem.setText(val);
							}	
							
							//TODO checkbox - whether to put back in original for the output
							//cells.add(cell);
	
						} 
					}
				}				
			}
			
			this.one.process(rssItem);
			
			
			NEROResult neroResult = new NEROResult();
									
			neroResult.setLanguage(rssItem.getLanguage());					
			neroResult.setPubDate(rssItem.getPubDate());						
			neroResult.setWordCount(rssItem.getWordCount());
			neroResult.setDate(rssItem.getDate().toString());
			neroResult.setName(rssItem.getName());
			neroResult.setText(rssItem.getText());						
			
			ArrayList<SimpleElement> entities = rssItem.getElements("emm:entity");
			//<emm:entity id="218730" type="o" count="1" pos="1592" name="Ryanair">Ryanair</emm:entity>
			
			ArrayList<Entity> entityObs = new ArrayList<Entity>();
			
			if(entities != null && entities.size() > 0) {
				for(SimpleElement el: entities) {
					Entity ent = new Entity();					
					ent.setValue(el.getValue());
					ent.setId(el.getAttributeValue("id"));
					ent.setType(el.getAttributeValue("type"));
					ent.setCount(el.getAttributeValue("count"));
					ent.setPos(el.getAttributeValue("pos"));
					ent.setName(el.getAttributeValue("name"));
					
					entityObs.add(ent);
				}
			}
			
			/*
			 * <emm:guess type="o" subtype="ORG" count="1" pos="186" name="Iranian military" rules="mo1_6">Iranian military</emm:guess>
			 */
			
			ArrayList<SimpleElement> guesses = rssItem.getElements("emm:guess");						
			
			if(guesses != null && guesses.size() > 0) {
				for(SimpleElement el: guesses) {
					Guess ent = new Guess();					
					ent.setValue(el.getValue());
					ent.setSubType(el.getAttributeValue("subtype"));
					ent.setName(el.getAttributeValue("name"));					
					ent.setId(el.getAttributeValue("id"));					
					ent.setCount(el.getAttributeValue("count"));
					ent.setPos(el.getAttributeValue("pos"));	
					ent.setRules(el.getAttributeValue("rules"));
					entityObs.add(ent);
				}
			}
			
			//Add entity count
			//cells.add(new IntCell(entityObs.size()));						
			
			/*<emm:georss name="Brussels:(Bruxelles-Capitale):Region de Bruxelles-Capitale / Brussels Hoofdstedelijk Gewes:Belgium" 
			id="16843797" lat="50.8371" lon="4.36761" count="1" pos="331" class="1" iso="BE" charpos="331" 
					wordlen="9" score="0.0">Bruxelles</emm:georss>*/
			
			
			ArrayList<SimpleElement> georss = rssItem.getElements("emm:georss");						
			
			if(georss != null && georss.size() > 0) {
				for(SimpleElement el: georss) {
					GeoRSS ent = new GeoRSS();					
					ent.setValue(el.getValue());
					ent.setName(el.getAttributeValue("name"));					
					ent.setId(el.getAttributeValue("id"));
					ent.setLat(el.getAttributeValue("lat"));
					ent.setLon(el.getAttributeValue("lon"));
					ent.setCount(el.getAttributeValue("count"));
					ent.setPos(el.getAttributeValue("pos"));
					ent.setEmmClass(el.getAttributeValue("class"));
					ent.setIso(el.getAttributeValue("iso"));
					ent.setCharpos(el.getAttributeValue("charpos"));
					ent.setWordlen(el.getAttributeValue("wordlen"));
					ent.setScore(el.getAttributeValue("score"));	
					entityObs.add(ent);
				}
			}			
			
			/*
			 * <emm:fullgeo name="Iranian" id="109" adjective="true" lat="0.0" lon="0.0" count="1" pos="151,579" 
			 * class="5" iso="null" charpos="151,579" wordlen="7,7" score="0.0">Iranian</emm:fullgeo>
			 */
			
			ArrayList<SimpleElement> fullGeos = rssItem.getElements("emm:fullgeo");						
			
			if(fullGeos != null && fullGeos.size() > 0) {
				for(SimpleElement el: fullGeos) {
					FullGeo ent = new FullGeo();					
					ent.setValue(el.getValue());
					ent.setName(el.getAttributeValue("name"));					
					ent.setId(el.getAttributeValue("id"));
					ent.setAdjective(el.getAttributeValue("adjective"));
					ent.setLat(el.getAttributeValue("lat"));
					ent.setLon(el.getAttributeValue("lon"));
					ent.setCount(el.getAttributeValue("count"));
					ent.setPos(el.getAttributeValue("pos"));
					ent.setEmmClass(el.getAttributeValue("class"));
					ent.setIso(el.getAttributeValue("iso"));
					ent.setCharpos(el.getAttributeValue("charpos"));
					ent.setWordlen(el.getAttributeValue("wordlen"));
					ent.setScore(el.getAttributeValue("score"));	
					entityObs.add(ent);
				}
			}

			if(entityObs.size() != 0) {
				// create the list of found entities
				String[] content = new String[entityObs.size()];
				for(int i = 0; i < content.length; i++) {
					content[i]= entityObs.get(i).getName(); 						
				}
				int[] cols = new int[entityObs.size()];
				for(int i = 0; i < cols.length; i++) {
					cols[i]= i; 
				}
				DataRow row = new DefaultRow("tmp", content);
				cells.add(CollectionCellFactory.createListCell(row, cols));
				// add the JSON advanced output
				if(m_include.getBooleanValue()) {
					neroResult.setElements(entityObs);			
					String neroResultJson = gson.toJson(neroResult);
					System.out.println("NERORESULT : " + neroResultJson);			
					//Add JSON entities					
					if(!neroResultJson.isEmpty()) {						
						cells.add(new StringCell(neroResultJson));						
					}
					else {
						if(m_include.getBooleanValue()) {
							cells.add(new MissingCell(""));
						}
						else {
							cells.add(new StringCell(""));
						}
						
					}
				}
			}
			else {
				// if there are no entities found
				if(m_missing.getBooleanValue()) {
					// set missing values
					cells.add(new MissingCell(""));
					if(m_include.getBooleanValue()) {
						cells.add(new MissingCell(""));
					}
				}
				else {
					// set empty strings
					DataRow row = new DefaultRow("tmp", "");
					int[] col = {0};
					cells.add(CollectionCellFactory.createListCell(row, col));					
					if(m_include.getBooleanValue()) {
						cells.add(new StringCell(""));
					}
				}
				
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
/*
	private void validateColumns(String[] colNames) throws InvalidSettingsException {
		boolean hasId = false;
		boolean hasLanguage = false;
		boolean hasTitle = false;
		boolean hasText = false;			
		
		for (int i = 0; i < colNames.length; i++) {
			String name = colNames[i];
			if(name.equalsIgnoreCase(COL_ID)) {
				hasId = true;
			}else if(name.equalsIgnoreCase(COL_LANGUAGE)) {
				hasLanguage = true;
			}else if(name.equalsIgnoreCase(COL_TITLE)) {
				hasTitle = true;
			}else if(name.equalsIgnoreCase(COL_TEXT)) {
				hasText = true;
			}
		}
		
		if(hasId && hasLanguage && hasTitle && hasText) {
			System.out.println("ALL REQUIRED COLUMNS PRESENT");
		}else {
			String str = "";
			if(!hasId) {
				str = COL_ID;
			}if(!hasLanguage) {
				str = str + ", " + COL_LANGUAGE;
			}if(!hasTitle) {
				str = str + ", " + COL_TITLE;
			}if(!hasText) {
				str = str + ", " + COL_TEXT;
			}
			
			if(str.startsWith(", ")) {
				str = str.substring(2);
			}
			
			
			//System.out.println("MISSING COLUMNS: " + str);
			throw new InvalidSettingsException("MISSING COLUMNS: " + str);
		}
	}
*/
/*	
	private String parseStringXML(String inputXmlStr, String resourcesDir, String outputDir) {
		String result = "";
		RSSParser parser = new RSSParser();
		try {
			RSS rss = null;
			
			InputStream is = new ByteArrayInputStream(inputXmlStr.getBytes());
			rss = parser.parse(new InputStreamReader(is) );
							
			
			for (RSSItem item : rss.getItems()) {
				removeElements(item.getElements("emm:entity"), item);
				removeElements(item.getElements("emm:guess"), item);
				removeElements(item.getElements("emm:georss"), item);
				removeElements(item.getElements("emm:fullgeo"), item);
			}
			
			double start = System.currentTimeMillis();
			NEROne one = new NEROne();
			one.initOutsideTheServlet("EntityMatcher EntityGuesser VariantIfs GeoMatcher", resourcesDir, null);
			double init = System.currentTimeMillis();
			one.process(rss);
			double end = System.currentTimeMillis();
			
			result = rss.toString();
			System.out.println("------------------------------------------------");
			if (true)
				for (RSSItem item : rss.getItems()) {

					//System.out.println("TITLE: " +item.getTitle());
					System.out.println("ITEM: " + item);					
					
*/					/*
					 * for (SimpleElement georss : item.getElements("emm:entity")) {
					 * System.out.println(georss); } System.out.println(); for (SimpleElement georss
					 * : item.getElements("emm:guess")) { System.out.println(georss); }
					 * System.out.println(); for (SimpleElement georss :
					 * item.getElements("emm:ifs")) { System.out.println(georss); }
					 * System.out.println(); for (SimpleElement georss :
					 * item.getElements("emm:georss")) { System.out.println(georss); }
					 * System.out.println(); for (SimpleElement georss :
					 * item.getElements("emm:fullgeo")) { System.out.println(georss); }
					 */
/*				}

			//System.out.println();

			System.out.println("Inizialization : " + (init - start));
			System.out.println("Processing     : " + (end - init));
			// log.info(rss);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
*/
/*	
	private void doNerone(String filePath, String resourcesDir, String outputDir) {
		String FILEPATH = null;
		String RESOURCES_DIR = null;
		String OUTPUT_DIR = "."; // default is current

		System.out.println("============ STARTING NERone");

		FILEPATH = filePath;
		RESOURCES_DIR = resourcesDir;

		if (outputDir != null) {
			OUTPUT_DIR = outputDir;
		}

		System.out.println("FILEPATH: " + FILEPATH);
		System.out.println("RESOURCES_DIR: " + RESOURCES_DIR);
		System.out.println("OUTPUT_DIR: " + OUTPUT_DIR);
*/
		/*
		 * LoggerContext context = (org.apache.logging.log4j.core.LoggerContext)
		 * LogManager.getContext(false); File file = new File("config/log4j2.xml"); //
		 * this will force a reconfiguration context.setConfigLocation(file.toURI());
		 * 
		 * if (false) try {
		 * 
		 * PrintStream newout = null; //newout = new PrintStream("outBIG.txt"); newout =
		 * new PrintStream("outSMA.txt"); System.setOut(newout); } catch (Exception e) {
		 * e.printStackTrace(); }
		 */

/*		RSSParser parser = new RSSParser();
		try {
			RSS rss = null;

			rss = parser.parse(new InputStreamReader(new FileInputStream(FILEPATH), "utf-8"));

			for (RSSItem item : rss.getItems()) {
				removeElements(item.getElements("emm:entity"), item);
				removeElements(item.getElements("emm:guess"), item);
				removeElements(item.getElements("emm:georss"), item);
				removeElements(item.getElements("emm:fullgeo"), item);
			}

			double start = System.currentTimeMillis();
			NEROne one = new NEROne();
			// one.initOutsideTheServlet("EntityMatcher EntityGuesser VariantIfs
			// GeoMatcher", "D:/workspace/NEROne/apache-tomcat-8.5.4/webapps/resources",
			// null);
			// one.initOutsideTheServlet("EntityMatcher EntityGuesser VariantIfs
			// GeoMatcher", "apache-tomcat-8.5.4/webapps/resources", null);
			one.initOutsideTheServlet("EntityMatcher EntityGuesser VariantIfs GeoMatcher", RESOURCES_DIR, null);

			double init = System.currentTimeMillis();
			one.process(rss);
			// one.process(rss.getItems().get(11));

			double end = System.currentTimeMillis();
			// rss.toXML(new OutputStreamWriter(System.out));

			// writing to file
			FileOutputStream fop = null;
			File f;
			try {
				f = new File(OUTPUT_DIR + "/result.xml");
				fop = new FileOutputStream(f);

				// if file doesn't exists, then create it
				if (!f.exists()) {
					f.createNewFile();
				}

				// get the content in bytes
				String xmlString = rss.toString();
				System.out.println(xmlString);
				byte[] contentInBytes = xmlString.getBytes();

				fop.write(contentInBytes);
				fop.flush();
				fop.close();

				System.out.println("RESULT OUTPUT TO: " + OUTPUT_DIR + "/result.xml");

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fop != null) {
						fop.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// System.out.println(rss);
			System.out.println("------------------------------------------------");

			if (true)
				for (RSSItem item : rss.getItems()) {
					
					// int kbytes = (item.getTitle().length()+ item.getText().length())/ 1024;
					// System.out.print(""+item.getGuid()+",");
					// if (NEROneProcessor.DEBUG_TIME) {
					// System.out.println(" "+item.getAttributeValue("emm:neronetime")+","+kbytes+",
					// Kbytes");
					// }

					for (SimpleElement georss : item.getElements("emm:entity")) {
						System.out.println(georss);
					}
					System.out.println();
					for (SimpleElement georss : item.getElements("emm:guess")) {
						System.out.println(georss);
					}
					System.out.println();
					for (SimpleElement georss : item.getElements("emm:ifs")) {
						System.out.println(georss);
					}
					System.out.println();
					for (SimpleElement georss : item.getElements("emm:georss")) {
						System.out.println(georss);
					}
					System.out.println();
					for (SimpleElement georss : item.getElements("emm:fullgeo")) {
						System.out.println(georss);
					}

				}

			System.out.println();

			System.out.println("Inizialization : " + (init - start));
			System.out.println("Processing     : " + (end - init));
			// log.info(rss);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
*/
/*	private static void removeElements(ArrayList<SimpleElement> arrayList, RSSItem item) {
		if (arrayList != null) {
			for (SimpleElement elem : arrayList) {
				item.removeElement(elem);
			}
		}
	}
*/
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
			
		/*
		String resourcesDir = m_resourceDirSettings.getStringValue();		
		System.out.println("USER resourcesDir: " + resourcesDir);
		*/
		String inOpts = m_InputOptionSettings.getStringValue();		
		System.out.println("USER inOpts: " + inOpts);
		
		if(inOpts.equalsIgnoreCase(NEROneNodeModel.USE_DOCUMENT)) {
			this.isDocumentInput = true;
			this.isStringsInput = false;
		}else {
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
		return new DataTableSpec[] { createOutputSpec(inputTableSpec,m_include.getBooleanValue()) };
	}

	/**
	 * Creates the output table spec from the input spec. 
	 * 
	 * the default output will report the list of found 
	 * 
	 * @param inputTableSpec
	 * @return
	 */
	private DataTableSpec createOutputSpec(DataTableSpec inputTableSpec, Boolean extended) {
		
		List<DataColumnSpec> newColumnSpecs = new ArrayList<>();

		// save all the inputs
		for (int i = 0; i < inputTableSpec.getNumColumns(); i++) {
			newColumnSpecs.add(inputTableSpec.getColumnSpec(i));
		}
		// add the category column
		DataColumnSpecCreator specCreator = new DataColumnSpecCreator("Named Entities", ListCell.getCollectionType(StringCell.TYPE));
		newColumnSpecs.add(specCreator.createSpec());
		// if triggers word are required, add a new column
		if (extended) {
			specCreator.setName("Extended JSON");
			specCreator.setType(StringCell.TYPE);
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
		//m_numberFormatSettings.saveSettingsTo(settings);
		m_LanguageColSettings.saveSettingsTo(settings);
		m_TextColSettings.saveSettingsTo(settings);
		m_TitleColSettings.saveSettingsTo(settings);
		//m_resourceDirSettings.saveSettingsTo(settings);
		m_InputOptionSettings.saveSettingsTo(settings);
		m_DocColSettings.saveSettingsTo(settings);
		m_missing.saveSettingsTo(settings);
		m_include.saveSettingsTo(settings);
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
		//m_numberFormatSettings.loadSettingsFrom(settings);
		m_InputOptionSettings.loadSettingsFrom(settings);
		m_LanguageColSettings.loadSettingsFrom(settings);
		m_TextColSettings.loadSettingsFrom(settings);
		m_TitleColSettings.loadSettingsFrom(settings);
		//m_resourceDirSettings.loadSettingsFrom(settings);		
		m_DocColSettings.loadSettingsFrom(settings);
		m_missing.loadSettingsFrom(settings);
		m_include.loadSettingsFrom(settings);
		
		
		//initial state
        String val = m_InputOptionSettings.getStringValue();
    	if(val.equalsIgnoreCase(NEROneNodeModel.USE_INPUT_COLUMNS)) {
    		m_DocColSettings.setEnabled(false);
    		m_TextColSettings.setEnabled(true);
    		m_TitleColSettings.setEnabled(true);
    	}else {
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
		//m_numberFormatSettings.validateSettings(settings);
		
		m_LanguageColSettings.validateSettings(settings);
		m_TextColSettings.validateSettings(settings);
		m_TitleColSettings.loadSettingsFrom(settings);
		//m_resourceDirSettings.validateSettings(settings);
		m_InputOptionSettings.validateSettings(settings);
		m_DocColSettings.validateSettings(settings);
		m_missing.validateSettings(settings);
		m_include.validateSettings(settings);
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
