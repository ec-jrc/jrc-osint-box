package com.jrc.tmacc.timelines;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

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
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.ext.textprocessing.data.Document;
import org.knime.ext.textprocessing.data.DocumentValue;

import it.jrc.osint.extract.timelines.TimelineExtractionService;
import it.jrc.osint.extract.timelines.TimelineExtractionServiceFactory;
import it.jrc.osint.extract.timelines.document.BasicDocumentInterpreter;
import it.jrc.osint.extract.timelines.document.FileSystemDocumentLoader;
import it.jrc.osint.extract.timelines.ling.Entity;
import it.jrc.osint.extract.timelines.ling.Timeline;

import piskorski.util.functions.PropertiesFunctions;


/**
 * This is an implementation of the node model of the "Timelines" node.
 * 
 * This node extracts Entities and Timelines from String data
 *
 * @author Andrea Caielli
 */
public class TimelinesNodeModel extends NodeModel {

	/**
	 * The logger is used to print info/warning/error messages to the KNIME console
	 * and to the KNIME log file. Retrieve it via 'NodeLogger.getLogger' providing
	 * the class of this node model.
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(TimelinesNodeModel.class);

	/**
	 * The settings key to retrieve and store settings shared between node dialog
	 * and node model. In this case, the key for the number format String that
	 * should be entered by the user in the dialog.
	 */
	private static final String KEY_ENTITIES = "entities_path";	
	private static final String KEY_COLUMN = "column";

	private static final String KEY_LOCALE ="LOCALE";
	private static final String KEY_ENTITY_MATCHER_EXTENDEDMATCHING = "ENTITY.MATCHER.EXTENDEDMATCHING";	
	private static final String KEY_ENTITY_MATCHER_FUZZYMATCHING = "ENTITY.MATCHER.FUZZYMATCHING";
	private static final String KEY_ENTITY_MATCHER_FUZZYMATCHING_METRIC = "ENTITY.MATCHER.FUZZYMATCHING.METRIC";
	private static final String KEY_ENTITY_MATCHER_FUZZYMATCHING_MINDIST= "ENTITY.MATCHER.FUZZYMATCHING.MINDIST";
	private static final String KEY_ANAPHORA_RESOLUTION= "ANAPHORA.RESOLUTION";
	private static final String KEY_ENTITY_RECOGNIZER_GUESSINGHEURISTICS="ENTITY.RECOGNIZER.GUESSINGHEURISTICS";
	private static final String KEY_ENTITY_RECOGNIZER_GUESSINGHEURISTICS_SINGLETOKENS="ENTITY.RECOGNIZER.GUESSINGHEURISTICS.SINGLETOKENS";
	private static final String KEY_EVENT_DUPLICATE_REMOVAL_STRATEGY="EVENT.DUPLICATE.REMOVAL.STRATEGY";
	private static final String KEY_EVENT_MATCHER_POS_TAGGER_FILTER="EVENT.MATCHER.POS.TAGGER.FILTER";
	/**
	 * The default values for the keys. 
	 */	
	private static final String DEFAULT_ENTITIES = "";
	private static final String DEFAULT_COLUMN = "none";

	private static final String DEFAULT_LOCALE ="en";
	private static final Boolean DEFAULT_ENTITY_MATCHER_EXTENDEDMATCHING = true;
	private static final Boolean DEFAULT_ENTITY_MATCHER_FUZZYMATCHING = true;
	private static final String DEFAULT_ENTITY_MATCHER_FUZZYMATCHING_METRIC = "WeightedLongestCommonSubstrings";
	private static final Double DEFAULT_ENTITY_MATCHER_FUZZYMATCHING_MINDIST= 0.25;
	private static final Boolean DEFAULT_ANAPHORA_RESOLUTION = true;
	private static final Boolean DEFAULT_ENTITY_RECOGNIZER_GUESSINGHEURISTICS=true;
	private static final Boolean DEFAULT_ENTITY_RECOGNIZER_GUESSINGHEURISTICS_SINGLETOKENS=true;
	private static final String DEFAULT_EVENT_DUPLICATE_REMOVAL_STRATEGY="complex";
	private static final Boolean DEFAULT_EVENT_MATCHER_POS_TAGGER_FILTER=true;



