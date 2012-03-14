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
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
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
    
    private AccessToken accessToken;
    static JFrame frame;
    
    public static String authURL;

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
                    System.out.println("fuck you weather");
                }
                else {
                    statuses.add(status.getText());
                }
            }
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
    }

    @Override public void run() {

        twitterStream.addListener(listener);
        twitterStream.filter(query);
    }
    
    public int statusCount() {
        return statuses.size();
    }
    public String getStatus(int i) {
        return statuses.get(i);
    }
    
    public String lastStatus(String search){
        String status = "";
        for (int i = 0; i <  statuses.size(); i++){
            if (statuses.get(i).contains(" "+search+" ")||statuses.get(i).contains(" "+search)||statuses.get(i).contains(search+" ")) {
                status = getStatus(i);
            }
        }
        return status;
    }
    
       private static void createAuthGUI(){
        
           frame = new JFrame("Twittersphere");
        frame.setResizable(false);
        JPanelWithBackground jBg = null;
        try {
            jBg = new JPanelWithBackground("background.png");
        } catch (Exception e) {
        }


        //frame.setUndecorated(true);

        Dimension size = new Dimension(375, 350);
        frame.setSize(size);

        // Get the size of the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        // Determine the new location of the window

        int w = frame.getSize().width;
        System.out.println(frame.getSize().width);
        int h = frame.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;

        // Move the window
        frame.setLocation(x, y - 50);

        ImageIcon image = new ImageIcon("twitter.png");
        JLabel label = new JLabel("", image, JLabel.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        jBg.add(label);

        jBg.add(Box.createRigidArea(new Dimension(0, 25)));



        jBg.add(new JLabel("Twitter needs to know if it's ok for this app to access") {

            {
                setForeground(Color.white);
                setAlignmentX(Component.CENTER_ALIGNMENT);

            }
        });
        jBg.add(new JLabel("your account. Please click 'Visit Twitter' to authorise.") {

            {
                setForeground(Color.white);
                setAlignmentX(Component.CENTER_ALIGNMENT);

            }
        });

        jBg.add(Box.createRigidArea(new Dimension(0, 25)));


        final JButton ok = new JButton("Visit Twitter");
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                //Execute when button is pressed
                System.out.println("You clicked the button");
                try {
                    java.awt.Desktop.getDesktop().browse(new URI(authURL));

                } catch (Exception e1) {
                    //will never through an exception beacuse URI is hard coded
                }

            }
        });


        jBg.add(ok);
        jBg.add(Box.createRigidArea(new Dimension(0, 5)));


        //this.add(auth);
        JLabel name = new JLabel("Twittersphere version 2.12");
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        name.setFont(name.getFont().deriveFont(8.0f));
        name.setForeground(Color.white);

        jBg.add(name);
        jBg.add(Box.createRigidArea(new Dimension(0, 10)));

        try {
            frame.getContentPane().add(jBg);
        } catch (Exception e) {
            System.out.println(e);
        }
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
    }
}
