package main.java.com.demo.entity;

import main.java.com.demo.Commons;
import main.java.com.demo.gfx.Screen;
import main.java.com.demo.level.Level;

/** Represents an enemy as a sprite.
 *  Keeps the image of the sprite and the coordinates of the sprite.
    @author zetcode.com */
public class Enemy extends Sprite {
    
    private int counter;
    private int curDx, initY;
    private boolean activated, crushed, isShot, jumping;
    private int deathTime;  // Counts ticks after death
    
    public Enemy(int x, int y, Level level) {                
        super(level);
        initEnemy(x, y);        
    }    
    
    private void initEnemy(int x, int y) {
        setX(x);
        setY(y); 
        width = height = ES;
        xS = 4;
        yS = 2;
        
        xSpeed = 1; 
        dx = -xSpeed;
        dy = ySpeed;
        ds = 1;
        dir = 2; // Always face left
        
        ground = y + height;
        wS = width / PPS;
        hS = height / PPS;
               
        unit = (int) (Math.log10(width)/Math.log10(2)); // the size of block to be used (4 for 16 px sprite and 3 for 8px sprite)
        aTile = Math.min(Math.pow(2, 4 - unit), 1); // 1 for unit 3, 1 for unit 4, 0.5 for unit 5 (big Pusheen)
        score = 100;
        
        counter = 0;
        curDx = dx;
        
        activated = crushed = isShot = jumping = false;
        deathTime = 0;
    }    
        
    // Positions the enemy in horizontal direction.
    @Override
    public void tick() {
        super.tick();     
                
        if (deathTime == 20) {    // Remove after 20 ticks.
            remove();
        } else if ((crushed) && !removed)   // increase tick when attacked but not removed
            deathTime++;
        else if (isShot) {
            dy = (int) ds;
            if (dy < 4)
                ds += 0.5;     

            x += dx;
            y += dy;
            
            // Update visibility on the screen.
            updateVisibility();     
            
        } else {  // unaffected and moves               
            // When punched from below, die.            
            if (grounded && isPunchedOnBottom) {     
                initY = y;
                jumping = true;
                setShot();                
            }
            
            /* Update y position. */
            if (jumping) {
                dy = (int) ds;
                ds += 0.5;
                if (y + dy >= initY)
                    jumping = false;
            } else if (!grounded)
                dy++;            
            else
                dy = ySpeed; // By default, there is gravity.
            
            /* Update x position. */
            // Moves in x direction every other tick. This is to slow down enemy.
            if (counter % 2 == 0) 
                dx = curDx;
            else  {
                dx = 0;
            }
            counter++;      
    
            // Adjust dy when facing a ground tile.
            if (dy > 0  && y + height < Commons.GROUND && willBeGrounded()) {
                int yt1 = y + dy + ES;
                int backoff = yt1 - (yt1 >> 4) * 16;
                if (backoff > 1)
                    dy -= backoff;
            }            
            
            /* Update y position. */
            boolean stopped = !move(dx, dy);     // Updates x and y.
                        
            if (stopped && dx != 0) {   // Has met a wall
                dx = - dx;
                curDx = dx;                    
            }
            
            // Update visibility on the screen.
            updateVisibility();
        }        
    }  
    
