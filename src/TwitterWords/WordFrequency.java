package TwitterWords;

import processing.core.*;
import java.util.*;
import fullscreen.*;
import java.awt.*;
import sms.*;

/**
 *
 */
public class WordFrequency extends PApplet {

    // extends PApplet so it can use processing.core framework
    /*
     * Main method launches processing applet
     */
    public static void main(String[] args) {

        PApplet.main(new String[]{"TwitterWords.WordFrequency"});
    }
    int noWords = 0;
    int noSpaces = 0;
    PFont font, font1, font2, font3, font4;
    String[] lines;
    String[] stop_words;
    int twitterWordsSize;
    int previousWordsSize; //used to calculate last words size to form bubble
    int dragging = -1;
    int amountOfWords = 0;
    float totalArea;
    int spaceDown;
    int spaceUp;
    int spaceRight;
    int spaceLeft;
    int maximumBubbles = 49;
    int bubblesPlotted = 0;
    int wordColor;
    Ball[] balls = new Ball[0];
    float gravity = 1.40f;
    float bounce = 0.85f;
    float friction = 0.90f;
    boolean show_info = false;
    boolean full_screen = false;
    boolean last_tweet = false;
    boolean motion_sensor = false;
    int currentSMSx;
    int currentSMSy;
    int currentSMSz;
    int startSMSx;
    int startSMSy;
    FullScreen fs;
    Dimension scrnsize;
    LocationStreamer loc;
    int last_tweetCounter = 0;
    ArrayList<String> alreadySeen;
    int lastCount = 0;

    @Override
    public void setup() {

        //gets starting position for motion sensors - will change when moved
        currentSMSx = sms.Unimotion.getSMSX();
        currentSMSy = sms.Unimotion.getSMSY();
        currentSMSz = sms.Unimotion.getSMSZ();


        double[][] locations = new double[2][2];
        locations[0][0] = -0.222816;
        locations[0][1] = 50.813528;
        locations[1][0] = -0.029526;
        locations[1][1] = 50.908445;
        loc = new LocationStreamer(locations);
        loc.run();

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        // Get the current screen size
        scrnsize = toolkit.getScreenSize();
        size(1000, 600);
        fs = new FullScreen(this);
        background(0);
        smooth();
        loop();

        alreadySeen = new ArrayList<String>();

        font = createFont("Times-Roman", 48);
        font1 = createFont("Helvetica-Bold", 18);
        font2 = createFont("Helvetica", 18);
        font3 = createFont("Times-Roman", 14);
        font4 = createFont("Heletica", 10);

        // calculateBubbleSize();

        stop_words = loadStrings("english_stopwords.txt");


    }

    public ArrayList toWordsTwitter(String line) {

        ArrayList<String> myWords = new ArrayList<String>();
        String pattern = "([a-zA-Z]+[^a-zA-Z]*[a-zA-Z]*)";
        String delimiter = "(\\s+)";
        String emoPattern = "([:|;|8][-|']*[(|)|p|D|o|O|L|s|S|x|X])";
        String hashTag = "#[a-zA-Z0-9]+";
        String[] tempWords = line.split(delimiter);
        for (String word : tempWords) {
            if (word.matches(pattern)) {
                word = word.replaceAll("[^A-Za-z0-9']", "");
                myWords.add(word);
            }
            if (word.matches(emoPattern) || word.matches(hashTag)) {
                myWords.add(word);
            }
        }
        return myWords;
    }

    /**
     * Draw method gets current motion sensor readings sets the word color and
     * then calls the getTweets(), createBubbles(), updateBubblesDisplay() and
     * shakeRandom() methods. These render the word bubbles on screen and
     * animate them. Then it calls the layout() method which sets the text on
     * the screen.
     */
    @Override
    public void draw() {


        background(0);

        //gets current motion sensor readings

        currentSMSx = sms.Unimotion.getSMSX();
        currentSMSy = sms.Unimotion.getSMSY();
        currentSMSz = sms.Unimotion.getSMSZ();


        //sets word color to gold (defined in setup)
        wordColor = color(120, 102, 0);

        getTweets();

        updateBubbles();

        updateBubblesDisplay();

        shakeRandom(1000);

        layout(font1, font2, font4);

        //1 in 1000 chance of random re-arrangement
    }

