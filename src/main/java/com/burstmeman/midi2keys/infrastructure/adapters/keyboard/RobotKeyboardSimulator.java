package com.burstmeman.midi2keys.infrastructure.adapters.keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.Robot;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java Robot-based implementation of KeyboardSimulator.
 * Uses java.awt.Robot to simulate actual keyboard events on Windows.
 */
public class RobotKeyboardSimulator implements KeyboardSimulator {
    
    private static final Logger logger = LoggerFactory.getLogger(RobotKeyboardSimulator.class);
    
    private final Robot robot;
    private final Set<Integer> pressedKeys;
    private int autoDelay;
    private boolean operational;
    
    public RobotKeyboardSimulator() {
        Robot robotInstance = null;
        boolean isOperational = false;
        
        try {
            robotInstance = new Robot();
            robotInstance.setAutoDelay(0);
            isOperational = true;
            logger.info("RobotKeyboardSimulator initialized successfully");
        } catch (AWTException e) {
            logger.error("Failed to initialize Robot - keyboard simulation will not work", e);
        } catch (SecurityException e) {
            logger.error("Security exception - keyboard simulation not permitted", e);
        }
        
        this.robot = robotInstance;
        this.operational = isOperational;
        this.pressedKeys = ConcurrentHashMap.newKeySet();
        this.autoDelay = 0;
    }
    
    @Override
    public void keyPress(int keyCode) {
        if (!operational || robot == null) {
            logger.warn("Robot not operational, ignoring keyPress: {}", keyCode);
            return;
        }
        
        try {
            robot.keyPress(keyCode);
            pressedKeys.add(keyCode);
            logger.trace("Key pressed: {}", keyCode);
            
            if (autoDelay > 0) {
                robot.delay(autoDelay);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid key code: {}", keyCode);
        }
    }
    
    @Override
    public void keyRelease(int keyCode) {
        if (!operational || robot == null) {
            logger.warn("Robot not operational, ignoring keyRelease: {}", keyCode);
            return;
        }
        
        try {
            robot.keyRelease(keyCode);
            pressedKeys.remove(keyCode);
            logger.trace("Key released: {}", keyCode);
            
            if (autoDelay > 0) {
                robot.delay(autoDelay);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid key code: {}", keyCode);
        }
    }
    
    @Override
    public void typeKey(int keyCode) {
        keyPress(keyCode);
        keyRelease(keyCode);
    }
    
    @Override
    public void pressKeyCombination(Set<Integer> modifiers, int keyCode) {
        // Press all modifiers first
        for (int modifier : modifiers) {
            keyPress(modifier);
        }
        // Press main key
        keyPress(keyCode);
    }
    
    @Override
    public void releaseKeyCombination(Set<Integer> modifiers, int keyCode) {
        // Release main key first
        keyRelease(keyCode);
        // Release all modifiers
        for (int modifier : modifiers) {
            keyRelease(modifier);
        }
    }
    
    @Override
    public void releaseAllKeys() {
        logger.info("Releasing all {} pressed keys", pressedKeys.size());
        
        // Create a copy to avoid concurrent modification
        Set<Integer> keysToRelease = new HashSet<>(pressedKeys);
        
        for (int keyCode : keysToRelease) {
            try {
                if (robot != null) {
                    robot.keyRelease(keyCode);
                }
            } catch (Exception e) {
                logger.warn("Failed to release key {}: {}", keyCode, e.getMessage());
            }
        }
        
        pressedKeys.clear();
        logger.debug("All keys released");
    }
    
    @Override
    public Set<Integer> getPressedKeys() {
        return Collections.unmodifiableSet(new HashSet<>(pressedKeys));
    }
    
    @Override
    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }
    
    @Override
    public void setAutoDelay(int delayMs) {
        this.autoDelay = Math.max(0, delayMs);
        if (robot != null) {
            robot.setAutoDelay(this.autoDelay);
        }
    }
    
    @Override
    public int getAutoDelay() {
        return autoDelay;
    }
    
    @Override
    public boolean isOperational() {
        return operational && robot != null;
    }
}

