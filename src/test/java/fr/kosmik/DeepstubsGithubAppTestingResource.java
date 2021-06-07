package fr.kosmik;

import io.quarkiverse.githubapp.testing.internal.GitHubAppTestingCallback;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

import java.util.HashMap;
import java.util.Map;

public class DeepstubsGithubAppTestingResource implements QuarkusTestResourceConfigurableLifecycleManager {
    @Override
    public Map<String, String> start() {
        Map<String, String> systemProperties = new HashMap<>();
        GitHubAppTestingCallback.enable(systemProperties);
        GitHubAppTestingCallback.enableDeepStubs(systemProperties);
        return systemProperties;
    }

    @Override
    public void stop() {

    }
}
