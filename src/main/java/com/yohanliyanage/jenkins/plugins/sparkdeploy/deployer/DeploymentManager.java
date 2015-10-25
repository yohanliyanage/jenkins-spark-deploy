/*
 * Copyright (C) 2015 Yohan Liyanage
 *
 * Release under the MIT License (MIT). See LICENSE file for details.
 */

package com.yohanliyanage.jenkins.plugins.sparkdeploy.deployer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the REST client for Spark REST API. Allows to kill, submit and poll status of applications.
 *
 * @author Yohan Liyanage
 */
public class DeploymentManager {

    private URL masterRestUrl;
    private PrintStream logger;
    private ObjectMapper mapper = new ObjectMapper();
    private boolean verbose;

    public DeploymentManager(String masterRestUrl, PrintStream logger, boolean verbose) {
        this.logger = logger;
        this.verbose = verbose;
        try {
            this.masterRestUrl = new URL(Utils.getActualSparkMasterUrl(masterRestUrl));
            logger.println("Spark REST Service endpoint resolved to " + this.masterRestUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Master REST URL: " + masterRestUrl);
        }
    }

    /**
     * Kills the submission with given ID.
     * @param submissionId submission to kill
     * @return true if successful, false otherwise
     * @throws IOException
     */
    public boolean kill(String submissionId) throws IOException {
        URL killUrl = new URL(masterRestUrl, "/v1/submissions/kill/" + submissionId);

        logger.println("[Spark-Deployer] Killing previous submission with ID: " + submissionId);
        Map<String, Object> response = invokePost(killUrl);

        boolean success = (Boolean) response.get("success");

        if (success) {
            logger.println("[Spark-Deployer] Successfully killed previous submission with ID " + submissionId);
        } else {
            logger.println("[Spark-Deployer] Failed to kill previous submission with ID  " + submissionId + " : "
                    + response.get("message"));
        }

        return success;
    }

    /**
     * Submits a Deployment to Spark and returns Submission ID.
     * @param request deployment metadata
     * @return Submission ID.
     * @throws IOException
     */
    public String submit(DeploymentRequest request) throws IOException {
        URL createUrl = new URL(masterRestUrl, "/v1/submissions/create");
        logger.println("[Spark-Deployer] Submitting Spark Application...");

        String payload;

        if (verbose) {
            payload = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        } else {
            payload = mapper.writeValueAsString(request);
        }

        Map<String, Object> response = invokePost(createUrl, payload);

        boolean success = (Boolean) response.get("success");

        if (! success) {
            throw new RuntimeException("Submission Failed. Response success flag is false");
        }

        return (String) response.get("submissionId");
    }

    /**
     * Returns Driver State for given Submission ID.
     * @param submissionId submission id.
     * @return driver state (ex. "RUNNING").
     * @throws IOException
     */
    public String getDriverState(String submissionId) throws IOException {
        URL statusUrl = new URL(masterRestUrl, "/v1/submissions/status/" + submissionId);

        Map<String, Object> response = invokeGet(statusUrl);

        boolean success = (Boolean) response.get("success");

        if (! success) {
            throw new RuntimeException("Status check failed for submission " + submissionId + " : " + response.get("message"));
        }

        return (String) response.get("driverState");
    }

    private Map<String, Object> invokePost(URL url) throws IOException {
        return invokeUrl("POST", url, null);
    }

    private Map<String, Object> invokePost(URL url, String payload) throws IOException {
        return invokeUrl("POST", url, payload);
    }

    private Map<String, Object> invokeGet(URL url) throws IOException {
        return invokeUrl("GET", url, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeUrl(String method, URL url, String payload) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");

        if (payload != null) {
            if (verbose) {
                logger.println("[Spark-Deployer] VERBOSE : Invoking URL: " + method + " " + url.toString()
                        + " with payload : \n" + payload);
            }
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStream os = connection.getOutputStream();
            os.write(payload.getBytes());
            os.flush();
        } else {
            if (verbose) {
                logger.println("[Spark-Deployer] VERBOSE : Invoking URL " + method + " " + url.toString());
            }
        }

        InputStream responseStream;

        if (isSuccessResponseCode(connection.getResponseCode())) {
            // 2xx - Success
            responseStream = connection.getInputStream();
        } else {
            responseStream = connection.getErrorStream();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(responseStream));

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line).append(System.lineSeparator());
        }

        if (verbose) {
            logger.println("[Spark-Deployer] VERBOSE : Response from Spark : \n" + response);
        }

        if (! isSuccessResponseCode(connection.getResponseCode())) {
            throw new RuntimeException("Operation Failed. Response is " +
                    connection.getResponseCode() + " : " + connection.getResponseMessage());
        }

        return (Map) mapper.readValue(response.toString(), HashMap.class);
    }

    private boolean isSuccessResponseCode(int responseCode) throws IOException {
        return responseCode >= 200 && responseCode < 300;
    }
}