	/**
	 * internal constants
	 */
	// represents the name of the jar
	private static final int NAME = 13;
	// constant defining the length of the jar name
	// will be used in the path definition	
	private static final String INPUT_NAME = "tmpIN.txt";	
	private static final String CONFIG_NAME = "config.txt";	
	// how to handle missing inputs - default has missing values as null
	private static final String KEY_HANDLE_MISSING = "missing_value";
	private static final Boolean DEFAULT_HANDLE_MISSING_VAL = true;
	// include JSON output
	private static final String KEY_INCLUDE_JSON = "include_json";
	private static final Boolean DEFAULT_INCLUDE_JSON = true;
	// include Entity Target
	private static final String KEY_INCLUDE_TARGET = "include_entity";
	private static final Boolean DEFAULT_INCLUDE_TARGET = true;

	/**
	 * The settings model to manage the shared settings. 
	 * There are models for all common data types. Also have a look at the comments 
	 * in the constructor of the {@link TimelinesNodeDialog} as the settings 
	 * models are also used to create simple dialogs.
	 */	
	private final SettingsModelString m_entitiesProfileSettings = createEntitiesProfileSettingsModel();
	private final SettingsModelString m_inputColumnSettings = createInputColumnSettingsModel();
	// for missing values
	private final SettingsModelBoolean missingValuesSettings = createMissingValuesSettingsModel();
	private final SettingsModelBoolean m_configuration10 = createConfiguration10Model();

	private final SettingsModelString m_configuration1 = createConfiguration1Model();
	private final SettingsModelBoolean m_configuration2 = createConfiguration2Model();
	private final SettingsModelBoolean m_configuration3 = createConfiguration3Model();
	private final SettingsModelString m_configuration4 = createConfiguration4Model();
	private final SettingsModelDoubleBounded m_configuration5 = createConfiguration5Model();
	private final SettingsModelBoolean m_configuration6 = createConfiguration6Model();
	private final SettingsModelBoolean m_configuration7 = createConfiguration7Model();
	private final SettingsModelBoolean m_configuration8 = createConfiguration7Model();
	private final SettingsModelString m_configuration9 = createConfiguration9Model();
	private final SettingsModelBoolean m_includeJSON = createIncludeJSONSettings();
	private final SettingsModelBoolean m_includeTargetEntity = createIncludeTargetEntitySettings();
	/**
	 * Constructor for the node model.
	 */
	protected TimelinesNodeModel() {
		/**
		 * Here we specify how many data input and output tables the node should have.
		 * In this case its one input and one output table.
		 */
		super(1, 1);
	}


	/**
	 * Convenience methods to create  new settings models 
	 * This method will also be used in the {@link TimelinesNodeDialog}. 
	 * The settings model will sync via the above defined key.
	 * 
	 * @return a new SettingsModelString with the key for the model String
	 */	
	public static SettingsModelString createEntitiesProfileSettingsModel() {
		return new SettingsModelString(KEY_ENTITIES, DEFAULT_ENTITIES);
	}

	public static SettingsModelString createInputColumnSettingsModel() {
		return new SettingsModelString(KEY_COLUMN, DEFAULT_COLUMN);	
	}

	static SettingsModelString createConfiguration1Model() {
		return new SettingsModelString(KEY_LOCALE, DEFAULT_LOCALE);
	}

	static SettingsModelBoolean createConfiguration2Model() {
		return new SettingsModelBoolean(KEY_ENTITY_MATCHER_EXTENDEDMATCHING, DEFAULT_ENTITY_MATCHER_EXTENDEDMATCHING);
	}

	static SettingsModelBoolean createConfiguration3Model() {
		return new SettingsModelBoolean(KEY_ENTITY_MATCHER_FUZZYMATCHING, DEFAULT_ENTITY_MATCHER_FUZZYMATCHING);
	}

