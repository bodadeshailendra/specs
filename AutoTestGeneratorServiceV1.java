/**
 * 
 */
package com.ai.testgenerator.service;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * 
 */
public interface AutoTestGeneratorService {
	
	public File createPostmanCollection(MultipartFile apiSpec, MultipartFile dataFile) throws Exception;
	
	public File createPostmanCollectionWithDataMap(MultipartFile apiSpec, Map<String, Object> apiData) throws Exception;
	
	public File createPostmanCollectionWithDataMap(MultipartFile apiSpec, Map<String, Object> apiData, List<MultipartFile> files) throws Exception;

	public File runPostmanCollection(MultipartFile collectionJson) throws Exception;
	
	public File runPostmanCollection(MultipartFile collectionJson, MultipartFile dataCsvFile) throws Exception;
	
	public File runPostmanCollection(MultipartFile collectionJson, Map<String, Object> apiData) throws Exception;

}
