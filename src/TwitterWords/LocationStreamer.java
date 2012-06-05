package TwitterWords;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import javax.swing.*;
import twitter4j.*;
import twitter4j.auth.AccessToken;

/*
 * A class which streams tweets from Brighton. All tweets are stored in the 
 * statuses arrayList. 
 */
/**
 *
 * @author zackbleach
 */
public class LocationStreamer extends StatusAdapter implements Runnable{

    private TwitterStream twitterStream;
    private StatusListener listener;
    private FilterQuery query;
    public ArrayList<String> statuses;
    
    /*
     * Constructs a new location streamer and overrides onStatus method such that
     * each time a status is collected it is added to the statuses arrayList.
     * This also includes a filter for tweets about the weather.
     * It was found that a large amount of accounts exist in the Brighton area
     * which simply re-tweet the weather at hourly intervals, this was deemed
     * to be uninteresteing information
     * the location you want to filter tweets from is passed in as the 
     * locations parameter. 
     */
    public LocationStreamer(double[][] locations) {
        statuses = new ArrayList<String>();
        twitterStream = new TwitterStreamFactory().getInstance();
        //createAuthGUI();
        query = new FilterQuery();
        query.locations(locations);
        listener = new StatusListener() {

            public void onStatus(Status status) {
                System.out.println(status.getText());
                
                if (status.getUser().getScreenName().equals("thanetweather")||status.getUser().getScreenName().equals("PCKINGuk")||status.getUser().getScreenName().equals("kevinalyons")||status.getUser().getScreenName().equals("andrewgrenn")) {
                    System.out.println("ignore weather");
                }
                else {
                    statuses.add(status.getText());
                }
            }
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
               //System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                //System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            public void onScrubGeo(long userId, long upToStatusId) {
                //System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
    }
    
    /*
     * sets the twitter stream to start filtering by the query, in this case
     * a geobox aroun brighton
     */

    @Override public void run() {
        
        twitterStream.addListener(listener);
        twitterStream.filter(query);
    }
    
    //returns amount of status
    
    public int statusCount() {
        return statuses.size();
    }
    
    //gets status from given position in the statuses array
    public String getStatus(int i) {
        return statuses.get(i);
    }
    
    /*
     * retries last status word was used in
     */
    public String lastStatus(String search){
        String status = "";
        for (int i = 0; i <  statuses.size(); i++){
            if (statuses.get(i).contains(" "+search+" ")||statuses.get(i).contains(" "+search)||statuses.get(i).contains(search+" ")) {
                status = getStatus(i);
            }
        }
        return status;
    }

}
