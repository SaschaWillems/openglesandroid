/* 
 * Copyright © 2014 by Sascha Willems - http://www.saschawillems.de
 * 
 * This code is licensed under the Mozilla Public License Version 2.0 (http://opensource.org/licenses/MPL-2.0) 
*/

package de.saschawillems.simplehud;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GLActivity extends Activity {

    private GLSurfaceView mGLSurfaceView;	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);        
        
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
