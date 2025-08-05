package micro.eurekaservice.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.EurekaServerHttpClientFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistry;

import java.io.File;
import java.io.IOException;
// Imports no utilizados como java.nio.file.Files y java.nio.file.Paths pueden ser eliminados si no se usan en otra parte.
import java.util.HashMap;
import java.util.Map;

public class PersistentInstanceRegistry extends InstanceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(PersistentInstanceRegistry.class);
    private static final String CACHE_FILE_PATH = "/data/eureka_cache.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PersistentInstanceRegistry(EurekaServerConfig serverConfig,
                                      EurekaClientConfig clientConfig,
                                      ServerCodecs serverCodecs,
                                      @Qualifier("offlineDummyEurekaClient") EurekaClient eurekaClient,
                                      EurekaServerHttpClientFactory httpClientFactory) {
        super(
                serverConfig,
                clientConfig,
                serverCodecs,
                eurekaClient,
                httpClientFactory,
                1,
                0
        );
        logger.info("PersistentInstanceRegistry inicializado. Cache file: {}", CACHE_FILE_PATH);
    }

    @Override
    public void register(InstanceInfo info, boolean isReplication) {
        logger.debug("Registering instance: {} with isReplication: {}", info.getInstanceId(), isReplication);
        super.register(info, isReplication);
        saveToFile();
    }

    @Override
    public void openForTraffic(ApplicationInfoManager applicationInfoManager, int count) {
        logger.info("Opening for traffic. Loading instances from cache file if exists.");
        loadFromFile();
        super.openForTraffic(applicationInfoManager, count);
    }

    @Override
    public boolean cancel(String appName, String serverId, boolean isReplication) {
        logger.debug("Canceling instance: {}/{} with isReplication: {}", appName, serverId, isReplication);
        boolean result = super.cancel(appName, serverId, isReplication);
        if (result) {
            saveToFile();
        }
        return result;
    }

    private void saveToFile() {
        try {
            File cacheFile = new File(CACHE_FILE_PATH);
            File parentDir = cacheFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs()) {
                    logger.info("Created directory for cache file: {}", parentDir.getAbsolutePath());
                } else {
                    logger.error("Failed to create directory for cache file: {}", parentDir.getAbsolutePath());
                    return;
                }
            }

            Map<String, Data> simplifiedRegistry = new HashMap<>();
            this.getSortedApplications().forEach(app ->
                    app.getInstances().forEach(instance -> {
                        if (instance.getStatus() != null &&
                                instance.getStatus() != InstanceInfo.InstanceStatus.DOWN && // Corregido: CANCELLED no es un estado estándar, DOWN sí.
                                instance.getStatus() != InstanceInfo.InstanceStatus.UNKNOWN) {
                            simplifiedRegistry.put(
                                    instance.getInstanceId(),
                                    new Data(
                                            instance.getAppName(),
                                            instance.getHostName(),
                                            instance.getIPAddr(),
                                            instance.getPort(),
                                            instance.getStatus(),
                                            instance.getDataCenterInfo() != null ? instance.getDataCenterInfo().getName() : DataCenterInfo.Name.MyOwn
                                    )
                            );
                        }
                    })
            );
            objectMapper.writeValue(cacheFile, simplifiedRegistry);
            logger.info("Successfully saved {} instances to cache file: {}", simplifiedRegistry.size(), CACHE_FILE_PATH);
        } catch (IOException e) {
            logger.error("Error saving instances to cache file: {}", CACHE_FILE_PATH, e);
        }
    }

    private void loadFromFile() {
        File file = new File(CACHE_FILE_PATH);
        if (file.exists() && file.length() > 0) {
            try {
                Map<String, Data> registryMap = objectMapper.readValue(
                        file,
                        new TypeReference<Map<String, Data>>() {});

                logger.info("Loading {} instances from cache file: {}", registryMap.size(), CACHE_FILE_PATH);

                for (Map.Entry<String, Data> entry : registryMap.entrySet()) {
                    Data data = entry.getValue();
                    InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder()
                            .setInstanceId(entry.getKey())
                            .setAppName(data.getAppName())
                            .setHostName(data.getHostName())
                            .setIPAddr(data.getIpAddr())
                            .setPort(data.getPort())
                            .setStatus(data.getStatus() != null ? data.getStatus() : InstanceInfo.InstanceStatus.UP);

                    final DataCenterInfo.Name loadedName = data.getDataCenterName();
                    builder.setDataCenterInfo(new DataCenterInfo() {
                        @Override
                        public Name getName() {
                            return loadedName != null ? loadedName : DataCenterInfo.Name.MyOwn;
                        }
                    });

                    // *** CORRECCIÓN AQUÍ ***
                    // LeaseInfo es una clase de primer nivel, no una interna de InstanceInfo.
                    // Su Builder también se accede directamente desde LeaseInfo.
                    LeaseInfo leaseInfo = LeaseInfo.Builder.newBuilder() // Acceso directo a LeaseInfo.Builder
                            .setRenewalIntervalInSecs(30) // Valores típicos
                            .setDurationInSecs(90)        // Valores típicos
                            .build();
                    builder.setLeaseInfo(leaseInfo); // El método setLeaseInfo en InstanceInfo.Builder espera un com.netflix.appinfo.LeaseInfo

                    // Estos métodos deberían estar bien si eureka-client es 2.0.x
                    builder.setHomePageUrl("/info", null); // (relativeUrl, explicitUrl)
                    builder.setStatusPageUrl("/health", null); // (relativeUrl, explicitUrl)
                    builder.setHealthCheckUrls("/health", null, null); // (relativeUrl, explicitUrl, secureExplicitUrl)

                    InstanceInfo instanceToRegister = builder.build();
                    super.register(instanceToRegister, true);
                    logger.debug("Loaded and registered instance from cache: {}", instanceToRegister.getInstanceId());
                }
            } catch (IOException e) {
                logger.error("Error loading instances from cache file: {}", CACHE_FILE_PATH, e);
            }
        } else {
            logger.info("Cache file not found or is empty, no instances loaded: {}", CACHE_FILE_PATH);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class Data {
        private String appName;
        private String hostName;
        private String ipAddr;
        private int port;
        private InstanceInfo.InstanceStatus status;
        private DataCenterInfo.Name dataCenterName;
    }
}