    /*
     * shakes word bubbles on screen wiht a random probability set by the chance
     * variable, in the form of 1/chance
     */
    void shakeRandom(int chance) {
        Random random = new Random();

        int wobble = random.nextInt(chance);
        if (wobble == 1) {
            for (int i = 0; i < balls.length; i++) {
                balls[i].x = random(balls[i].radius + spaceLeft, width - spaceRight - balls[i].radius);
                balls[i].y = random(balls[i].radius + spaceUp, height - spaceDown - balls[i].radius);
            }
        }

    }

    /*
     * updats the word bubbles on screen, for each bubble the bounce(),
     * collide(), move(), above() and display() methods are called. Fals is
     */
    void updateBubblesDisplay() {
        for (int i = maximumBubbles; i >= 0; i--) {
            if (i < balls.length) {
                if (motion_sensor) {
                    balls[i].fall();
                }
                balls[i].bounce();
                balls[i].collide();
                balls[i].move();
                balls[i].above();
                balls[i].display();
            }
        }
    }

    /*
     * Sets raduius and are of word bubbles, based on the amount of times the
     * word has occured; the more times the word has been mentioned the bigger
     * the word bubble will be TODO: this still assumes that there are only 2
     * characters per bubble consider adjusting the area such that it takes in
     * to account longer words
     */
    void updateBubbles() {


        amountOfWords = 0;
        bubblesPlotted = 0;
        for (int i = maximumBubbles; i >= 0; i--) {
            if (i < balls.length) {
                amountOfWords += balls[i].occurences;
                bubblesPlotted++;
            }
        }

        calculateBubbleSize();
        for (int i = 0; i < balls.length; i++) {
            float bestArea = (totalArea * balls[i].occurences) / amountOfWords;
            balls[i].area = bestArea;
            balls[i].radius = sqrt(((bestArea) / PI));
        }
    }

    /*
     * Checks to see if any new tweets have come in from the twitter streamer if
     * so tokenises tweet and if word is not a stopword either adds word to
     * wordBubble array or increments existing bubble array value Finally the
     * wordBubble array is re-ordered such that the largest bubble is at the
     * front
     */
    void getTweets() {
        String tweetWord = "";
        //BEGIN COMMENTING OUT 
        if (lastCount < loc.statusCount()) { //if more tweets have been stored in the twitter streamer
            ArrayList<String> alreadySeen = new ArrayList<String>(); //count each word from tweet only once (avoids manipulation from tweets which repear the same word)
            //SHAKE WHEN NEW TWEET COMES IN
            shakeRandom(1);

            ArrayList<String> tw = new ArrayList<String>();
            tw = toWordsTwitter(loc.getStatus(lastCount)); //tokenize latest tweet

            //for each word in the tweet, if it's a stopword remove, otherwise keep anything that is a word or an emoticon
            boolean isStopword = false;
            for (String w : tw) {
                w = w.toLowerCase();
                for (String s : stop_words) {
                    if (s.equals(w.toLowerCase())) {
                        isStopword = true;
                        break;
                    }
                }
                if (!isStopword && !alreadySeen.contains(w)) {
                    alreadySeen.add(w);
                    tweetWord = w;
                    addWord(w);
                    isStopword = false;
                }
                isStopword = false;
            }

            lastCount = loc.statusCount();
        }
        noWords++;
        // refresh info order 
        orderArray();
    }

