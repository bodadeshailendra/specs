/**
 * 
 */
package com.ai.testgenerator.service.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ai.testgenerator.models.Event;
import com.ai.testgenerator.models.Script;
//import com.ai.testgenerator.models.Event;
//import com.ai.testgenerator.models.Script;
import com.ai.testgenerator.service.AutoTestGeneratorService;
import com.ai.testgenerator.utils.ExecCommand;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;

/**
 * 
 */

@Service
public class AutoTestGeneratorServiceImpl implements AutoTestGeneratorService {

	String nodePath = "C:/Users/ShailendraBodade/AppData/Roaming/npm/";
	@Override
	public File createPostmanCollection(MultipartFile apiSpec, MultipartFile dataFile) throws Exception {

		StringBuffer stringBuffer = new StringBuffer(System.getProperty("user.dir")).append("/").append("output");
		File outputFolder = new File(stringBuffer.toString());
		if (!outputFolder.exists()) {
			outputFolder.mkdir();
		}
		File apiSpecFile = multipartFileToFile(apiSpec, outputFolder.toPath());
		File apiDataFile = multipartFileToFile(dataFile, outputFolder.toPath());
		String command = "openapi2postmanv2 --version";
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}
		ExecCommand execCommand = new ExecCommand(command);
		String error = execCommand.getError();
		if (error.contains("is not recognized")) {
			throw new Exception("openapi2postmanv2 is not installed properly....");
		}

