package graphtutorial;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.google.gson.JsonElement;
import com.microsoft.graph.models.AppRoleAssignmentCollectionResponse;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.GiphyRatingType;
import com.microsoft.graph.models.Team;
import com.microsoft.graph.models.TeamFunSettings;
import com.microsoft.graph.models.TeamMemberSettings;
import com.microsoft.graph.models.TeamMessagingSettings;
import com.microsoft.graph.core.tasks.PageIterator;
import com.microsoft.graph.users.item.calendarview.delta.DeltaGetResponse;

/**
 * Graph Tutorial
 *
 */
public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Java Graph Tutorial");
        System.out.println();
    
        final Properties properties = new Properties();
        properties.load(App.class.getResourceAsStream("oAuth.properties"));
    
        final String clientId = properties.getProperty("app.clientId");
        final String tenantId = properties.getProperty("app.tenantId");
        final String[] graphUserScopes = properties.getProperty("app.graphUserScopes").split(",");

        DeviceCodeCredential deviceCodeCredential = new DeviceCodeCredentialBuilder()
            .clientId(clientId)
            .tenantId(tenantId)
            .challengeConsumer(challenge -> System.out.println(challenge.getMessage()))
            .build();

        //create a client for calling v1.0 endpoint
        com.microsoft.graph.serviceclient.GraphServiceClient graphClient = 
            new com.microsoft.graph.serviceclient.GraphServiceClient(
                deviceCodeCredential,
                graphUserScopes);
        
        com.microsoft.graph.models.User me = graphClient.me().get();
        System.out.println("V1: " + me.getDisplayName());

        //create a client for calling beta endpoint
        com.microsoft.graph.beta.serviceclient.GraphServiceClient betaGraphClient = 
            new com.microsoft.graph.beta.serviceclient.GraphServiceClient(
                deviceCodeCredential,
                graphUserScopes);
        
        com.microsoft.graph.beta.models.User meBeta = betaGraphClient.me().get();
        System.out.println("Beta: " + meBeta.getDisplayName());
        
        AppRoleAssignmentCollectionResponse response = graphClient.me().appRoleAssignments().get();
        response.getValue().forEach(role -> {
            System.out.println(role.getPrincipalDisplayName() + "\n");
        });

        String eventId = properties.getProperty("app.eventId");
        System.out.println(graphClient.me().calendar().events().byEventId(eventId).toGetRequestInformation().getUri());

        // get the object 
        Event event = graphClient.me()
        .calendar()
        .events()
        .byEventId(eventId)
        .get(requestConfiguration -> {
            requestConfiguration.queryParameters.select = 
                new String[] {"subject,body,bodyPreview,organizer,attendees,start,end,location,locations"};
        });

        // the backing store will keep track that the property change and send the updated value. 
        event.setRecurrence(null); // set to null  

        // update the object 
        graphClient.me()
            .events()
            .byEventId("event-id")
            .patch(event);

        DeltaGetResponse deltas = graphClient.me()
            .calendarView()
            .delta()
            .get(requestConfiguration -> {
                requestConfiguration.queryParameters.startDateTime = "2023-12-04T21:28:37.145Z";
                requestConfiguration.queryParameters.endDateTime = "2023-12-07T21:28:37.145Z";
            });  

        getEvents(deltas, graphClient);

        Thread.sleep(10000);

        //when you process run out of events, you can use the deltaLink to get the next set of events in the next iteration
        DeltaGetResponse deltas2 = graphClient.me()
            .calendarView()
            .delta()
            .withUrl(deltas.getOdataDeltaLink())
            .get();
        
        getEvents(deltas2, graphClient);

        Thread.sleep(10000);

        //when you process run out of events, you can use the deltaLink to get the next set of events in the next iteration
        DeltaGetResponse deltas3 = graphClient.me()
            .calendarView()
            .delta()
            .withUrl(deltas2.getOdataDeltaLink())
            .get();
        
        getEvents(deltas3, graphClient);

        Team team = new Team();
        TeamMemberSettings memberSettings = new TeamMemberSettings();
        memberSettings.setAllowCreatePrivateChannels(true);
        memberSettings.setAllowCreateUpdateChannels(true);
        team.setMemberSettings(memberSettings);
        TeamMessagingSettings messagingSettings = new TeamMessagingSettings();
        messagingSettings.setAllowUserEditMessages(true);
        messagingSettings.setAllowUserDeleteMessages(true);
        team.setMessagingSettings(messagingSettings);
        TeamFunSettings funSettings = new TeamFunSettings();
        funSettings.setAllowGiphy(true);
        funSettings.setGiphyContentRating(GiphyRatingType.Strict);
        team.setFunSettings(funSettings);

        graphClient.groups().byGroupId("id").team()
            .put(team);                
    }

    public static String getUserToken(Properties properties, DeviceCodeCredential deviceCodeCredential) throws Exception {
        // Ensure credential isn't null
        if (deviceCodeCredential == null) {
            throw new Exception("Graph has not been initialized for user auth");
        }
    
        final String[] graphUserScopes = properties.getProperty("app.graphUserScopes").split(",");
    
        final TokenRequestContext context = new TokenRequestContext();
        context.addScopes(graphUserScopes);
    
        final AccessToken token = deviceCodeCredential.getToken(context).block();
        return token.getToken();
    }

    private static void getEvents(DeltaGetResponse deltas, com.microsoft.graph.serviceclient.GraphServiceClient graphClient) {
        List<Event> events = new LinkedList<Event>();
        
        try {
            PageIterator<Event, DeltaGetResponse> pageIterator = new PageIterator
                .Builder<Event, DeltaGetResponse>()
                .client(graphClient)
                .collectionPage(deltas)
                .collectionPageFactory(DeltaGetResponse::createFromDiscriminatorValue)
                .processPageItemCallback(delta -> {
                    events.add(delta);
                    return true;
                })
                .build();
            
            //This will iterate follow through the odata.nextLink until the last page is reached with an odata.deltaLink
            pageIterator.iterate();

            for (Event event : events) {
                //manipulate removed events
                if (event.getAdditionalData().get("@removed") != null) {
                    JsonElement jsonElement = (JsonElement) event.getAdditionalData().get("@removed");
                    String reason = jsonElement.getAsJsonObject().get("reason").getAsString();
                    if ("deleted".equals(reason)){
                        //do something
                        System.out.println("deleted");
                    }
                } else { //manipulate events
                    String subject = event.getSubject();
                    String id = event.getId();
                    System.out.println(subject + " " + id);
                }
            }            
        } catch (Exception e) {
            System.out.println("Error getting events");
            System.out.println(e.getMessage());
        }
    }

}