    /**
     *
     * Sets text on screen including amount of words collected my name and the
     * last tweet collected
     *
     * @param font1 Font used for text, defined in setup
     * @param font2 Font used for text, defined in setup
     * @param font3 Font used for text, defined in setup
     */
    void layout(PFont font1, PFont font2, PFont font3) {


        // data - bubbles plotted / bubbles available
        textFont(font1, 25);
        textAlign(LEFT);
        fill(120);
        text(str(bubblesPlotted) + " / " + str(balls.length), 30, height - 30);

        textFont(font2, 18);
        textAlign(RIGHT);
        String infoText = "";
        if (loc.statuses.size() != 0) {
            if (loc.statuses.size() == 1) {
                infoText = loc.getStatus(0);
            } else {
                infoText = loc.getStatus(lastCount - 1);
                if (infoText.length() > 80) {
                    infoText = infoText.substring(0, 80) + "...";
                }
            }
        }
        text(infoText + " ", width - 30, height - 31); //NOTE changed to last tweet
        textFont(font3, 10);

        text("[www.zackbleach.com]" + " ", width - 30, 35);
    }

    /*
     * Checks to see if word already exists in ball array, if true incrementes
     * the wordBubbles occurences by one, otherwise creates a new wordBubble in
     * the array for that word
     */
    void addWord(String newWord) {

        //checks to see if word already exists in ball array, if it does increments it's occurences by one
        //else, creates a new ball in the array containing that word 
        int wordFound = 0;
        for (int i = 0; i < balls.length; i++) {
            if (balls[i].name.equals(newWord) == true) { // if you find the most one occurrence kp
                wordFound = 1;
                balls[i].occurences++;
            }
            if (wordFound == 1) {
                break;
            }
        }

        if (wordFound == 0) {
            newWord(newWord);
        }

    }

    /*
     * Creates a new ball by copying ball array, adding new ball to end of array
     * and replacing initial array with it
     */
    void newWord(String newx) {

        calculateBubbleSize();
        float myArea;
        if (balls.length > 0) {
            myArea = totalArea / balls.length;
        } else {
            myArea = totalArea;
        }
        Ball[] tempBall = append(balls, myArea, newx, 1);
        balls = tempBall;

    }

    /*
     * Simple sort algorithm to put bubbles with the most occurences in at the
     * front (first position) of the array
     */
    void orderArray() {

        Ball[] tempOccurences = new Ball[balls.length];
        System.arraycopy(balls, 0, tempOccurences, 0, balls.length);

        Ball temp;
        int i, j;
        for (i = tempOccurences.length - 1; i >= 0; i--) {
            for (j = 0; j < i; j++) {
                if (tempOccurences[j].occurences < tempOccurences[j + 1].occurences) {
                    temp = tempOccurences[j];
                    tempOccurences[j] = tempOccurences[j + 1];
                    tempOccurences[j + 1] = temp;
                }
            }
        }
        balls = tempOccurences;
    }

    /*
     * Changes size of bubbles depending on how many have been plotted
     */
    void calculateBubbleSize() {

        float myHeight = height - spaceUp - spaceDown;
        float myWidth = width - spaceLeft - spaceRight;

        if (bubblesPlotted <= 1) {
            if (myHeight < myWidth) {
                totalArea = PI * pow(myHeight / 2, 2) * 0.8f;
            } else {
                totalArea = PI * pow(myWidth / 2, 2) * 0.8f;
            }
        } else if (bubblesPlotted > 1 && bubblesPlotted <= 6) {
            totalArea = myWidth * myHeight * 0.65f;
        } else if (bubblesPlotted > 6 && bubblesPlotted <= 20) {
            totalArea = myWidth * myHeight * 0.75f;
        } else if (bubblesPlotted > 20 && bubblesPlotted <= 50) {
            totalArea = myWidth * myHeight * 0.80f;
        } else if (bubblesPlotted > 50 && bubblesPlotted <= 200) {
            totalArea = myWidth * myHeight * 0.86f;
        } else if (bubblesPlotted > 200) {
            totalArea = myWidth * myHeight * 0.92f;
        }

    }

