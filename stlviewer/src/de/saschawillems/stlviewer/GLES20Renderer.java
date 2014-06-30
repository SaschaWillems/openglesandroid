/*
 This code is licensed under the Mozilla Public License Version 2.0 (http://opensource.org/licenses/MPL-2.0)
 © 2014 by Sascha Willems - http://www.saschawillems.de
 
 Basic OpenGL ES 2.0 renderer with touch rotate and zoom
*/

package de.saschawillems.stlviewer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//import android.R;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import de.saschawillems.stlviewer.R;

class GLES20Renderer implements GLSurfaceView.Renderer {

	stlLoader stlFile;
	
	private stlLoader loadModel(int id) {
		stlLoader stl = new stlLoader();
		try {
			stl.loadFromStream(mContext.getResources().openRawResource(id));
			stl.generateVertexArrays(0.05f);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return stl;
	}
	
    public GLES20Renderer(Context context) {
        mContext = context;       
      
        Matrix.setIdentityM(mAccumulatedRotation, 0);
        stlFile = loadModel(R.raw.purple);
       
   }

    public void onDrawFrame(GL10 glUnused) {
        // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
    	
        String version = glUnused.glGetString(GL10.GL_VERSION);
        Log.w(TAG, "Version: " + version );         	
    	
        GLES20.glClearColor(0.0f, 0.0f, 0.2f, 1.0f);
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glUseProgram(mProgram);
       
        Matrix.setIdentityM(mMMatrix, 0);

        Matrix.setIdentityM(mCurrentRotation, 0);        
    	Matrix.rotateM(mCurrentRotation, 0, mAngleX, 0.0f, 1.0f, 0.0f);
    	Matrix.rotateM(mCurrentRotation, 0, mAngleY, -1.0f, 0.0f, 0.0f);
    	mAngleX = 0.0f;
    	mAngleY = 0.0f;
    	Matrix.scaleM(mMMatrix, 0, scaleFactor, scaleFactor, scaleFactor);
    	
    	float[] mTemporaryMatrix = new float[16];    	
    	
    	// Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
    	Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
    	System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);
    	
        // Rotate the cube taking the overall rotation into account.     	
    	Matrix.multiplyMM(mTemporaryMatrix, 0, mMMatrix, 0, mAccumulatedRotation, 0);
    	System.arraycopy(mTemporaryMatrix, 0, mMMatrix, 0, 16);       	
  	
    	// Modelviewprojection matrix
    	Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
                    
		// Pass in matrices
		GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
       	
        stlFile.render(maPositionHandle, maNormalHandle);    
    }

    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
    	    	
        mProgram = createProgram(mVertexShader, mFragmentShader);
        if (mProgram == 0) {
            return;
        }
        
        // Store shader uniform and attribute handles for better performance
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        maNormalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal"); 
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");      
       
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
 
    private final String mVertexShader =
		"uniform mat4 uMVPMatrix;\n" +  
		"attribute vec4 aPosition;\n" +
		"attribute vec3 aNormal;\n" +
		"varying vec4 vColor;\n" +
		"void main()\n" +
		"{\n" +
	    "   gl_Position = uMVPMatrix * aPosition;\n" + 
		"	vColor = vec4(aNormal, 1.0);\n" +
		"}";          		  		

    private final String mFragmentShader =
		"precision mediump float;\n" +
		"varying vec4 vColor;\n" +
		"void main() {\n" +
		"  gl_FragColor = vColor;\n" +
		"}\n";
    
    private float[] mMVPMatrix = new float[16]; // Combined model/view/projection matrix
    private float[] mProjMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
	private float[] mAccumulatedRotation = new float[16];
	private float[] mCurrentRotation = new float[16];
	
    private int mProgram;
    
    // Shader uniform handles 
    private int muMVPMatrixHandle;
    
	// Shader attribute handles
    private int maPositionHandle;
    private int maNormalHandle;

    private Context mContext;
    private static String TAG = "GLES20Renderer";
    
    public volatile float mAngleX = 180.0f;
    public volatile float mAngleY = 90.0f;
    float scaleFactor = 1.0f;
    
}

class MyGLSurfaceView extends GLSurfaceView {

    private final GLES20Renderer mRenderer;

    public MyGLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new GLES20Renderer(context);
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
