/*package micro.eurekaservice.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
//import org.springframework.security.core.parameters.P;

import java.util.Collections;
import java.util.List;

@Configuration
@Profile("offline")
public class OfflineEurekaClientConfig {

    @Bean
    @Primary
    public EurekaClient eurekaClient(
            ApplicationInfoManager applicationInfoManager,
            EurekaClientConfig config,
            TransportClientFactories transportClientFactories,
            ApplicationEventPublisher applicationEventPublisher) {

        return new CloudEurekaClient(
                applicationInfoManager,
                config,
                transportClientFactories,
                applicationEventPublisher
        ) {
            @Override
            public Applications getApplicationsForARegion(String region) {
                return new Applications();
            }
            @Override
            public void shutdown() {
                // No hacer nada
            }

            @Override
            public Applications getApplications() {
                return new Applications(); // vacías
            }

            @Override
            public Application getApplication(String appName) {
                return null;
            }

            @Override
            public List<InstanceInfo> getInstancesById(String id) {
                return Collections.emptyList();
            }

            @Override
            public void registerHealthCheck(com.netflix.appinfo.HealthCheckHandler healthCheckHandler) {
                // No hacer nada
            }

            public void register() {
                // No hacer nada
            }

            public boolean cancel(String appName, String id) {
                return true;
            }
        };
    }
}
*/