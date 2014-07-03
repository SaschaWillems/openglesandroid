/*
 This code is licensed under the Mozilla Public License Version 2.0 (http://opensource.org/licenses/MPL-2.0)
 © 2014 by Sascha Willems - http://www.saschawillems.de
 
 OpenGL ES 2.0 renderer that displays camera image on a rotating cube
*/

package de.saschawillems.cameratexture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

class GLES20Renderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

	private MyGLSurfaceView mView;
	private Camera mCamera;
	private SurfaceTexture mSurfaceTex;
	private boolean mUpdateSurfaceTex;
  
    public GLES20Renderer(MyGLSurfaceView view) {
        
    	mView = view;
    	
        mCubeVertices = ByteBuffer.allocateDirect(cubeVertices.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeVertices.put(cubeVertices).position(0);
        
        mCubeTexCoords = ByteBuffer.allocateDirect(cubeTexCoords.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTexCoords.put(cubeTexCoords).position(0);  
        
        Matrix.setIdentityM(mAccumulatedRotation, 0);
       
   }

    public void onDrawFrame(GL10 glUnused) {

    	// Update camera image if necessary
    	synchronized(this) {
		  if (mUpdateSurfaceTex) {
			mSurfaceTex.updateTexImage();
		    mUpdateSurfaceTex = false;
		  }
		}   	
    	
        GLES20.glClearColor(0.0f, 0.0f, 0.2f, 1.0f);
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);        
        GLES20.glUseProgram(mProgramID);
        checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID); // TODO : Check for equiv in ES20
       
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 0, mCubeVertices);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, 0, mCubeTexCoords);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTextureHandle);
           
        Matrix.setIdentityM(mMMatrix, 0);

    	float[] mCurrentRotation = new float[16];
        Matrix.setIdentityM(mCurrentRotation, 0);        
    	Matrix.rotateM(mCurrentRotation, 0, mAngleX, 0.0f, 1.0f, 0.0f);
    	Matrix.rotateM(mCurrentRotation, 0, mAngleY, -1.0f, 0.0f, 0.0f);
    	mAngleX = 0.0f;
    	mAngleY = 0.0f;
    	Matrix.scaleM(mMMatrix, 0, scaleFactor, scaleFactor, scaleFactor);
    	
    	// Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
    	float[] mTemporaryMatrix = new float[16];
    	Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
    	System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);
    	
        // Rotate the cube taking the overall rotation into account.     	
    	Matrix.multiplyMM(mTemporaryMatrix, 0, mMMatrix, 0, mAccumulatedRotation, 0);
    	System.arraycopy(mTemporaryMatrix, 0, mMMatrix, 0, 16);       	
        
        //Matrix.multiplyMM(mMMatrix, 0, tmpMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
        mAngleX = 0.0f;
        mAngleY = 0.0f;      

		// Pass in the combined matrix.
		GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 26);
        checkGlError("glDrawArrays");     
        
    }

    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
    	
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        
    }

    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

        mProgramID = createProgram(mVertexShader, mFragmentShader);
        if (mProgramID == 0) {
            return;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgramID, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgramID, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramID, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        // Create empty texture to store camera image
        // Texture target is GL_TEXTURE_EXTERNAL_OES, not a simple GL_TEXTURE2D
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                        
        // Create and connect surface texture to OpenGL texture
        mTextureID = textures[0]; 
        mSurfaceTex = new SurfaceTexture(mTextureID);
        mSurfaceTex.setOnFrameAvailableListener(this);
        
        // Open camera
        mCamera = Camera.open();
        try {
          mCamera.setPreviewTexture(mSurfaceTex);
          Camera.Size size = mCamera.getParameters().getPreviewSize();
          Log.i("RENDERER", "Camera preview size: " + size.width + ", " + size.height);
          mCamera.startPreview();
          
        } 
        catch (IOException ioException) {
        	//
        }
        
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        
    }

    private int loadShader(int shaderType, String source) {
    	
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
        
    }

    private int createProgram(String vertexSource, String fragmentSource) {
    	
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
        
    }

    private void checkGlError(String op) {
    	
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
        
    }
    
	@Override
	public synchronized void onFrameAvailable(SurfaceTexture arg0) {
		// This is called each time a new camera image is available, so we request to update the 
	    mUpdateSurfaceTex = true;
	    mView.requestRender();		
	}    
    
    private static final int FLOAT_SIZE_BYTES = 4;

    private final float[] cubeVertices = {
        // Front face
        -1,-1,1, 1,-1,1, -1,1,1, 1,1,1,
        // Right face
        1,1,1, 1,-1,1, 1,1,-1, 1,-1,-1,
        // Back face
        1,-1,-1, -1,-1,-1, 1,1,-1, -1,1,-1,
        // Left face
        -1,1,-1, -1,-1,-1, -1,1,1, -1,-1,1,
        // Bottom face
        -1,-1,1, -1,-1,-1, 1,-1,1, 1,-1,-1,       
        // move to top
        1,-1,-1, -1,1,1,       
        // Top Face
        -1,1,1, 1,1,1, -1,1,-1, 1,1,-1
    };    
    
    private final float[] cubeTexCoords = {
        // Front face
        0,0, 1,0, 0,1, 1,1,
        // Right face
        0,1, 0,0, 1,1, 1,0,
        // Back face
        0,0, 1,0, 0,1, 1,1,
        // Left face
        0,1, 0,0, 1,1, 1,0,
        // Bottom face
        0,1, 0,0, 1,1, 1,0,       
        // Top face
        1,0, 0,0,       
        0,0, 1,0, 0,1, 1,1
    };    
        
    private FloatBuffer mCubeVertices;
    private FloatBuffer mCubeTexCoords;

    private final String mVertexShader =
        "uniform mat4 uMVPMatrix;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTextureCoord;\n" +
        "varying vec2 vTextureCoord;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  vTextureCoord = aTextureCoord;\n" +
        "}\n";

    private final String mFragmentShader =
      "#extension GL_OES_EGL_image_external : require\n" +
      "precision mediump float;\n" +
      "uniform samplerExternalOES sTexture;\n" +                             // Access to the camera texture requires a special sampler
      "varying vec2 vTextureCoord;\n" +
      "void main() {\n" +
      "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
      "}";

    
    private float[] mMVPMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
	private float[] mAccumulatedRotation = new float[16];
	
    private int mProgramID;
    private int mTextureID;
    private int muMVPMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    private static String TAG = "GLES20Renderer";
    
    public volatile float mAngleX;
    public volatile float mAngleY;
    float scaleFactor = 1.0f;
      
}

class MyGLSurfaceView extends GLSurfaceView {

    private final GLES20Renderer mRenderer;

    public MyGLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new GLES20Renderer(this);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        // Create gesture detector for multitouch scale
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());                
    }

  
    private float mPreviousX;
    private float mPreviousY;
    private ScaleGestureDetector scaleGestureDetector;
    
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

    	scaleGestureDetector.onTouchEvent(e);
    	  	
        float x = e.getX();
        float y = e.getY();

		switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;
               
                mRenderer.mAngleX += dx; 
                mRenderer.mAngleY += dy;               
            	
        }
    	
        mPreviousX = x;
        mPreviousY = y;
    	
    	requestRender();   	
    	
        return true;
    }
    
    private class ScaleListener extends
    	ScaleGestureDetector.SimpleOnScaleGestureListener {
    	
    	@Override
    	public boolean onScale(ScaleGestureDetector detector) {
    		
    		mRenderer.scaleFactor *= detector.getScaleFactor();

    		// don't let the object get too small or too large.
    		mRenderer.scaleFactor = Math.max(0.1f, Math.min(mRenderer.scaleFactor, 5.0f));

    		return true;    		
    	}
    }
    
}