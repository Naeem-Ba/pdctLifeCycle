package com.checkmk.pdctLifeCycle.service;

import com.checkmk.pdctLifeCycle.config.CheckmkConfig;
import com.checkmk.pdctLifeCycle.model.CheckmkHostsResponse;
import com.checkmk.pdctLifeCycle.model.Host;
import com.checkmk.pdctLifeCycle.repository.HostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HostImportService {

    private static final Logger logger = LoggerFactory.getLogger(HostImportService.class);

    private final HostRepository hostRepository;
    private final CheckmkConfig checkmkConfig;
    private final ObjectMapper objectMapper;
    private final RestClientService restClientService;

    @Autowired
    public HostImportService(HostRepository hostRepository, CheckmkConfig checkmkConfig, ObjectMapper objectMapper, RestClientService restClientService) {
        this.hostRepository = hostRepository;
        this.checkmkConfig = checkmkConfig;
        this.objectMapper = objectMapper;
        this.restClientService = restClientService;
    }

    public List<Host> getCheckMkHosts() {
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

    public void saveSelectedHosts(List<String> selectedHostIds) {
        List<Host> fetchedHosts = getCheckMkHosts();

        List<Host> selectedHosts = fetchedHosts.stream()
                .filter(host -> selectedHostIds.contains(host.getHostName()))
                .map(this::setHostNameAsId)
                .collect(Collectors.toList());

        hostRepository.saveAll(selectedHosts);
    }

    private Host setHostNameAsId(Host host) {
        host.setId(host.getHostName());
        return host;
    }

    private boolean isHostInDatabase(String hostName) {
        Optional<Host> existingHost = hostRepository.findById(hostName);
        return existingHost.isPresent();
    }

    private Host mapToHost(Host host) {
        Host newHost = new Host();
        newHost.setId(host.getHostName()); // Ensure ID is set to hostName
        newHost.setHostName(host.getHostName());
        newHost.setIpAddress(host.getIpAddress());
        newHost.setCreationDate(host.getCreationDate());
        newHost.setImported(isHostInDatabase(host.getHostName())); // Set imported flag
        return newHost;
    }
}