    /*
     * Looks to see if an ascii character key has been pressed and performs
     * appropriate action
     *
     * '+' increases amount of wordBubbles on screen '-' reduces amount of
     * wordBubbles on screen 'i' displays the amount of times the word has been
     * mentioned 't' displays the the context that the word was last used in 'f'
     * switches to full screen mode 'm' activates the motion sensor and moves
     * the wordBubbles depending on the position of device 'r' randomly
     * rearranges the wordBubbles on screen
     */
    @Override
    public void keyPressed() {


        if (keyCode < 256) {    //Ignore keys that are outside of ascii range
            keyboard.press(keyCode);
        }
        if (key == '+') { // Increase amount of words displayed on screen 
            if (maximumBubbles == 0) {
                maximumBubbles = 4;
            } else if (maximumBubbles == 4) {
                maximumBubbles = 19;
            } else if (maximumBubbles == 19) {
                maximumBubbles = 49;
            } else if (maximumBubbles == 49) {
                maximumBubbles = 99;
            } else if (maximumBubbles == 99) {
                maximumBubbles = 199;
            } else if (maximumBubbles == 199) {
                maximumBubbles = 399;
            } else if (maximumBubbles == 399) {
                maximumBubbles = 499;
            }
        }
        if (key == '-') { // reduce amount of words displayed on screen 
            if (maximumBubbles == 499) {
                maximumBubbles = 399;
            } else if (maximumBubbles == 399) {
                maximumBubbles = 199;
            } else if (maximumBubbles == 199) {
                maximumBubbles = 99;
            } else if (maximumBubbles == 99) {
                maximumBubbles = 49;
            } else if (maximumBubbles == 49) {
                maximumBubbles = 19;
            } else if (maximumBubbles == 19) {
                maximumBubbles = 4;
            } else if (maximumBubbles == 4) {
                maximumBubbles = 0;
            }
        }
        if (key == 'i' || key == 'I') { // display amount of times word has been mentioned
            if (show_info == true) {
                show_info = false;
            } else if (show_info == false) {
                if (last_tweet) {
                    last_tweet = false;
                }
                show_info = true;
            }
        }
        if (key == 't' || key == 'T') { // display context word was last used in
            if (last_tweet == true) {
                last_tweet = false;
            } else if (last_tweet == false) {
                if (show_info) {
                    show_info = false;
                }
                last_tweet = true;
            }
        }
        if (key == 'f' || key == 'F') { // switch full screen
            if (!full_screen) {
                size(scrnsize.width, scrnsize.height - 20);
                try {
                    fs.enter();
                    full_screen = true;
                } catch (Exception e) {
                    //do nothing
                }
            } else {
                try {
                    fs.leave();
                    size(1000, 600);
                    full_screen = false;
                } catch (Exception e) {
                    //do nothing 
                    //Full screen mode often throws exception, 
                    //when set to native screen resolution full screen module
                    //crashes, when set to non native resolution
                    //fs mode throws exception
                    //but performs better
                }
            }
        }
        if (key == 'm' || key == 'M') {  //activate motion sensor balls move depending on position of device
            if (!motion_sensor) {
                startSMSx = sms.Unimotion.getSMSX();
                startSMSy = sms.Unimotion.getSMSY();
                motion_sensor = true;
            } else {
                motion_sensor = false;
            }
        }


        if (key == 'r' || key == 'R') { // randomly re-arrange balls
            for (int i = 0; i < balls.length; i++) {
                balls[i].x = random(balls[i].radius + spaceLeft, width - spaceRight - balls[i].radius);
                balls[i].y = random(balls[i].radius + spaceUp, height - spaceDown - balls[i].radius);
            }
        }
    }

    @Override
    public void keyReleased() {

        if (keyCode < 256) {
            keyboard.release(keyCode);
        }
    }

    @Override
    public void mouseReleased() {

        dragging = -1;
    }