	static SettingsModelString createConfiguration4Model() {
		return new SettingsModelString(KEY_ENTITY_MATCHER_FUZZYMATCHING_METRIC, DEFAULT_ENTITY_MATCHER_FUZZYMATCHING_METRIC);
	}

	static SettingsModelDoubleBounded createConfiguration5Model() {
		return new SettingsModelDoubleBounded(KEY_ENTITY_MATCHER_FUZZYMATCHING_MINDIST, DEFAULT_ENTITY_MATCHER_FUZZYMATCHING_MINDIST, 0, 1);
	}

	static SettingsModelBoolean createConfiguration6Model() {
		return new SettingsModelBoolean(KEY_ANAPHORA_RESOLUTION, DEFAULT_ANAPHORA_RESOLUTION);
	}

	static SettingsModelBoolean createConfiguration7Model() {
		return new SettingsModelBoolean(KEY_ENTITY_RECOGNIZER_GUESSINGHEURISTICS, DEFAULT_ENTITY_RECOGNIZER_GUESSINGHEURISTICS);
	}

	static SettingsModelBoolean createConfiguration8Model() {
		return new SettingsModelBoolean(KEY_ENTITY_RECOGNIZER_GUESSINGHEURISTICS_SINGLETOKENS, DEFAULT_ENTITY_RECOGNIZER_GUESSINGHEURISTICS_SINGLETOKENS);
	}

	static SettingsModelString createConfiguration9Model() {
		return new SettingsModelString(KEY_EVENT_DUPLICATE_REMOVAL_STRATEGY, DEFAULT_EVENT_DUPLICATE_REMOVAL_STRATEGY);
	}

