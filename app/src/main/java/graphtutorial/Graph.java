package graphtutorial;

import java.util.Properties;
import java.util.function.Consumer;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.DeviceCodeInfo;

import com.microsoft.serviceclient.GraphServiceClient;

/**
 * Authentication
 */
public class Graph {

    private static Properties _properties;
    private static DeviceCodeCredential _deviceCodeCredential;
    static GraphServiceClient graphClient;

    public static void initializeGraphForUserAuth(Properties properties, Consumer<DeviceCodeInfo> challenge) throws Exception {
        // Ensure properties isn't null
        if (properties == null) {
            throw new Exception("Properties cannot be null");
        }

        _properties = properties;

        final String clientId = properties.getProperty("app.clientId");
        final String tenantId = properties.getProperty("app.tenantId");
        final String[] graphUserScopes = properties.getProperty("app.graphUserScopes").split(",");

        _deviceCodeCredential = new DeviceCodeCredentialBuilder()
            .clientId(clientId)
            .tenantId(tenantId)
            .challengeConsumer(challenge)
            .build();

        /*final TokenCredentialAuthProvider authProvider =
            new TokenCredentialAuthProvider(graphUserScopes, _deviceCodeCredential);

        graphClient = GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient();
        */

        graphClient = new GraphServiceClient(_deviceCodeCredential, graphUserScopes);
    }

    public static String getUserToken() throws Exception {
        // Ensure credential isn't null
        if (_deviceCodeCredential == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }
    
        final String[] graphUserScopes = _properties.getProperty("app.graphUserScopes").split(",");
    
        final TokenRequestContext context = new TokenRequestContext();
        context.addScopes(graphUserScopes);
    
        final AccessToken token = _deviceCodeCredential.getToken(context).block();
        return token.getToken();
    }
}