    /*
     * Class used to represent word bubbles contains content on the bubble and
     * information about it's position. Class also controls bubbles interactions
     * with other bubbles (for example collision) and it's motion.
     */
    class Ball {

        float radius;
        float m;
        float x;
        float y;
        float velocityX;
        float velocityY;
        int id; // do I need this?
        float area;
        String name;
        int occurences;
        // Spring
        float mass;                                       // Masa
        float kspring;                                    // Constante de resorte
        float damp;                                       // Damping 
        float rest_posx = ((width - spaceRight) / 2) + spaceLeft / 2;
        float rest_posy = ((height - spaceDown) / 2) + spaceRight / 2;
        float accel = 0;                                  // Aceleracion 
        float force = 0;                                  // Fuerza
        boolean isAbove;

        /**
         * Constructor for ball object
         *
         * @param myID identifier for wordBubble, used to work out which one is
         * being dragged
         * @param myArea area of bubble
         * @param myName name of bubble, is also the text that is displayed
         * @param myOccurences amount of times word has been used
         */
        Ball(int myID, float myArea, String myName, int myOccurences) {

            area = myArea;
            radius = sqrt(area / PI);
            m = radius;
            x = random(radius + spaceLeft, width - spaceRight - radius);
            y = random(radius + spaceUp, height - spaceDown - radius);
            velocityX = random(-3, 3);
            velocityY = random(-3, 3);
            id = myID;
            name = myName;
            occurences = myOccurences;
            isAbove = false;

            mass = sqrt((((PI * pow((height - spaceDown - spaceUp) / 2, 2) * 0.8f) / 2000) / PI));
            damp = 0.85f;
            kspring = 0.01f;
        }

        /*
         * If motin sensor is key has been pressed, adjust x, y position
         * according to readings (threshold is set at 10)
         */
        void fall() {

            if (motion_sensor) {
                if (Math.abs(currentSMSy - startSMSy) > 10 || Math.abs(currentSMSx - startSMSx) > 10) {
                    velocityY += (currentSMSy - startSMSy) * -1;
                    velocityX += ((currentSMSx - startSMSy)) / 2;
                }
            }

        }

        /*
         * Method to work out the amount of bounse of ball for when it collides
         * with another ball
         */
        void bounce() {

            if (y + velocityY + radius > height - spaceDown) {

                y = height - spaceDown - radius;
                velocityX *= friction;
                velocityY *= -bounce;
            }
            if (y + velocityY - radius < spaceUp) {

                y = radius + spaceUp;
                velocityX *= friction;
                velocityY *= -bounce;
            }
            if (x + velocityX + radius > width - spaceRight) {

                x = width - spaceRight - radius;
                velocityX *= -bounce;
                velocityY *= friction;
            }
            if (x + velocityX - radius < spaceLeft) {

                x = radius + spaceLeft;
                velocityX *= -bounce;
                velocityY *= friction;
            }
        }

        /*
         * Checks if wordBubble has collided with another if so adjust balls
         * position accordingly
         */
        void collide() {

            for (int i = maximumBubbles; i >= 0; i--) {

                if (i < balls.length) {

                    float X1 = balls[i].x;
                    float Y1 = balls[i].y;
                    float R1 = balls[i].radius;
                    float M = balls[i].m;

                    float deltax = X1 - x;
                    float deltay = Y1 - y;
                    float d = sqrt(pow(deltax, 2) + pow(deltay, 2));

                    if (d < radius + R1 && d > 0) {

                        float dD = radius + R1 - d;
                        float theta = atan2(deltay, deltax);

                        velocityX += -dD * cos(theta) * M / (m + M);
                        velocityY += -dD * sin(theta) * M / (m + M);

                        velocityX *= bounce;
                        velocityY *= bounce;

                    }
                }
            }
        }

