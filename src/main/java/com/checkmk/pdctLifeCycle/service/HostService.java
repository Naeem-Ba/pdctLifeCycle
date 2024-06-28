package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.checkmk.pdctLifeCycle.exception.HostServiceException;
import com.checkmk.pdctLifeCycle.model.CheckmkHostsResponse;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.repository.HostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HostService {

    private static final Logger logger = LoggerFactory.getLogger(HostService.class);

    private final HostRepository hostRepository;
    private final CheckmkConfig checkmkConfig;
    private final ObjectMapper objectMapper;
    private final RestClientService restClientService;

    @Autowired
    public HostService(HostRepository hostRepository, CheckmkConfig checkmkConfig, ObjectMapper objectMapper, RestClientService restClientService) {
        this.hostRepository = hostRepository;
        this.checkmkConfig = checkmkConfig;
        this.objectMapper = objectMapper;
        this.restClientService = restClientService;
    }

    public List<Host> getAllHosts() {
        return hostRepository.findAll();
    }

    public Host getHostById(String id) {
        return hostRepository.findById(id).orElse(null);
    }

    public List<Host> getCheckMkHost() {
        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/host_config/collections/all";
        try {
            ResponseEntity<CheckmkHostsResponse> response = restClientService.sendGetRequest(apiUrl, CheckmkHostsResponse.class);
            CheckmkHostsResponse checkmkHostsResponse = response.getBody();
            List<Host> hosts = checkmkHostsResponse != null ? checkmkHostsResponse.getHosts() : List.of();

            return hosts.stream()
                    .map(this::mapToHost)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to fetch hosts from Checkmk", e);
            return List.of();
        }
    }

    public Host addHost(Host host) throws HostServiceException {
        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/host_config/collections/all";

        try {
            // Payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("folder", "/");
            payload.put("host_name", host.getHostName());

            ObjectNode attribute = objectMapper.createObjectNode();
            attribute.put("ipaddress", host.getIpAddress());
            payload.set("attributes", attribute);

            // Save in Checkmk
            restClientService.sendPostRequest(apiUrl, payload.toString());
            this.checkmkActivateChanges();

            // Save in Database
            return hostRepository.save(host);
        } catch (Exception e) {
            logger.error("Couldn't add a new host", e);
            throw new HostServiceException("Couldn't add a new host", e);
        }
    }

    public Host updateHost(Host host) throws HostServiceException {
        try {
            Host existingHost = getHostById(host.getId());
            if (existingHost != null) {
                String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + host.getHostName();

                // Payload
                ObjectNode payload = objectMapper.createObjectNode();
                ObjectNode attributes = objectMapper.createObjectNode();
                attributes.put("ipaddress", host.getIpAddress());
                payload.set("attributes", attributes);

                restClientService.sendPutRequest(apiUrl, payload.toString(), restClientService.getEtag(apiUrl));
                this.checkmkActivateChanges();
                return hostRepository.save(host);
            } else {
                throw new HostServiceException("Host not found");
            }
        } catch (Exception e) {
            logger.error("Couldn't update the host", e);
            throw new HostServiceException("Couldn't update the host", e);
        }
    }

    public void deleteHost(String id) throws HostServiceException {
        try {
            Host host = this.getHostById(id);
            if (host != null) {
                String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/objects/host_config/" + host.getHostName();
                restClientService.sendDeleteRequest(apiUrl, restClientService.getEtag(apiUrl));
                this.checkmkActivateChanges();
                hostRepository.delete(host);
            } else {
                throw new HostServiceException("Host not found");
            }
        } catch (Exception e) {
            logger.error("Couldn't delete the host", e);
            throw new HostServiceException("Couldn't delete the host", e);
        }
    }

    public void checkmkActivateChanges() {
        String apiUrl = checkmkConfig.getApiUrl() + "/api/1.0/domain-types/activation_run/actions/activate-changes/invoke";

        // Payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("redirect", false);
        payload.putArray("sites").add(checkmkConfig.getCheckmkSite());
        payload.put("force_foreign_changes", true);

        // Send request
        restClientService.sendPostRequest(apiUrl, payload.toString());
    }

    private Host mapToHost(Host host) {
        return new Host(host.getHostName(), host.getIpAddress(), host.getCreationDate());
    }
}
