package graphtutorial;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.gson.JsonElement;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.GiphyRatingType;
import com.microsoft.graph.models.Team;
import com.microsoft.graph.models.TeamFunSettings;
import com.microsoft.graph.models.TeamMemberSettings;
import com.microsoft.graph.models.TeamMessagingSettings;
import com.microsoft.graph.tasks.PageIterator;
import com.microsoft.graph.users.item.calendarview.delta.DeltaGetResponse;


/**
 * Graph Tutorial
 *
 */
public class App {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Java Graph Tutorial");
        System.out.println();
    
        final Properties oAuthProperties = new Properties();
        try {
            oAuthProperties.load(App.class.getResourceAsStream("oAuth.properties"));
        } catch (IOException e) {
            System.out.println("Unable to read OAuth configuration. Make sure you have a properly formatted oAuth.properties file. See README for details.");
            return;
        }
    
        initializeGraph(oAuthProperties);
        getUserAccess();

        DeltaGetResponse deltas = Graph.graphClient.me()
            .calendarView()
            .delta()
            .get(requestConfiguration -> {
                requestConfiguration.queryParameters.startDateTime = "2023-12-04T21:28:37.145Z";
                requestConfiguration.queryParameters.endDateTime = "2023-12-07T21:28:37.145Z";
            });  

        getEvents(deltas);

        Thread.sleep(10000);

        //when you process run out of events, you can use the deltaLink to get the next set of events in the next iteration
        DeltaGetResponse deltas2 = Graph.graphClient.me()
            .calendarView()
            .delta()
            .withUrl(deltas.getOdataDeltaLink())
            .get();
        
        getEvents(deltas2);

        Thread.sleep(10000);

        //when you process run out of events, you can use the deltaLink to get the next set of events in the next iteration
        DeltaGetResponse deltas3 = Graph.graphClient.me()
            .calendarView()
            .delta()
            .withUrl(deltas2.getOdataDeltaLink())
            .get();
        
        getEvents(deltas3);

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

        Graph.graphClient.groups().byGroupId("id").team()
            .put(team);                
    }

    private static void initializeGraph(Properties properties) {
        try {
            Graph.initializeGraphForUserAuth(properties,
                challenge -> System.out.println(challenge.getMessage()));
        } catch (Exception e)
        {
            System.out.println("Error initializing Graph for user auth");
            System.out.println(e.getMessage());
        }
    }

    private static void getUserAccess() {
        try {
            System.out.println(Graph.getUserToken());
        } catch (Exception e) {
            System.out.println("Error getting access token");
            System.out.println(e.getMessage());
        }
    }

    private static void getEvents(DeltaGetResponse deltas) {
        List<Event> events = new LinkedList<Event>();
        
        try {
            PageIterator<Event, DeltaGetResponse> pageIterator = new PageIterator
                .Builder<Event, DeltaGetResponse>()
                .client(Graph.graphClient)
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