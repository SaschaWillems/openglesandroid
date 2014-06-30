/*
 This code is licensed under the Mozilla Public License Version 2.0 (http://opensource.org/licenses/MPL-2.0)
 © 2014 by Sascha Willems - http://www.saschawillems.de
 
 Basic OpenGL ES 2.0 activity
*/


package de.saschawillems.stlviewer;

import de.saschawillems.stlviewer.R;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class GLActivity extends Activity {

    private GLSurfaceView mGLSurfaceView;	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_gl);   
        
        mGLSurfaceView = new MyGLSurfaceView(this);
        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
