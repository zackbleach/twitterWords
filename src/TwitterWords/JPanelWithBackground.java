package TwitterWords;


import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.BoxLayout;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author zackbleach
 */
public class JPanelWithBackground extends JPanel {

    private Image backgroundImage;

    // Some code to initialize the background image.
    // Here, we use the constructor to load the image. This
    // can vary depending on the use case of the panel.
    public JPanelWithBackground(String fileName) throws IOException {

        this.add(Box.createRigidArea(new Dimension(0, 25)));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        backgroundImage = ImageIO.read(new File(fileName));

    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background image.
        g.drawImage(backgroundImage, 0, 0, null);
    }
}