    /** What happens when the player touches an entity.
     * @param sprite The sprite that this sprite is touched by. */    
    @Override
    protected void touchedBy(Sprite sprite) {
        if (sprite instanceof Player && !sprite.isDying()) { // if this enemy touches the player
            Player p = (Player) sprite;            
            boolean isOverTop = p.y + p.height <= y;
            boolean willCrossTop = p.y + p.height + p.dy >= y;
            boolean isXInRange = p.x + p.width > x && x + width > p.x;
            if (isOverTop && willCrossTop && isXInRange && !crushed) { // player jumps onto the enemy
                xS += 2;
                crushed = true;
                dx = 0;
                p.addScore(score); // gives the player 100 points of score
                level.add(new ScoreString(x, y - height, score, level));
                p.setCrushedEnemy(true);
            } else if (!crushed && !isShot) {   // regular encounter
                if (p.isImortal()) { // when player is immortal (ate starman)
                    setShot();
                } else {    
                    sprite.hurt(1); // hurts the player, damage is based on lvl.
//                    p.touchedBy(this);                    
                    if (p.isEnlarged()) {                        
                        p.width /= 2;
                        p.height /= 2;
                        p.wS = p.width / PPS;
                        p.hS = p.height / PPS;
                        p.unit = (int) (Math.log10(p.width)/Math.log10(2)); // the size of block to be used (5 for 32 px, 4 for 16 px sprite, and 3 for 8px sprite)
                        p.aTile = Math.min(Math.pow(2, 4 - p.unit), 1); // 1 for unit 3, 1 for unit 4, 0.5 for unit 5 (big Pusheen)
                        p.yS -= 4;
                        p.setEnlarged(false);
                        level.flower2Mushroom();    // change flowers back to mushrooms.
                        if (p.isFired()) {
                            p.yS -= 8;
                            p.setFired(false);
                        }
                    } 
//                    else {
//                        p.setDying(true);
//                        p.ds = -5;
//                    }           
                }
            }
        }
        
        if (sprite instanceof Enemy) {
            if (!((Enemy) sprite).isCrushed()) {
                if (dx != 0) {
                    dx = -dx;
                    curDx = dx; 
                    sprite.dx = -sprite.dx;
                    ((Enemy) sprite).curDx = sprite.dx;
                }          
            }
        }
    }

    @Override
    public void render(Screen screen) {        
        // Becomes 1 every other square (8 pixels)
        int flip1 = (walkDist >> 2) & 1; // This will either be a 1 or a 0 depending on the walk distance (Used for walking effect by mirroring the sprite)
                       
        int sw = screen.getSheet().width;   // width of sprite sheet (256)
        int colNum = sw / PPS;    // Number of squares in a row (32)    
//        screen.renderFont(x, y, xS + yS * colNum, flip1); // draws the top-left tile
        
        if (isVisible()) {
            if (crushed) {  // crushed into half the height
                screen.render(x, y + PPS, xS + yS * colNum, 0); // renderFont the top-left part of the sprite         
                screen.render(x + PPS, y + PPS, (xS + 1) + yS * colNum, 0);  // renderFont the top-right part of the sprite
            } else if (isShot) {    // Upside down
                flip1 = 0;
                screen.render(x + PPS * flip1, y + PPS, xS + yS * colNum, 2); // renderFont the top-left part of the sprite         
                screen.render(x - PPS * flip1 + PPS, y + PPS, (xS + 1) + yS * colNum, 2);  // renderFont the top-right part of the sprite
                screen.render(x + PPS * flip1, y, xS + (yS + 1) * colNum, 2); // renderFont the bottom-left part of the sprite
                screen.render(x - PPS * flip1 + PPS, y, xS + 1 + (yS + 1) * colNum, 2); // renderFont the bottom-right part of the sprite        
            } else { // Normal state
                screen.render(x + PPS * flip1, y, xS + yS * colNum, flip1); // renderFont the top-left part of the sprite         
                screen.render(x - PPS * flip1 + PPS, y, (xS + 1) + yS * colNum, flip1);  // renderFont the top-right part of the sprite
                screen.render(x + PPS * flip1, y + PPS, xS + (yS + 1) * colNum, flip1); // renderFont the bottom-left part of the sprite
                screen.render(x - PPS * flip1 + PPS, y + PPS, xS + 1 + (yS + 1) * colNum, flip1); // renderFont the bottom-right part of the sprite        
            }
        }               
    }    
    
    public void activate() {
        activated = true;
    }
    
    public boolean isActivated() {
        return activated;
    }
    
    @Override
    public boolean blocks(Sprite e) {
//        if (crushed) return true;
//        else 
            return false;
    }   
    
    public boolean isCrushed() {
        return crushed;
    }
    
    public boolean isShot() {
        return isShot;
    }
    
    public void setShot() {
        isShot = true;
        dx = 0;
        ds = -3;
        level.player.addScore(score);
        level.add(new ScoreString(x, y - height, score, level));
    }
    
    private void updateVisibility() {
        // Update visibility on the screen.
        int offset = level.getOffset();
        if (x <= 0)
            remove();   //hurt(health);     
        else if (x+width <= offset && offset + Commons.BOARD_WIDTH <= x)
            setVisible(false);   
        else
            setVisible(true);   

        if (y > Commons.BOARD_HEIGHT)
            remove();              
    }
}