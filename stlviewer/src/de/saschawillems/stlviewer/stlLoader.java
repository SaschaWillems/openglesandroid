/*
 This code is licensed under the Mozilla Public License Version 2.0 (http://opensource.org/licenses/MPL-2.0)
 © 2014 by Sascha Willems - http://www.saschawillems.de
 
 Simple ASCII stl loader and renderer for OpenGL ES 2.0
*/

package de.saschawillems.stlviewer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.opengl.GLES20;
import android.util.Log;

class Face {
	public String mIndex[];
	
	Face(String i1, String i2, String i3) {
		mIndex = new String[3];
		mIndex[0] = i1;
		mIndex[1] = i2;
		mIndex[2] = i3;
	}
		
}

public class stlLoader {

	public ArrayList<Vector3f> mVertices;
	public ArrayList<Vector3f> mNormals;
	
	public FloatBuffer mVertexBuffer;
	public FloatBuffer mNormalBuffer;		
	
	public Vector3f mSize;

	// Bounding box
	public float mRight = -4096;
	public float mLeft = 4096;
	public float mTop = -4096;
	public float mBottom = 4096;
	public float mFront = -4096;
	public float mBack = 4096;
	
	boolean loadFromStream(InputStream is) throws Exception {

    	Log.i("stlLoader", "Loading .stl...");		
		
		mVertices = new ArrayList<Vector3f>();
		mNormals = new ArrayList<Vector3f>();
		
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
		               
        while ((line = reader.readLine()) != null) {
                       	
        	String tokens[] = line.trim().split("\\s+");
        	       	
        	// New face
        	if (tokens[0].equals("facet")) {
            	// Normal 
        		Vector3f faceNormal = new Vector3f(Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]), Float.parseFloat(tokens[4]));
        		// Get vertices from ("outer loop"..."endloop")
        		String tmpLine;
        		int numVertices = 0;
        		while ((tmpLine = reader.readLine()) != null) {
                	String tmpTokens[] = tmpLine.trim().split("\\s+");     			
        			if (tmpTokens[0].equals("endloop")) {
        				break;
        			}
        			if (tmpTokens[0].equals("vertex")) {
	    				mVertices.add(new Vector3f(Float.parseFloat(tmpTokens[1]), Float.parseFloat(tmpTokens[2]), Float.parseFloat(tmpTokens[3])));   					    				
	    				numVertices++;
        			}
        		}
        		// Duplicate normals
        		for (int i = 0; i < numVertices; i++) {
        			mNormals.add(faceNormal);
        		}
        	}
        	      			
        }
        
        reader.close();		

        Log.i("stlLoader", "model loaded, facecount = " + String.valueOf(mVertices.size() / 3));
		
		return true;
	}
	
	void generateVertexArrays(float scale) {

		float[] mVA = new float[mVertices.size()*3]; 
		float[] mVNA = new float[mVertices.size()*3];
			
		for (int i = 0; i < mVertices.size(); i++) {
			
			mVA[i*3]   = mVertices.get(i).x * scale;
			mVA[i*3+1] = mVertices.get(i).y * scale;
			mVA[i*3+2] = mVertices.get(i).z * scale;

			//if (mVA[i*3+2] < 0.0f) { mVA[i*3+2] = 0.0f; };
	
			
			// Extremes				
			if (mVA[i*3] < mLeft)
				mLeft = mVA[i*3]; 
			if (mVA[i*3] > mRight)
				mRight = mVA[i*3]; 

			if (mVA[i*3+1] < mBottom)
				mBottom = mVA[i*3+1]; 
			if (mVA[i*3+1] > mTop)
				mTop = mVA[i*3+1]; 

			if (mVA[i*3+2] < mBack)
				mBack = mVA[i*3+2]; 
			if (mVA[i*3+2] > mFront)
				mFront = mVA[i*3+2]; 				
					
			mVNA[i*3]   = mNormals.get(i).x;
			mVNA[i*3+1] = mNormals.get(i).y;
			mVNA[i*3+2] = mNormals.get(i).z;				
			
		}
		

		// Center object
		
		mSize = new Vector3f(mRight - mLeft, mTop - mBottom, mFront - mBack);
	
		for (int i = 0; i < mVertices.size(); i++) {
			mVA[i*3]   -= (mRight + mLeft) / 2.0f; 
			mVA[i*3+1] -= (mTop + mBottom) / 2.0f; 
			mVA[i*3+2] -= (mFront + mBack) / 2.0f; 
		}
			
			mVertexBuffer = ByteBuffer.allocateDirect(mVA.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			mVertexBuffer.put(mVA).position(0);
			
			mNormalBuffer = ByteBuffer.allocateDirect(mVNA.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			mNormalBuffer.put(mVNA).position(0);
			
				
	}
	
	void render(int positionHandle, int normalHandle) {
		GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
		GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, mNormalBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);			
        GLES20.glEnableVertexAttribArray(normalHandle);			
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mVertexBuffer.capacity() / 3);			
	}
		

}
