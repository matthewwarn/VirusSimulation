package VirusSimulation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class Panel extends JPanel implements KeyListener, ComponentListener, Phone.PhoneListener {

	//Lists for the phones, images, and threads so they can be added and removed easily
    private ArrayList<BufferedImage> images;
    private ArrayList<Phone> phones;
    private ArrayList<Thread> phoneThreads;
    public JFrame frame;
    public RepairShop repairShop;
    
    public Panel(JFrame frame)
    {
        this.frame = frame;
       
        images = new ArrayList<>();
        phones = new ArrayList<>();
        phoneThreads = new ArrayList<>();
        
        repairShop = new RepairShop();
        
        //Creating first phone
        initializePhone();
        startCollisionDetectionThread();
        
        this.addKeyListener(this);
        this.addComponentListener(this);
        this.setFocusable(true);
    }
    
    private void initializePhone() {
        Phone phone = new Phone(randInt(100, frame.getWidth() - 200), randInt(100, frame.getHeight() - 200), (Phone.PhoneListener) this);
        phone.setRange(frame.getWidth() - 100, frame.getHeight() - 100); //Making the phone stay on screen.
        phones.add(phone); 
        
        BufferedImage image = loadImage("./res/blue phone.png"); 
        images.add(image);
        
        //Creating a new thread for each phone
        Thread phoneThread = new Thread(phone);
        phoneThread.start();
        phoneThreads.add(phoneThread);
        
        repaint();
    }
    
     private synchronized void infectRandomPhone(){
    	//Getting a random phone to infect from the phone list. Synchronized because it accesses the lists and could cause issues if multiple threads tried to access them at once
        int randomIndex = (int) (Math.random() * phones.size());
        
        if(phones.get(randomIndex).isInfected() == false){
            images.set(randomIndex, loadImage("./res/red phone.png"));
            phones.get(randomIndex).setDeathTimer(500);
            phones.get(randomIndex).setInfected(true);
            
            //Checking if any phones are currently being repaired
            repairCheckSet();
        }
        
        repaint();
    }
    
    private synchronized void infectPhone(int index){
        images.set(index, loadImage("./res/red phone.png"));
        phones.get(index).setDeathTimer(500);
        phones.get(index).setInfected(true);
        
        repairCheckSet();
    }
     
    public static BufferedImage loadImage(String filePath){
        try{
            return ImageIO.read(new File(filePath));
        }
        catch(IOException ex) {
            System.out.println("Failed to load image.");
            return null;
        }
    }
    
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        //Creating repair shop image at start of program
        if(repairShop != null && repairShop.getImage() != null){
            g.drawImage(repairShop.getImage(), -5, -20, 225, 225, null);
        }
        
        //Updating the phones' images to match their position
        for (int i = 0; i < phones.size(); i++) {
            Phone phone = phones.get(i);
            BufferedImage image = images.get(i);
            int xPos = phone.x;
            int yPos = phone.y;
            
            if (phone.isRepairing()) {
                g.drawImage(image, xPos, yPos, 110, 110, null);
            } else {
                g.drawImage(image, xPos, yPos, 75, 75, null);
            }
            
            //Painting the timer above all infected phones
            if(phone.isInfected()) {
                g.drawString(phone.getDeathTimer(), xPos, yPos - 10);
            }
        }
        
    }

    @Override
    public void keyPressed(KeyEvent ke) {
    	//Recognizing Up and V inputs to run their respective methods
        if(ke.getKeyCode() == KeyEvent.VK_UP){
            initializePhone();
            repaint();
        }
        
        if(ke.getKeyCode() == KeyEvent.VK_V){
            infectRandomPhone();
            repaint();
        }
    }

    @Override
    public void componentResized(ComponentEvent ce) {
    	//If the frame is resized, set the range of the phone to the new frame size
        for(Phone phone : phones){
            phone.setRange(frame.getWidth() - 150, frame.getHeight() - 150);
        }
        repaint();
    }
    
    public static int randInt(int min, int max) {
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }
    
    public void phoneMoved() {
    	//Updating phone image after moving
        repaint();
    }
   
    public boolean arePhonesTouching(Phone phone1, Phone phone2) {
    	//If distance between the phones is small enough, returns true
        int distanceX = Math.abs(phone1.x - phone2.x);
        int distanceY = Math.abs(phone1.y - phone2.y);
        int minDistance = 50;
        
        return distanceX < minDistance && distanceY < minDistance;
    }
    
    private void startCollisionDetectionThread(){
    	//Thread that checks if phones are touching
        Thread collisionThread = new Thread(() -> {
            while(true){
                checkPhoneCollisions();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Panel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        collisionThread.start();
    }
    
    public synchronized void checkPhoneCollisions(){
    	//Loops through all phones, finds an infected one and checks if it is touching any non-infected phones. Synchronized because it accesses important lists.
        for(int i = 0; i < phones.size(); i++){
            Phone phone1 = phones.get(i);
            if(phone1.isInfected()){
                for(int j = 0; j < phones.size(); j++){
                    if(i != j && !phones.get(j).isInfected()){
                        Phone phone2 = phones.get(j);
                        if (arePhonesTouching(phone1, phone2)){
                            phones.get(j).setInfected(true);
                            infectPhone(j);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public synchronized void removePhone(Phone phone) {
    	//Removes the phone from all lists. Synchronized because if multiple threads tried to access the lists at the same time there could be a race condition.
        int index = phones.indexOf(phone);
        if (index >= 0 && index < phones.size()) {
            phones.remove(index);
            images.remove(index);
            phoneThreads.remove(index);
            repairCheckSet(); //Checking if a phone needs to be repaired
            repaint();
        }
    }
    
    public void moveTowardsRepairShop(Phone phone) {
    	//Makes repairing phones move faster than normal phones
        phone.vx = 4;
        phone.vy = 4;
        
        //Changing image to yellow
        images.set(phones.indexOf(phone), loadImage("./res/yellow phone.png"));
        repaint();
        
        //Calculating distance from shop and moving towards it
        int dx = phone.shopX - phone.x;
        int dy = phone.shopY - phone.y;
        
        int moveX = Integer.compare(dx, 0) * phone.vx;
        int moveY = Integer.compare(dy, 0) * phone.vy;

        phone.x += moveX;
        phone.y += moveY;

        //If phone is close enough to the shop coordinates, runs repair()
        if (Math.abs(phone.x - phone.shopX) <= phone.vx && Math.abs(phone.y - phone.shopY) <= phone.vy) {
            repaint();
            repair(phone);

            //Setting velocity back to normal
            phone.vx = 2;
            phone.vy = 2;
        }
        
        phoneMoved();

    }
    
    public void repair(Phone phone){
    	//Sets shop timer to 200 and sets the inShop flag to true
    	phone.setShopTimer(200);
        phone.setInShop(true);
    }
    
    public void tryExitShop(Phone phone) {
    	//Checking if the phone has spent enough time in the shop
	    if(phone.getShopTimer() <= 0){
	    	//Setting the phones traits back to normal
	    	phone.setInShop(false);
	        images.set(phones.indexOf(phone), loadImage("./res/blue phone.png"));
	        repaint();
	        
	        phone.setRepairing(false);
	        phone.setInfected(false);
	        repairCheckSet(); //Checking if a new phone needs to be repaired
	    }
    }
    
    public synchronized void repairCheckSet() {
        ArrayList<Phone> infectedPhones = new ArrayList<>(); //Temporary list of all infected phones

        for (Phone phone : phones) {
            if (phone.isInfected()) {
                infectedPhones.add(phone);
            }
        }

        //If a phone in the list is already repairing, do nothing
        boolean isAnyPhoneRepairing = false;
        for (Phone phone : infectedPhones) {
            if (phone.isRepairing()) {
                isAnyPhoneRepairing = true;
                break;
            }
        }

        //If no phone is repairing, set the first infected phone to be repaired
        if (!isAnyPhoneRepairing && !infectedPhones.isEmpty()) {
            Phone phoneToRepair = infectedPhones.get(0); 
            phoneToRepair.setRepairing(true);
        }
    }
    
    //The below methods are required for the frame, but I haven't used them

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyReleased(KeyEvent e) {
        
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {
    
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    
    }
}