		command = "openapi2postmanv2 -s " + apiSpecFile.getAbsolutePath() + " -o collection.json";
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}

		execCommand = new ExecCommand(command);
		String output = execCommand.getOutput();
		if (!output.contains("Conversion successful")) {
			throw new Exception("Conversion Failed, validate the YAML spec....");
		}

		File fileOp = new File("collection.json");
		if (addTestsToRequest(fileOp, apiDataFile)) {
			fileOp = new File("ModifiedJson.json");
		}

		return fileOp;
	}

	private File multipartFileToFile(MultipartFile multipart, Path dir) throws IOException {
		Path filepath = Paths.get(dir.toString(), multipart.getOriginalFilename());
		multipart.transferTo(filepath);

		return filepath.toFile();
	}

	@Override
	public File runPostmanCollection(MultipartFile collectionJson) throws Exception {
		StringBuffer stringBuffer = new StringBuffer(System.getProperty("user.dir")).append("/").append("output");
		File outputFolder = new File(stringBuffer.toString());
		if (!outputFolder.exists()) {
			outputFolder.mkdir();
		}
		File postmanCollection = multipartFileToFile(collectionJson, outputFolder.toPath());
		String command = "newman -v";
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}
		ExecCommand execCommand = new ExecCommand(command);
		String error = execCommand.getError();
		if (error.contains("is not recognized")) {
			throw new Exception("newman is not installed properly....");
		}

		command = "newman run " + postmanCollection.getAbsolutePath() + " -r htmlextra";
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}
		execCommand = new ExecCommand(command);
		File newmanDir = new File("newman");
		File[] files = newmanDir.listFiles(File::isFile);
		long lastModifiedTime = Long.MIN_VALUE;
		File chosenFile = null;

		if (files != null) {
			for (File file : files) {
				if (file.lastModified() > lastModifiedTime) {
					chosenFile = file;
					lastModifiedTime = file.lastModified();
				}
			}
		}
		return chosenFile;
	}

	private boolean addTestsToRequest(File jsonFile, File dataFile) {
		boolean fileModified = true;
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode rootNode = objectMapper.readTree(jsonFile);
			// Updating global variables if any
			JsonNode updatedJsonNode = updateGlobalVariables(rootNode, dataFile);
			addTestInEvent(updatedJsonNode, dataFile);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			fileModified = false;
		}
		return fileModified;
	}

	private void addTestInEvent(JsonNode jsonNode, File dataFile)
			throws StreamWriteException, DatabindException, IOException {
		JsonNode nxtNode = jsonNode.get("item");
		for (JsonNode arrNode : nxtNode) {
			if (arrNode.has("item")) {
				addTestInEvent(arrNode, dataFile);
			} else {
				// Read Data from CSV and change the values in
				FileReader fileReader = new FileReader(dataFile);
				CSVReader apiData = new CSVReader(fileReader);
				List<String[]> completeAPIData = apiData.readAll();
				if (completeAPIData.size() > 0) {
					// Update Query Parameters
					for (JsonNode queryParams : arrNode.get("request").get("url").get("query")) {
						queryParams = updateDataFromCSV(queryParams, completeAPIData);
					}

					// Update Variables
					for (JsonNode variables : arrNode.get("request").get("url").get("variable")) {
						variables = updateDataFromCSV(variables, completeAPIData);
					}

					if (arrNode.get("request").get("body").has("mode")) {
						String bodyMode = arrNode.get("request").get("body").get("mode").asText();
						if (!bodyMode.equalsIgnoreCase("raw")) {
							// Update body
							for (JsonNode variables : arrNode.get("request").get("body").get(bodyMode)) {
								variables = updateDataFromCSV(variables, completeAPIData);
							}
						}
					}
				}
				apiData.close();

				if (arrNode.get("response").get(0) != null) {
					int status = arrNode.get("response").get(0).get("code").asInt();
					Script script = new Script();
					String[] exec = new String[] {
							"pm.test('Validate Status Code',function () { pm.response.to.have.status(" + status
									+ ");});" };
					script.setExec(exec);
					Event event = new Event();
					event.setScript(script);
					ObjectNode eventNode = (ObjectNode) arrNode;
					eventNode.putArray("event").addPOJO(event);
				}
			}
		}
		new ObjectMapper().writeValue(new File("ModifiedJson.json"), jsonNode);
	}

	private JsonNode updateDataFromCSV(JsonNode node, List<String[]> completeAPIData) {
		if (node.get("value").asText().contains("<") || node.get("value").asText().contains("NaN")) {
			String[] headers = completeAPIData.get(0);
			String key = node.get("key").asText();
			int count = -1;
			boolean keyFound = false;
			for (String header : headers) {
				count = count + 1;
				if (key.equalsIgnoreCase(header)) {
					keyFound = true;
					break;
				}
			}
			if (keyFound && (count != -1)) {
				ObjectNode queryParameter = (ObjectNode) node;
				Random rand = new Random();
				int low = 1;
				int high = completeAPIData.size();
				int row = rand.nextInt(high - low) + low;
				String value = completeAPIData.get(row)[count];
				queryParameter.remove("value");
				queryParameter.put("value", value);
			}
		}
		return node;
	}
	
	private JsonNode updateDataFromCSVAuth(JsonNode node, List<String[]> completeAPIData) {
		if (node.get("value").asText().contains("bearerToken")) {
			String[] headers = completeAPIData.get(0);
			String key = node.get("key").asText();
			int count = -1;
			for (String header : headers) {
				count = count + 1;
				if (key.equalsIgnoreCase(header)) {
					break;
				}
			}
			if (count != -1) {
				ObjectNode queryParameter = (ObjectNode) node;
				Random rand = new Random();
				int low = 1;
				int high = completeAPIData.size();
				int row = rand.nextInt(high - low) + low;
				String value = completeAPIData.get(row)[count];
				queryParameter.remove("value");
				queryParameter.put("value", value);
			}
		}
		return node;
	}
	
	private JsonNode updateDataFromCSVAtNodeValue(JsonNode node, List<String[]> completeAPIData, String nodeValue) {
		if (node.get("value").asText().contains(nodeValue)) {
			String[] headers = completeAPIData.get(0);
			String key = node.get("key").asText();
			int count = -1;
			for (String header : headers) {
				count = count + 1;
				if (key.equalsIgnoreCase(header)) {
					break;
				}
			}
			if (count != -1) {
				ObjectNode queryParameter = (ObjectNode) node;
				
				Random rand = new Random();
				int low = 1;
				int high = completeAPIData.size();
				int row = rand.nextInt(high - low) + low;
				String value = completeAPIData.get(row)[count];
				
				queryParameter.remove("value");
				queryParameter.put("value", value);
			}
		}
		return node;
	}

	private JsonNode updateDataFromCSVAuthBearerToken(JsonNode node, List<String[]> completeAPIData) {
		return updateDataFromCSVAtNodeValue(node, completeAPIData, "bearerToken");
	}

	private JsonNode updateGlobalVariables(JsonNode jsonNode, File dataFile) throws IOException {
		
		// Read Data from CSV and change the values in
		FileReader fileReader = new FileReader(dataFile);
		CSVReader apiData = new CSVReader(fileReader);
		
		List<String[]> completeAPIData = apiData.readAll();
		if (completeAPIData.size() > 0) {
			
			JsonNode authNode = jsonNode.get("auth");
			if((authNode != null) && (!authNode.isEmpty())) {
				
				JsonNode bearerNode = authNode.get("bearer");
				if((bearerNode != null) && (!bearerNode.isEmpty())) {
					// Update Variables
					for (JsonNode variables : bearerNode) {
						variables = updateDataFromCSVAuthBearerToken(variables, completeAPIData);
					}
				}
			}
		}
		apiData.close();
		return jsonNode;
	}
	
	@Override
 	public File runPostmanCollection(MultipartFile collectionJson, MultipartFile dataFile) throws Exception {
		
		StringBuffer stringBuffer = new StringBuffer(System.getProperty("user.dir")).append("/").append("output");
		File outputFolder = new File(stringBuffer.toString());
		if (!outputFolder.exists()) {
			outputFolder.mkdir();
		}
		File postmanCollection = multipartFileToFile(collectionJson, outputFolder.toPath());
		File testDataFile = multipartFileToFile(dataFile, outputFolder.toPath());
		String command = "newman -v";
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}
		ExecCommand execCommand = new ExecCommand(command);
		String error = execCommand.getError();
		if (error.contains("is not recognized")) {
			throw new Exception("newman is not installed properly....");
		}

		command = "newman run " 
		+ postmanCollection.getAbsolutePath() + " "
		+ " -d " + testDataFile.getAbsolutePath()
		+ " -r htmlextra";
		
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}
		execCommand = new ExecCommand(command);
		File newmanDir = new File("newman");
		File[] files = newmanDir.listFiles(File::isFile);
		long lastModifiedTime = Long.MIN_VALUE;
		File chosenFile = null;

		if (files != null) {
			for (File file : files) {
				if (file.lastModified() > lastModifiedTime) {
					chosenFile = file;
					lastModifiedTime = file.lastModified();
				}
			}
		}
		return chosenFile;
	}

	
	@Override
	public File createPostmanCollectionWithDataMap(MultipartFile apiSpec, Map<String, Object> apiDataMap) throws Exception {
		
		StringBuffer stringBuffer = new StringBuffer(System.getProperty("user.dir")).append("/").append("output");
		File outputFolder = new File(stringBuffer.toString());
		if (!outputFolder.exists()) {
			outputFolder.mkdir();
		}
		File apiSpecFile = multipartFileToFile(apiSpec, outputFolder.toPath());
		//File apiDataFile = multipartFileToFile(dataFile, outputFolder.toPath());
		String command = "openapi2postmanv2 --version";
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}
		ExecCommand execCommand = new ExecCommand(command);
		String error = execCommand.getError();
		if (error.contains("is not recognized")) {
			throw new Exception("openapi2postmanv2 is not installed properly....");
		}

		command = "openapi2postmanv2 -s " + apiSpecFile.getAbsolutePath() + " -o collection.json";
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}

		execCommand = new ExecCommand(command);
		String output = execCommand.getOutput();
		if (!output.contains("Conversion successful")) {
			throw new Exception("Conversion Failed, validate the YAML spec....");
		}

		File fileOp = new File("collection.json");
		if (addTestsToRequest(fileOp, apiDataMap)) {
			fileOp = new File("ModifiedJson.json");
		}

		return fileOp;
		
	}

	private boolean addTestsToRequest(File jsonFile, Map<String, Object> apiDataMap) {
		boolean fileModified = true;
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode rootNode = objectMapper.readTree(jsonFile);
			// Updating global variables if any
			// JsonNode updatedJsonNode = updateGlobalVariables(rootNode, apiDataMap);
			// addTestInEvent(updatedJsonNode, apiDataMap);
			addTestInEvent(rootNode, apiDataMap);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			fileModified = false;
		}
		return fileModified;
	}
	
	private JsonNode updateGlobalVariables(JsonNode jsonNode, Map<String, Object> apiDataMap) throws IOException {
		
		// Read Data from CSV and change the values in
		//FileReader fileReader = new FileReader(dataFile);
		//CSVReader apiData = new CSVReader(fileReader);
		
		//List<String[]> completeAPIData = apiData.readAll();
		//if (completeAPIData.size() > 0) {
		if (apiDataMap.size() > 0) {	
			JsonNode authNode = jsonNode.get("auth");
			if((authNode != null) && (!authNode.isEmpty())) {
				
				JsonNode bearerNode = authNode.get("bearer");
				if((bearerNode != null) && (!bearerNode.isEmpty())) {
					// Update Variables
					for (JsonNode variables : bearerNode) {
						variables = updateDataFromCSVAuthBearerToken(variables, apiDataMap);
					}
				}
			}
		}
		//apiData.close();
		return jsonNode;
	}
	
	private JsonNode updateDataFromCSVAuthBearerToken(JsonNode node, Map<String, Object> apiDataMap) {
		return updateDataFromCSVAtNodeValue(node, apiDataMap, "bearerToken");
	}
	
	private JsonNode updateDataFromCSVAtNodeValue(JsonNode node, Map<String, Object> apiDataMap, String nodeValue) {
		
		if (node.get("value").asText().contains(nodeValue)) {
			
			//String[] headers = completeAPIData.get(0);
			String key = node.get("key").asText();
			//int count = -1;
			//for (String header : headers) {
			//	count = count + 1;
			//	if (key.equalsIgnoreCase(header)) {
			//		break;
			//	}
			//}
			//if (count != -1) {
			if (apiDataMap.containsKey(key)) {
				ObjectNode queryParameter = (ObjectNode) node;
				
				//Random rand = new Random();
				//int low = 1;
				//int high = completeAPIData.size();
				//int row = rand.nextInt(high - low) + low;
				String value = (String) apiDataMap.get(key);  //completeAPIData.get(row)[count];
				
				queryParameter.remove("value");
				queryParameter.put("value", value);
			}
		}
		return node;
	}
	
	private void addTestInEvent(JsonNode jsonNode, Map<String, Object> apiDataMap)
			throws StreamWriteException, DatabindException, IOException {
		
		JsonNode nxtNode = jsonNode.get("item");
		for (JsonNode arrNode : nxtNode) {
			if (arrNode.has("item")) {
				addTestInEvent(arrNode, apiDataMap);
			} else {
				// Read Data from CSV and change the values in
				//FileReader fileReader = new FileReader(dataFile);
				//CSVReader apiData = new CSVReader(fileReader);
				//List<String[]> completeAPIData = apiData.readAll();
				if (apiDataMap.size() > 0) {
					// Update Query Parameters
					System.out.println("\nChecking Query Param..");
					if (arrNode.get("request").get("url").get("query") != null) {
						for (JsonNode queryParams : arrNode.get("request").get("url").get("query")) {
							System.out.println("\\nQuery Param..");
							queryParams = updateDataFromMap(queryParams, apiDataMap);
						}
					}
					// Update Variables
					System.out.println("\\nChecking Variables..");
					if(arrNode.get("request").get("url").get("variable") != null) {
						for (JsonNode variables : arrNode.get("request").get("url").get("variable")) {
							variables = updateDataFromMap(variables, apiDataMap);
						}
					}
					System.out.println("\\nChecking mode..");
					if(arrNode.get("request").get("body") != null) {
						if (arrNode.get("request").get("body").has("mode")) {
							String bodyMode = arrNode.get("request").get("body").get("mode").asText();
							if (!bodyMode.equalsIgnoreCase("raw")) {
								// Update body
								for (JsonNode variables : arrNode.get("request").get("body").get(bodyMode)) {
									variables = updateDataFromMap(variables, apiDataMap);
								}
							}
						}
					}
				}
				//apiData.close();

				if (arrNode.get("response").get(0) != null) {
					int status = arrNode.get("response").get(0).get("code").asInt();
					Script script = new Script();
					String[] exec = new String[] {
							"pm.test('Validate Status Code',function () { pm.response.to.have.status(" + status
									+ ");});" };
					script.setExec(exec);
					Event event = new Event();
					event.setScript(script);
					ObjectNode eventNode = (ObjectNode) arrNode;
					eventNode.putArray("event").addPOJO(event);
				}
			}
		}
		new ObjectMapper().writeValue(new File("ModifiedJson.json"), jsonNode);
	}
	
	private JsonNode updateDataFromMap(JsonNode node, Map<String, Object> apiDataMap) {
		//if (node.get("value").asText().contains("<") || node.get("value").asText().contains("NaN")) {
			
			//String[] headers = completeAPIData.get(0);
			String key = node.get("key").asText();
			//int count = -1;			
			//for (String header : headers) {
			//	count = count + 1;
			//	if (key.equalsIgnoreCase(header)) {
			//		break;
			//	}
			//}
			//if (count != -1) {
			if(apiDataMap.containsKey(key)) {
				ObjectNode queryParameter = (ObjectNode) node;
				//Random rand = new Random();
				//int low = 1;
				//int high = completeAPIData.size();
				//int row = rand.nextInt(high - low) + low;
				String value = (String) apiDataMap.get(key); // completeAPIData.get(row)[count];
				queryParameter.remove("value");
				queryParameter.put("value", value);
			}
		//}
		return node;
	}

	
	
	@Override
	public File runPostmanCollection(MultipartFile collectionJson, Map<String, Object> apiData) throws Exception {
		
		StringBuffer stringBuffer = new StringBuffer(System.getProperty("user.dir")).append("/").append("output");
		File outputFolder = new File(stringBuffer.toString());
		if (!outputFolder.exists()) {
			outputFolder.mkdir();
		}
		File postmanCollection = multipartFileToFile(collectionJson, outputFolder.toPath());
		
		//TODO Logic to be added to update the uploaded or attached file with provided Key-Value pairs
		
		//File fileOp = new File("collection.json");
		if (addTestsToRequest(postmanCollection, apiData)) {
			postmanCollection = new File("ModifiedJson.json");
		}
		//TODO To be tested
		
		//File testDataFile = multipartFileToFile(dataFile, outputFolder.toPath());
		String command = "newman -v";
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}
		ExecCommand execCommand = new ExecCommand(command);
		String error = execCommand.getError();
		if (error.contains("is not recognized")) {
			throw new Exception("newman is not installed properly....");
		}

		command = "newman run " 
		+ postmanCollection.getAbsolutePath() + " "
		//+ " -d " + testDataFile.getAbsolutePath()
		+ " -r htmlextra";
		
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command = new StringBuilder("cmd.exe /c ").append(command).toString();
		} else {
			command = new StringBuilder("/bin/sh -c ").append(command).toString();
		}
		execCommand = new ExecCommand(command);
		File newmanDir = new File("newman");
		File[] files = newmanDir.listFiles(File::isFile);
		long lastModifiedTime = Long.MIN_VALUE;
		File chosenFile = null;

		if (files != null) {
			for (File file : files) {
				if (file.lastModified() > lastModifiedTime) {
					chosenFile = file;
					lastModifiedTime = file.lastModified();
				}
			}
		}
		return chosenFile;		
	}
	
	
}
