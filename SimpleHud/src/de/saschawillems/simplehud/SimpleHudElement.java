/* 
 * Copyright © 2014 by Sascha Willems - http://www.saschawillems.de
 * 
 * This code is licensed under the Mozilla Public License Version 2.0 (http://opensource.org/licenses/MPL-2.0) 
*/

package de.saschawillems.simplehud;

import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * Implements a simple element that's drawn on the containing HUD class
 * @author Sascha Willems
 *
 */
public class SimpleHudElement {

	private String mName;
	private String mText;
	private Point mPosition;
	private Paint mTextPaint;
	
	public SimpleHudElement(String name, String text, Point position) {
		mName = name;
		mText = text;
		mPosition = position;
		// Set some default paint parameters, use getTextPaint to change them
		mTextPaint = new Paint();
		mTextPaint.setTextSize(32);
		mTextPaint.setAntiAlias(true);
		mTextPaint.setARGB(0xff, 0xFF, 0xFF, 0xFF);
		mTextPaint.setTextAlign(Paint.Align.LEFT);    				
	}
	
	public String getName() {
		return mName;
	}
	
	public String getText() {
		return mText;
	}
	
	public void setText(String text) {
		mText = text;
	}
	
	public Point getPosition() {
		return mPosition;
	}
	
	public void setPosition(Point position) {
		mPosition = position;
	}
	
	public Paint getTextPaint() {
		return mTextPaint;
	}
	
	/**
	 * @param point Point to check against hud element boundaries
	 * @return true if the point is within the boundaries of this element
	 */
	public boolean pointInElement(Point point) {
		Rect boundsRect = new Rect();
		mTextPaint.getTextBounds(mText, 0, mText.length(), boundsRect);
		
		int offsetX = (mTextPaint.getTextAlign() == Paint.Align.CENTER) ? -boundsRect.width() / 2 : 0; 
		
		return ( (point.x >= mPosition.x + boundsRect.left + offsetX) & (point.x <= mPosition.x + boundsRect.left + boundsRect.width() + offsetX) & (point.y >= mPosition.y - boundsRect.height()) & (point.y <= mPosition.y) );
	}
		
}