	static SettingsModelBoolean createConfiguration10Model() {
		return new SettingsModelBoolean(KEY_EVENT_MATCHER_POS_TAGGER_FILTER, DEFAULT_EVENT_MATCHER_POS_TAGGER_FILTER);
	}
	//missing values
	static SettingsModelBoolean createMissingValuesSettingsModel() {
		return new SettingsModelBoolean(KEY_HANDLE_MISSING, DEFAULT_HANDLE_MISSING_VAL);
	}
	// advanced JSON
	public static SettingsModelBoolean createIncludeJSONSettings() {
		return new SettingsModelBoolean(KEY_INCLUDE_JSON, DEFAULT_INCLUDE_JSON);
	}
	// include target entity
	public static SettingsModelBoolean createIncludeTargetEntitySettings() {
		return new SettingsModelBoolean(KEY_INCLUDE_TARGET, DEFAULT_INCLUDE_TARGET);
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
		DataTableSpec tableSpec = inputTable.getDataTableSpec();	
		// see how to handle missing values
		Boolean missingValues = missingValuesSettings.getBooleanValue();
		// we find the index of the column which had been selected by the user as input
		int column = tableSpec.findColumnIndex(m_inputColumnSettings.getStringValue());
		// a tricky way to hardcode the path of the model files
		// we get the path of execution of the plugin (which is a jar)
		String path = new File(TimelinesNodeModel.class.getProtectionDomain().getCodeSource().getLocation()
				.toURI()).getPath();
		// remove the name of the jar 		
		path = path.substring(0, path.length() - NAME);		
		//prepare the configuration files		 
		String generalConfigPath = "";
		if(File.separator == "/") {
			generalConfigPath= System.getProperty("user.dir")+File.separator+"system_resources";
		}
		else {
			//for magical reasons we need to double the windows
			String tmpString =System.getProperty("user.dir");
			tmpString = tmpString.replace("\\", "\\\\");
			generalConfigPath =tmpString+"\\\\system_resources";
		}
		// here we compose the configuration file with all the user choices
		String fileContent = "RESOURCES.PATH="+generalConfigPath+"\n\n"+
				"LOCALE="+m_configuration1.getStringValue()+"\n\n"+					
				"ENTITY.MATCHER.EXTENDEDMATCHING="+String.valueOf(m_configuration2.getBooleanValue())+"\n\n"+
				"ENTITY.MATCHER.FUZZYMATCHING="+String.valueOf(m_configuration3.getBooleanValue())+"\n\n"+	
				"ENTITY.MATCHER.FUZZYMATCHING.METRIC="+m_configuration4.getStringValue()+"\n\n"+
				//fixed
				"ENTITY.MATCHER.FUZZYMATCHING.METRIC.PARAMETERS=\"\"\n\n"+
				"ENTITY.MATCHER.FUZZYMATCHING.MINDIST="+m_configuration5.getDoubleValue()+"\n\n"+
				//fixed
				"ENTITY.MATCHER.FILTERINGHEURISTICS=false\n\n"+
				"ANAPHORA.RESOLUTION="+String.valueOf(m_configuration6.getBooleanValue())+"\n\n"+
				"ENTITY.RECOGNIZER.GUESSINGHEURISTICS="+String.valueOf(m_configuration7.getBooleanValue())+"\n\n"+	
				"ENTITY.RECOGNIZER.GUESSINGHEURISTICS.SINGLETOKENS="+String.valueOf(m_configuration8.getBooleanValue())+"\n\n"+	
				//fixed
				"RELATEDENTITY.TO.EVENT.MAPPER.STRATEGY=all-in-sentence\n\n"+
				"EVENT.DUPLICATE.REMOVAL.STRATEGY="+m_configuration9.getStringValue()+"\n\n"+
				"EVENT.MATCHER.POS.TAGGER.FILTER="+String.valueOf(m_configuration10.getBooleanValue())+"\n\n"+	
				//fixed
				"EVENT.ENTITY.MAPPING.STRATEGY=baseline\n\n"+
				//fixed
				"EVENT.TIMEX.MAPPING.STRATEGY=baseline";

		// write the configuration file
		File configFile = new File(path+CONFIG_NAME);
		if (configFile.exists()) {
			configFile.delete();
		} 
		configFile.createNewFile();
		FileWriter fw = new FileWriter(configFile.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(fileContent);
		bw.close();

		/*
        // we have to standardize all the internal configurations for the system_resources
        BufferedReader br = new BufferedReader(new FileReader(generalConfigPath+File.separator+"files.txt"));
		String line;
		while ((line = br.readLine()) != null) {
			if(!(line.startsWith("#"))) {
				line= line.replace("\\", File.separator).replace("/", File.separator);
				LOGGER.warn(path+line);
				String[] words=path.split(Pattern.quote(File.separator));
				String relPath ="."+File.separator+words[words.length-2]+File.separator+words[words.length-1]+File.separator;
				LOGGER.warn(relPath);						
				replacePath(path+line,"= ./","="+path);
				replacePath(path+line,"< ./","="+path);
				replacePath(path+line,"; ./","="+path);				
			}
		}
		br.close();		
        LOGGER.warn("files copiati");
		 */

		// we need to copy system_resources folders in user_dir        
		File srcDir = new File(path+"system_resources");
		File destDir = new File(generalConfigPath);
		try {
			FileUtils.copyDirectory(srcDir, destDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//LOGGER.warn(path+"system_resources"+" copied into "+generalConfigPath);

		// we instantiate an "empty" result table
		BufferedDataTable out = null;	
		LOGGER.info("preparing output...");		
		int output_fields = 1;
		Boolean jsonInput = m_includeJSON.getBooleanValue();
		Boolean entityTarget = m_includeTargetEntity.getBooleanValue();
		if(jsonInput && entityTarget) {
			// include both
			output_fields = 2;
		}
		else {
			if(jsonInput) {
				// include only JSON
				output_fields = 3;
			}
			else {
				if(entityTarget) {
					// include entity target
					output_fields = 4;
				}				
			}
		}
		DataTableSpec outputSpec = createOutputSpec(inputTable.getDataTableSpec(),output_fields);						
		// Create a data container to which we will add rows sequentially.
		BufferedDataContainer container = exec.createDataContainer(outputSpec);						
		// create an iterator		
		CloseableRowIterator rowIterator = inputTable.iterator();
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
			//get the input column
			DataCell cell = currentRow.getCell(column);
			String text = "";
			// handle the Document input
			try {				
				DocumentValue dv = (DocumentValue) cell;
				Document d = dv.getDocument();	        	
				if(d != null) { 		        		
					text = d.getDocumentBodyText();	 
				}
			}
			catch (Exception e) {
				System.out.print(e.getMessage());
			}		        	
			//handle the string input
			try {
				StringCell argumentCell = (StringCell) cell;						
				text = argumentCell.getStringValue();
			} catch (Exception e) {
				System.out.print(e.getMessage());
			}

			// if the line is empty
			/*if(text.isEmpty()) {
				switch (output_fields) {
				case 1:
					// only standard output
					if(missingValues) {
						// here we add a missing value
						cells.add(new MissingCell(""));
					}
					else {
						// here we add an empty string
						// set empty strings
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
					}					
					break;
				case 2:
					// both
					if(missingValues) {
						// here we add a missing value
						cells.add(new MissingCell(""));
						cells.add(new MissingCell(""));
						cells.add(new MissingCell(""));
					}
					else {
						// here we add an empty string
						// set empty strings
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
						cells.add(new StringCell(""));
						cells.add(new StringCell(""));
					}					
					break;
				case 3:
					// JSON
					if(missingValues) {
						// here we add a missing value
						cells.add(new MissingCell(""));
						cells.add(new MissingCell(""));
					}
					else {
						// here we add an empty string
						// set empty strings
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
						cells.add(new StringCell(""));
					}		
					break;
				case 4:
					// target
					if(missingValues) {
						// here we add a missing value
						cells.add(new MissingCell(""));
						cells.add(new MissingCell(""));
					}
					else {
						// here we add an empty string
						// set empty strings
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
						cells.add(new StringCell(""));
					}		
					break;
				default:
					LOGGER.warn("Huston, we got a problem here");
					break;
				}


			}
			else {	*/


			// create the input file 
			// it has to have the following structure
			/*
				1 - id
				en -  language
				16-11-2016 - date
				xxxxx - source
				%%% 
				xxxxx - title    
			 */	 
			//get the date correct
			LocalDateTime ldt = LocalDateTime.now();
			String date = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH).format(ldt);
			String argument = currentRowCounter+"\n"+
					"en\n"+
					date+"\n"+
					"JRC_osint_box\n"+
					"%%%\n"+
					"Entity "+currentRowCounter+"\n"
					+text;
			// write the input row in the file input file					
			//File inputFile = new File(path+"input"+File.separator+INPUT_NAME);				
			File newDirectory = new File(path+"input");
			//Create directory for non existed path.
			newDirectory.mkdirs();
			File inputFile = new File(path+"input"+File.separator+INPUT_NAME);
			if (inputFile.exists()) {
				inputFile.delete();
			}
			try {
				inputFile.createNewFile(); 
			} catch (Exception e) {
				LOGGER.warn(e.getMessage());
			}

			FileWriter fwin = new FileWriter(inputFile.getAbsolutePath());
			BufferedWriter bwin = new BufferedWriter(fwin); 
			bwin.write(argument);			
			bwin.close();		
			/*
			 * runs Timelines Extraction service. It takes four arguments:  
			 * %1 - configuration file - input from user
			 * %2 - path to the file from which an entity object will be created - from user
			 * %3 - path to the directory with documents to process (in a specific format) - hardcoded
			 * %4 - path to the file where the output will be written to - hardcoded
			 */
			LOGGER.info("calling extractor...");			
			// create an entity
			Entity myEntity = Entity.createEntity(getFilePathFromUrlPath(m_entitiesProfileSettings.getStringValue(),true));			
			// define parameters required to launch the service
			Properties serviceProperties = new Properties();
			serviceProperties = PropertiesFunctions.readProperties(path+CONFIG_NAME,"UTF-8");
			// create an instance of a timeline extraction service 
			TimelineExtractionService tles = TimelineExtractionServiceFactory.getService(serviceProperties);			
			// create a handle to read files from a given directory  
			FileSystemDocumentLoader fsDocLoader = new FileSystemDocumentLoader(BasicDocumentInterpreter.getInstance());			
			// create an entity around which timeline will be extracted and provide the corresponding name variants			
			// define parameters for applying the service
			Properties parameters = new Properties();
			// call the service to extract timelines				
			Timeline result = tles.extractTimeline(myEntity, fsDocLoader.getDocumentIterator(path+"input"), parameters);	
			// get how many events are extracted
			int events_count = result.getEvents().size();				
			if(events_count > 0) {
				// create the list of found entities
				String[] content = new String[events_count];
				for(int i = 0; i < events_count; i++) {
					String tmp_res = result.getEvent(i).getEventMatch().toString();
					content[i] = tmp_res.substring(tmp_res.lastIndexOf("TEXT SNIPPET")+14, tmp_res.indexOf("EVENT DESCRIPTION"));
				}
				int[] cols = new int[events_count];
				for(int i = 0; i < cols.length; i++) {
					cols[i]= i; 
				}
				DataRow row = new DefaultRow("tmp", content);
				cells.add(CollectionCellFactory.createListCell(row, cols));
				/*
				DataRow row = new DefaultRow("tmp", "");
				int[] col = {0};
				cells.add(CollectionCellFactory.createListCell(row, col));
				*/

				switch (output_fields) {
				case 1:
					// all done
					break;
				case 2:
					// add both JSON and Target Entity
					cells.add(new StringCell(result.toJSON("")));
					cells.add(new StringCell(myEntity.getName()));
					break;
				case 3: 
					//add JSON
					cells.add(new StringCell(result.toJSON("")));
					break;
				case 4:
					// add Target Entity
					cells.add(new StringCell(myEntity.getName()));
					break;
				default:
					LOGGER.warn("Huston, we got a problem here");
					break;
				}
			}
			else {
				switch (output_fields) {
				case 1:
					// only standard output
					if(missingValues) {
						// here we add a missing value
						cells.add(new MissingCell(""));
					}
					else {
						// here we add an empty string
						// set empty strings
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
					}					
					break;
				case 2:
					// both
					if(missingValues) {
						// here we add a missing value
						cells.add(new MissingCell(""));
						cells.add(new MissingCell(""));
						cells.add(new StringCell(myEntity.getName()));
					}
					else {
						// here we add an empty string
						// set empty strings
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
						cells.add(new StringCell(""));
						cells.add(new StringCell(myEntity.getName()));
					}					
					break;
				case 3:
					// JSON
					if(missingValues) {
						// here we add a missing value
						cells.add(new MissingCell(""));
						cells.add(new MissingCell(""));
					}
					else {
						// here we add an empty string
						// set empty strings
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
						cells.add(new StringCell(""));
					}		
					break;
				case 4:
					// target
					if(missingValues) {
						// here we add a missing value
						cells.add(new MissingCell(""));
						cells.add(new StringCell(myEntity.getName()));
					}
					else {
						// here we add an empty string
						// set empty strings
						DataRow row = new DefaultRow("tmp", "");
						int[] col = {0};
						cells.add(CollectionCellFactory.createListCell(row, col));
						cells.add(new StringCell(myEntity.getName()));
					}		
					break;
				default:
					LOGGER.warn("Huston, we got a problem here");
					break;
				}

			}
			inputFile.delete();
			//}endelse
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
		//remove the temporary files	
		configFile.delete();	
		Runtime.getRuntime().gc();
		FileUtils.deleteDirectory(destDir);
		//Runtime.getRuntime().gc();
		//FileUtils.forceDeleteOnExit(destDir);
		//File lastFile = new File(destDir+File.separator+INPUT_NAME);	

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
	 * One String column will be created containing Timelines data as String cells.
	 * 
	 * @param inputTableSpec
	 * @return output table specs.
	 */
	private DataTableSpec createOutputSpec(DataTableSpec inputTableSpec,int fields) {
		List<DataColumnSpec> newColumnSpecs = new ArrayList<>();
		// Iterate over the input column specs
		for(int i = 0; i < inputTableSpec.getNumColumns();i++) {
			newColumnSpecs.add(inputTableSpec.getColumnSpec(i));
		}
		DataColumnSpecCreator specCreator = new DataColumnSpecCreator("Event Sentence", ListCell.getCollectionType(StringCell.TYPE));
		newColumnSpecs.add(specCreator.createSpec());
		switch (fields) {
		case 1:
			// all good
			break;
		case 2:
			// both
			specCreator.setName("JSON Output");
			specCreator.setType(StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			specCreator.setName("Target Entity");			
			specCreator.setType(StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());	
			break;	
		case 3:
			// only JSON
			specCreator.setName("JSON Output");
			specCreator.setType(StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());
			break;
		case 4:
			// only target entity
			specCreator.setName("Target Entity");
			specCreator.setType(StringCell.TYPE);
			newColumnSpecs.add(specCreator.createSpec());
			break;
		default:
			break;
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
		 * Save user settings to the NodeSettings object. SettingsModels already know how to
		 * save them self to a NodeSettings object by calling the below method. In general,
		 * the NodeSettings object is just a key-value store and has methods to write
		 * all common data types. Hence, you can easily write your settings manually.
		 * See the methods of the NodeSettingsWO.
		 */
		m_configuration1.saveSettingsTo(settings);
		m_configuration2.saveSettingsTo(settings);
		m_configuration3.saveSettingsTo(settings);
		m_configuration4.saveSettingsTo(settings);
		m_configuration5.saveSettingsTo(settings);
		m_configuration6.saveSettingsTo(settings);
		m_configuration7.saveSettingsTo(settings);
		m_configuration8.saveSettingsTo(settings);
		m_configuration9.saveSettingsTo(settings);
		m_configuration10.saveSettingsTo(settings);
		m_entitiesProfileSettings.saveSettingsTo(settings);
		m_inputColumnSettings.saveSettingsTo(settings);
		missingValuesSettings.saveSettingsTo(settings);
		m_includeJSON.saveSettingsTo(settings);
		m_includeTargetEntity.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		/*
		 * Load (valid) settings from the NodeSettings object. It can be safely assumed that
		 * the settings are validated by the method below.
		 * 
		 * The SettingsModel will handle the loading. After this call, the current value
		 * (from the view) can be retrieved from the settings model.
		 */
		m_configuration1.loadSettingsFrom(settings);
		m_configuration2.loadSettingsFrom(settings);
		m_configuration3.loadSettingsFrom(settings);
		m_configuration4.loadSettingsFrom(settings);
		m_configuration5.loadSettingsFrom(settings);
		m_configuration6.loadSettingsFrom(settings);
		m_configuration7.loadSettingsFrom(settings);
		m_configuration8.loadSettingsFrom(settings);
		m_configuration9.loadSettingsFrom(settings);
		m_configuration10.loadSettingsFrom(settings);
		m_entitiesProfileSettings.loadSettingsFrom(settings);
		m_inputColumnSettings.loadSettingsFrom(settings);
		missingValuesSettings.loadSettingsFrom(settings);
		m_includeJSON.loadSettingsFrom(settings);
		m_includeTargetEntity.loadSettingsFrom(settings);
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
		m_configuration1.validateSettings(settings);
		m_configuration2.validateSettings(settings);
		m_configuration3.validateSettings(settings);
		m_configuration4.validateSettings(settings);
		m_configuration5.validateSettings(settings);
		m_configuration6.validateSettings(settings);
		m_configuration7.validateSettings(settings);
		m_configuration8.validateSettings(settings);
		m_configuration9.validateSettings(settings);
		m_configuration10.validateSettings(settings);
		m_entitiesProfileSettings.validateSettings(settings);
		m_inputColumnSettings.validateSettings(settings);
		missingValuesSettings.validateSettings(settings);
		m_includeJSON.validateSettings(settings);
		m_includeTargetEntity.validateSettings(settings);
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
				CheckUtils.checkSourceFile(filePath.toString());
			} else {
				CheckUtils.checkDestinationDirectory(filePath.getParent().toString());
			}

			return filePath.toString();

		} catch(Exception e) {
			throw new Exception(e.getLocalizedMessage());
		}
	}	

}

