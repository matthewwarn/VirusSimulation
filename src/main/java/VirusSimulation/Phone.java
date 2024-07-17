package VirusSimulation;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;

/*Question: Which objects have you chosen to synchronize? Why?

Answer: I have chosen to synchronize a few methods in the code. I chose to sync any method that accesses my 
phones, images or phoneThreads lists, such as methods related to infecting, removing, or repairing phones. This 
is because the methods access valuable data and if there were multiple threads trying to modify those lists at 
the same time, it could cause a race condition and break the code. I have also synchronized the setRepairing() 
method as there is only supposed to be one phone repairing at a time. If multiple threads tried to repair at 
the same time it could cause an error. */

public class Phone implements Runnable {
    int x;
    int y;
    int vx;
    int vy;
    int shopX;
    int shopY;
    int delay = 10;
    int width;
    int height;
    int deathTimer = 500;
    int shopTimer;
    boolean repairing = false;
    boolean infected = false;
    boolean inShop = false;
    boolean vxFlipped = false;
    boolean vyFlipped = false;
    private PhoneListener listener;
    RepairShop repairShop;

    public Phone(int x, int y, PhoneListener listener) {
        this.x = x;
        this.y = y;
        vx = 2;
        vy = 2;
        this.listener = listener;
        repairing = false;
        shopX = 55;
        shopY = 55;
    }

    public void setRange(int width, int height) {
    	//Sets phones range to the inputted values
    	this.width = width;
        this.height = height;
    }

    //Main thread that all of the phones run
    public void run() {
    	//Picking a random direction for the phone to start moving in
        int xDirection = Panel.randInt(1,2);
        int yDirection = Panel.randInt(1,2);
            
        while (true) {
        	//If in the shop, try to exit and decrement the shop timer
        	if(inShop) {
        		listener.tryExitShop(this);
        		shopTimer--;
        	}	
        	//If infected, count down the death timer and remove if timer reaches zero
        	else if(infected){
                deathTimer--;
                if(deathTimer <= 0){
                    listener.removePhone(this);
                    return;
                }
                //If infected and repairing, move towards the shop
                if(repairing){
                	//Sets the velocity back to the original direction before moving to shop
                    if(vxFlipped == true){
                        vx *= -1;
                        vxFlipped = false;
                    }
                    if(vyFlipped == true){
                        vy *= -1;
                        vyFlipped = false;
                    }
                    
                    listener.moveTowardsRepairShop(this);
                    
                }
                //If infected and not repairing, move like normal
                else {
                	move(xDirection, yDirection);
                }
            }
            
        	//Normal phones will run this to move normally 
            else{
                move(xDirection, yDirection);
            }
            
        	//Pause the thread for the delay which is 10ms
            try {
                Thread.sleep(delay);
                
            } catch (InterruptedException ex) {
                Logger.getLogger(Panel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void move(int xDirection, int yDirection) {
    	//Change directions if the phone hits a wall
        if (x > width || x < 0) {
            if(vxFlipped == false){
                vxFlipped = true;
            }
            else if(vxFlipped == true){
                vxFlipped = false;
            }
            
            vx *= -1;
        }
        if (y > height || y < 0) {
            if(vyFlipped == false){
                vyFlipped = true;
            }
            else if(vyFlipped == true){
                vyFlipped = false;
            }
            
            vy *= -1;
        }
        
        //Starts moving in the random direction that was chosen earlier
        if(xDirection == 1){
            x -= vx;
        } else if(xDirection == 2){
            x += vx;
        }
        
        if(yDirection == 1){
            y -= vy;
        } else if(yDirection == 2){
            y += vy;
        }
        
        //Updates phone's position
        listener.phoneMoved();
    }
    
    //Using a listener to run the following methods from the Panel class because they couldn't be called from this class on their own
    public interface PhoneListener {
        void phoneMoved();
        void tryExitShop(Phone phone);
		void moveTowardsRepairShop(Phone phone);
		void removePhone(Phone phone);
        void repairCheckSet();
    }

    //There are various getters, setters, and boolean methods below

    public boolean isInfected() {
        return infected;
    }

    public void setInfected(boolean infected) {
        this.infected = infected;
    }
    
    public boolean isRepairing() {
        return repairing;
    }

    public synchronized void setRepairing(boolean repairing) {
        this.repairing = repairing;
    }
    
    //Returns the string which gets painted on screen next to the infected phones
    public String getDeathTimer(){
        String strTimer = "";
        strTimer += deathTimer;
        return strTimer;
    }
    
    public void setDeathTimer(int value) {
        this.deathTimer = value;
    }

	public void setInShop(boolean inShop) {
		this.inShop = inShop;
	}

	public void setShopTimer(int shopTimer) {
		this.shopTimer = shopTimer;
	}

	public int getShopTimer() {
		return this.shopTimer;
	}

}
