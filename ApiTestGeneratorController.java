/**
 * 
 */
package com.ai.testgenerator.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ai.testgenerator.service.AutoTestGeneratorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import jakarta.validation.constraints.NotNull;

/**
 * 
 */
@RestController
@CrossOrigin
@OpenAPIDefinition
//@RequestMapping("/v1")
public class ApiTestGeneratorController {

	@Autowired
	AutoTestGeneratorService autoTestGeneratorService;

	@PostMapping(name = "CreatePostmanCollection", value = "/createPostmanCollection", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Resource> createPostmanCollection(
			@RequestPart(required = true, name = "apiSpec") @NotNull MultipartFile apiSpec,
			@RequestPart(name = "apiData") MultipartFile dataFile) throws Exception {
		File file = autoTestGeneratorService.createPostmanCollection(apiSpec, dataFile);
		Path path = Paths.get(file.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
		header.add("Cache-Control", "no-cache, no-store, must-revalidate");
		header.add("Pragma", "no-cache");
		header.add("Expires", "0");

		return ResponseEntity.ok().headers(header).contentLength(file.length())
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}

	@PostMapping(name = "CreatePostmanCollectionWithDataMap", value = "/createPostmanCollectionWithDataMap", consumes = {
			MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource> createPostmanCollection(
			@RequestPart(required = true, name = "apiSpec") @NotNull MultipartFile apiSpec,
			@RequestParam HashMap<String, Object> dataMap) throws Exception { // @RequestParam(name = "apiData")
																				// Map<String, String> keyValuePairs

		// ObjectMapper objectMapper = new ObjectMapper();
		// HashMap<String, String> map = objectMapper.readValue(jsonString, new
		// TypeReference<HashMap<String, String>>(){});

		File file = autoTestGeneratorService.createPostmanCollectionWithDataMap(apiSpec, dataMap);
		Path path = Paths.get(file.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
		header.add("Cache-Control", "no-cache, no-store, must-revalidate");
		header.add("Pragma", "no-cache");
		header.add("Expires", "0");

		return ResponseEntity.ok().headers(header).contentLength(file.length())
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}

	@PostMapping(name = "RunPostmanCollection", value = "/runPostmanCollection", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Resource> runPostmanCollection(
			@RequestPart(required = true, name = "postmanCollection") @NotNull MultipartFile postmanCollection)
			throws Exception {
		File file = autoTestGeneratorService.runPostmanCollection(postmanCollection);
		Path path = Paths.get(file.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
		header.add("Cache-Control", "no-cache, no-store, must-revalidate");
		header.add("Pragma", "no-cache");
		header.add("Expires", "0");

		return ResponseEntity.ok().headers(header).contentLength(file.length())
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}

	@PostMapping(name = "RunPostmanCollectionWithDataFile", value = "/runPostmanCollectionWithDataFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Resource> runPostmanCollection(
			@RequestPart(required = true, name = "postmanCollection") @NotNull MultipartFile postmanCollection,
			@RequestPart(name = "dataCsvFile") MultipartFile dataCsvFile) throws Exception {
		File file = autoTestGeneratorService.runPostmanCollection(postmanCollection, dataCsvFile);
		Path path = Paths.get(file.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
		header.add("Cache-Control", "no-cache, no-store, must-revalidate");
		header.add("Pragma", "no-cache");
		header.add("Expires", "0");

		return ResponseEntity.ok().headers(header).contentLength(file.length())
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}

	@PostMapping(name = "RunPostmanCollectionWithDataMap", value = "/runPostmanCollectionWithDataMap", consumes = {
			MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource> runPostmanCollection(
			@RequestPart(required = true, name = "postmanCollection") @NotNull MultipartFile postmanCollection,
			@RequestParam HashMap<String, Object> dataMap) throws Exception {
		
		File file = autoTestGeneratorService.runPostmanCollection(postmanCollection, dataMap);
		Path path = Paths.get(file.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
		header.add("Cache-Control", "no-cache, no-store, must-revalidate");
		header.add("Pragma", "no-cache");
		header.add("Expires", "0");

		return ResponseEntity.ok().headers(header).contentLength(file.length())
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
	}

}