        /**
         * if cursor is over a wordBubble and mouse button is pressed change
         * position of ball so it follows the cursor
         */
        void move() {

            if (isAbove && mousePressed && (dragging == -1 || dragging == id)) {
                x = mouseX;
                y = mouseY;
                velocityX = 0;
                velocityY = 0;
                dragging = id;
            } else {
                x += velocityX;
                y += velocityY;
            }
        }

        /*
         * checks to see if cursor is currently above wordBubble
         */
        void above() {

            if (dist(x, y, mouseX, mouseY) < radius) {
                isAbove = true;
            } else {
                isAbove = false;
            }


        }

        /*
         * renders wordBubble to display, prints text and if info or last_tweet
         * is active display those two
         */
        void display() {

            int myWordColor = wordColor;

            float A = balls[0].occurences;                        // maximo original
            float C = occurences;                                 // valor original
            float B = balls[bubblesPlotted - 1].occurences;    // minimo original
            float D;                                               // nuevo maximo
            float E;                                               // nuevo minimo

            noStroke();
            float lcalpha = -1 * (((A - C) / (A - B)) * (255 - 90) - 255);

            //if most amount of occurences is the same as this amount of occurences, set to max brightness
            if (A == B) {
                lcalpha = 255;
            }

            //light up word when mouse is over it (TODO: possibly include highlight mode)
            if (isAbove) {
                lcalpha = 255;
            }


            noFill();
            ellipse(x, y, (2 * radius - radius / 10), (2 * radius - radius / 10));

            float size = radius * 0.8f;
            textFont(font, size);
            textAlign(CENTER);

            fill(myWordColor, lcalpha);

            if (show_info) {
                text(name, x, y + size / 5);
            } else {
                text(name, x, y + size / 3);
            }

            //if ( show_info || estamos_encima ) {
            if (show_info) {
                float tamanio1 = radius * 0.3f;
                textFont(font, tamanio1);
                fill(0, 102, 153, lcalpha);
                text(str(occurences), x, y + size / 3 + tamanio1);


            }
            if (last_tweet) {

                String display = "";
                String newDisplay = "";


                int index = 0;
                int start;
                int end;
                float tamanio1 = radius * 0.3f;
                textFont(font, tamanio1);//tamanio1);
                fill(0, 102, 153, lcalpha);
                display = loc.lastStatus(name);

                ArrayList<String> words = toWordsTwitter(display);

                for (int i = 0; i < words.size(); i++) {
                    if (words.get(i).equals(name)) {
                        index = i;
                        break;
                    }
                }

                if (index - 2 < 0) {
                    start = 0;
                } else {
                    start = index - 2;
                }

                if (index + 3 > words.size()) {
                    end = words.size();
                } else {
                    end = index + 3;
                }

                for (int i = start; i < end; i++) {
                    newDisplay += words.get(i) + " ";
                }

                text("..." + newDisplay + "...", x, y + size / 3 + tamanio1 + 10);


            }


        }
    }
    /*
     * copies previous array and adds new word bubble returns array with new
     * bubble added
     */

    Ball[] append(Ball t[], float ka, String NOMBRE, int OCURR) {
        Ball temp[] = new Ball[t.length + 1];
        System.arraycopy(t, 0, temp, 0, t.length);
        temp[t.length] = new Ball(t.length, ka, NOMBRE, OCURR);
        return temp;
    }
    Keys keyboard = new Keys();

    class Keys {

        boolean[] k;

        Keys() {
            k = new boolean[255];
            for (int i = 0; i < k.length; i++) {
                k[i] = false;
            }
        }

        void press(int x) {
            k[x] = true;
        }

        void release(int x) {
            k[x] = false;
        }

        boolean pressed(int x) {
            return k[x];
        }

        void releaseAll() {
            for (int i = 0; i < k.length; i++) {
                k[i] = false;
            }
        }

        boolean anyPressed() {
            for (int i = 0; i < k.length; i++) {
                if (k[i] == true) {
                    return true;
                }
            }
            return false;
        }
    }
}
