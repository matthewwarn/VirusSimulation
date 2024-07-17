package VirusSimulation;

import java.awt.image.BufferedImage;

public class RepairShop {
    BufferedImage image;
    
    public RepairShop(){
        image = Panel.loadImage("./res/shop icon.png");
    }
    
    public BufferedImage getImage() {
        return image;
    }

}
