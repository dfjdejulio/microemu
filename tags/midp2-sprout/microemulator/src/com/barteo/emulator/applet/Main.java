/*
 *  MicroEmulator
 *  Copyright (C) 2001 Bartek Teodorczyk <barteo@it.pl>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package com.barteo.emulator.applet;

import java.applet.Applet;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Enumeration;

import javax.microedition.lcdui.Command;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import com.barteo.emulator.CommandManager;
import com.barteo.emulator.DisplayComponent;
import com.barteo.emulator.EmulatorContext;
import com.barteo.emulator.MIDletBridge;
import com.barteo.emulator.MicroEmulator;
import com.barteo.emulator.device.Device;
import com.barteo.emulator.device.DeviceFactory;
import com.barteo.emulator.device.SoftButton;
import com.barteo.emulator.device.applet.AppletButton;
import com.barteo.emulator.device.applet.AppletDevice;
import com.barteo.emulator.device.applet.AppletDeviceDisplay;
import com.barteo.emulator.device.applet.AppletInputMethod;


public class Main extends Applet implements MicroEmulator, DisplayComponent
{
  Main instance;
  MIDlet midlet;

  Font defaultFont;
  
  AppletButton prevOverButton;
  AppletButton overButton;
  AppletButton pressedButton;

	Image offi;

  MouseAdapter mouseListener = new MouseAdapter() 
  {
    
    public void mousePressed(MouseEvent e) 
    {
      pressedButton = getButton(e.getX(), e.getY());
      if (pressedButton != null) {
        if (pressedButton instanceof SoftButton) {
          Command cmd = ((SoftButton) pressedButton).getCommand();
          if (cmd != null) {
            CommandManager.getInstance().commandAction(cmd);
          }
        } else {
          int key = pressedButton.getKey();
          KeyEvent ev = new KeyEvent(instance, 0, 0, 0, key, KeyEvent.CHAR_UNDEFINED);
          DeviceFactory.getDevice().getInputMethod().keyPressed(ev.getKeyCode());
        }
        repaint();
      }
    }


    public void mouseReleased(MouseEvent e) 
    {
      AppletButton prevOverButton = getButton(e.getX(), e.getY());
      if (prevOverButton != null) {
        int key = prevOverButton.getKey();
        KeyEvent ev = new KeyEvent(instance, 0, 0, 0, key, KeyEvent.CHAR_UNDEFINED);

        DeviceFactory.getDevice().getInputMethod().keyReleased(ev.getKeyCode());
      }
      pressedButton = null;
      repaint();      
    }

  };
  
  MouseMotionListener mouseMotionListener = new MouseMotionListener() 
  {

    public void mouseDragged(MouseEvent e)
    {
      overButton = getButton(e.getX(), e.getY());
    }

    
    public void mouseMoved(MouseEvent e)
    {
      prevOverButton = overButton;
      overButton = getButton(e.getX(), e.getY());
      if (overButton != prevOverButton) {
        repaint();
      }
    }
    
  };

  KeyListener keyListener = new KeyListener()
  {
    
    public void keyTyped(KeyEvent ev)
    {
    }

    
    public void keyPressed(KeyEvent ev)
    {
      ((AppletInputMethod) DeviceFactory.getDevice().getInputMethod()).keyboardKeyPressed(ev);
      pressedButton = getButton(ev);
      repaint();
      if (pressedButton instanceof SoftButton) {
        Command cmd = ((SoftButton) pressedButton).getCommand();
        if (cmd != null) {
          CommandManager.getInstance().commandAction(cmd);
        }
      }      
    }

    
    public void keyReleased(KeyEvent ev)
    {
      ((AppletInputMethod) DeviceFactory.getDevice().getInputMethod()).keyboardKeyReleased(ev);
      prevOverButton = pressedButton;
      pressedButton = null;
      repaint();      
    }
    
  };

    
  EmulatorContext emulatorContext = new EmulatorContext()
  {    
    public ClassLoader getClassLoader()
    {
      return getClass().getClassLoader();
    }
    
    public DisplayComponent getDisplayComponent()
    {
      return instance;
    }    
  };

  
  public void init()
  {
    instance = this;

    MIDletBridge.setMicroEmulator(this);
      
    addKeyListener(keyListener);

    addMouseListener(mouseListener);
    addMouseMotionListener(mouseMotionListener);
    
    AppletDevice device;
    String deviceClassName = getParameter("device");
    if (deviceClassName == null) {
      device = new AppletDevice();    
    } else {
      try {
        device = (AppletDevice) Class.forName(deviceClassName).newInstance();
      } catch (ClassNotFoundException ex) {
        System.out.println(ex);
        return;
      } catch (IllegalAccessException ex) {
        System.out.println(ex);
        return;
      } catch (InstantiationException ex) {
        System.out.println(ex);
        return;
      }
    }

    DeviceFactory.setDevice(device);
    device.init(emulatorContext);

    String midletName = getParameter("midlet");
		if (midletName == null) {
			System.out.println("There is no midlet parameter");
			return;
		}
    
    Class midletClass;
		try {
			midletClass = Class.forName(midletName);
		} catch (ClassNotFoundException ex) {
			System.out.println("Cannot find " + midletName + " MIDlet class");
			return;
		}

    try {
      midlet = (MIDlet) midletClass.newInstance();
    } catch (Exception ex) {
      System.out.println("Cannot initialize " + midletClass + " MIDlet class");
      System.out.println(ex);        
      ex.printStackTrace();
      return;
    }

    Image tmpImg = ((AppletDevice) DeviceFactory.getDevice()).getNormalImage();
    resize(tmpImg.getWidth(null), tmpImg.getHeight(null));
    
    return;
  }


  public void start()
	{
    try {
      MIDletBridge.getMIDletAccess(midlet).startApp();
		} catch (MIDletStateChangeException ex) {
      System.err.println(ex);
		}
    requestFocus();
  }


	public void stop() 
  {
    MIDletBridge.getMIDletAccess(midlet).pauseApp();
  }


	public void destroy()
	{
    try {
			MIDletBridge.getMIDletAccess(midlet).destroyApp(true);
		} catch (MIDletStateChangeException ex) {
  		System.err.println(ex);
		}
	}


  public String getAppProperty(String key)
  {
    return null;
  }

  
  public void notifyDestroyed()
	{
  }
  
  
  public void notifySoftkeyLabelsChanged()
  {
  }
  
  
  public String getAppletInfo()
	{
    return "Title: MicroEmulator \nAuthor: Bartek Teodorczyk, 2001";
  }


  public String[][] getParameterInfo()
	{
    String[][] info = {
		    {"midlet", "MIDlet class name", "The MIDlet class name. This field is mandatory."},
    };

		return info;
  }

                       
  public void paint(Graphics g) 
  {
    Device device = DeviceFactory.getDevice();
    Rectangle displayRectangle = 
        ((AppletDeviceDisplay) device.getDeviceDisplay()).getDisplayRectangle();

    g.drawImage(((AppletDevice) DeviceFactory.getDevice()).getNormalImage(), 0, 0, this);
    
    g.translate(displayRectangle.x, displayRectangle.y);
    
    ((AppletDeviceDisplay) device.getDeviceDisplay()).paint(g);
    
    g.translate(-displayRectangle.x, -displayRectangle.y);

    Rectangle rect;
    if (prevOverButton != null ) {
      rect = prevOverButton.getRectangle();    
      g.drawImage(((AppletDevice) DeviceFactory.getDevice()).getNormalImage(), 
          rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
          rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, null);
      prevOverButton = null;
    }
    if (overButton != null) {
      rect = overButton.getRectangle();    
      g.drawImage(((AppletDevice) DeviceFactory.getDevice()).getOverImage(), 
          rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
          rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, null);
    }
    if (pressedButton != null) {
      rect = pressedButton.getRectangle();    
      g.drawImage(((AppletDevice) DeviceFactory.getDevice()).getPressedImage(), 
          rect.x, rect.y, rect.x + rect.width, rect.y + rect.height,
          rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, null);
    }    
  }


	public void update(Graphics g)
	{
    if (offi == null || 
        offi.getWidth(null) != getSize().width || offi.getHeight(null) != getSize().height) {
			offi = createImage(getSize().width, getSize().height);
    }

		Graphics offg = offi.getGraphics();
		paint(offg);

    g.drawImage(offi, 0, 0, null);
  }

  
  AppletButton getButton(int x, int y)
  {
    for (Enumeration e = ((AppletDevice) DeviceFactory.getDevice()).getButtons().elements(); e.hasMoreElements(); ) {
      AppletButton button = (AppletButton) e.nextElement();
      Rectangle tmp = new Rectangle(button.getRectangle());
      if (x >= tmp.x && x < tmp.x + tmp.width && y >= tmp.y && y < tmp.y + tmp.height) {
        return button;
      }
    }        
    return null;
  }  

  
  private AppletButton getButton(KeyEvent ev)
  {
    for (Enumeration e = ((AppletDevice) DeviceFactory.getDevice()).getButtons().elements(); e.hasMoreElements(); ) {
      AppletButton button = (AppletButton) e.nextElement();
      if (ev.getKeyCode() == button.getKey()) {
        return button;
      }
      if (button.isChar(ev.getKeyChar())) {
        return button;
      }
    }        
    return null;
  }
  
}