/*
 * Copyright (c) 2017-present, Viro, Inc.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.aucutt.arbucks;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.virosample.R;
import com.viro.core.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Activity that initializes Viro and ARCore. This activity builds an AR scene that lets the user
 * place and drag objects. Tap on the 'Viro' button to get a dialog of objects to place in the scene.
 * Once placed, the objects can be dragged, rotated, and scaled using pinch and rotate gestures.
 */
public class ViroActivity extends Activity {
    private static final String TAG = ViroActivity.class.getSimpleName();

    // Constants used to determine if plane or point is within bounds. Units in meters.
    private static final float MIN_DISTANCE = 0.2f;
    private static final float MAX_DISTANCE = 10f;
    private ViroView mViroView;
    private final static String TAG2 ="Darrell";

    /**
     * The ARScene we will be creating within this activity.
     */
    private ARScene mScene;

    /**
     * List of draggable 3D objects in our scene.
     */
    private List<Draggable3DObject> mDraggableObjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDraggableObjects = new ArrayList<Draggable3DObject>();
        mViroView = new ViroViewARCore(this, new ViroViewARCore.StartupListener() {
            @Override
            public void onSuccess() {
                displayScene();
            }

            @Override
            public void onFailure(ViroViewARCore.StartupError error, String errorMessage) {
                Log.e(TAG, "Error initializing AR [" + errorMessage + "]");
            }
        });
        setContentView(mViroView);
    }

    /**
     * Contains logic for placing, dragging, rotating, and scaling a 3D object in AR.
     */
    private class Draggable3DObject {

        private String mFileName;
        private float rotateStart;
        private float scaleStart;

        public Draggable3DObject(String filename) {
            mFileName = filename;
        }

        private void addModelToPosition(Vector position) {
            final Object3D object3D = new Object3D();

            object3D.setPosition(position);
            // Shrink the objects as the original size is too large.
            float darrellScale = 0.07f;
            object3D.setScale(new Vector(darrellScale, darrellScale, darrellScale));
            rotateStart = object3D.getRotationEulerRealtime().y;
            float rotatorx = -0.68f;
            float rotatory = -0.68f;
            object3D.setRotation(new Vector(rotatorx, rotatory, 0));


           // object3D.setRotation();
            object3D.setGestureRotateListener(new GestureRotateListener() {
                @Override
                public void onRotate(int i, Node node, float rotation, RotateState rotateState) {
                    if(rotateState == RotateState.ROTATE_START) {
                        rotateStart = object3D.getRotationEulerRealtime().y;
                        Log.d(TAG2, "rotate start "  + rotateStart   +  "  thing " +  rotation);
                    }
                    float totalRotationY = rotateStart + rotation;
                    object3D.setRotation(new Vector(0, totalRotationY, 0));
                }
            });


            object3D.setClickListener(new ClickListener() {
                @Override
                public void onClick(int i, Node node, Vector vector) {
                    Log.d(TAG2,"destroy ");
                    object3D.removeFromParentNode();
                    object3D.dispose();
                }

                @Override
                public void onClickState(int i, Node node, ClickState clickState, Vector vector) {

                }
            });
            //TODO I don't need this, but maybe a click listener.
            object3D.setGesturePinchListener(new GesturePinchListener() {
                @Override
                public void onPinch(int i, Node node, float scale, PinchState pinchState) {
                    if(pinchState == PinchState.PINCH_START) {
                        scaleStart = object3D.getScaleRealtime().x;
                        Log.d(TAG2, " this is the scale " + scaleStart);
                    } else {
                        object3D.setScale(new Vector(scaleStart * scale, scaleStart * scale, scaleStart * scale));
                    }
                }
            });

            object3D.setDragListener(new DragListener() {
                @Override
                public void onDrag(int i, Node node, Vector vector, Vector vector1) {
                    Log.d(TAG2, " int " + i + " vector "  + vector.toString()  +  "  vertor 2 "  + vector1.toString());
                }
            });

            // Load the Android model asynchronously.
            object3D.loadModel(Uri.parse(mFileName), Object3D.Type.FBX, new AsyncObject3DListener() {
                @Override
                public void onObject3DLoaded(final Object3D object, final Object3D.Type type) {

                }

                @Override
                public void onObject3DFailed(String s) {
                    Toast.makeText(ViroActivity.this, "An error occured when loading the 3D Object!",
                            Toast.LENGTH_LONG).show();
                }
            });

            // Make the object draggable.
            object3D.setDragType(Node.DragType.FIXED_TO_WORLD);
            mScene.getRootNode().addChildNode(object3D);
        }
    }

    private void displayScene() {
        mScene = new ARScene();
        // Add a listener to the scene so we can update the 'AR Initialized' text.
        mScene.setListener(new ARSceneListener(this, mViroView));
        // Add a light to the scene so our models show up
        mScene.getRootNode().addLight(new AmbientLight(Color.WHITE, 1000f));
        mViroView.setScene(mScene);
        View.inflate(this, R.layout.viro_view_ar_hit_test_hud, ((ViewGroup) mViroView));
    }

    /**
     * Perform a hit-test and place the object (identified by its file name) at the intersected
     * location.
     *
     * @param fileName The resource name of the object to place.
     */
    private void placeObject(final String fileName) {
        ViroViewARCore viewARView = (ViroViewARCore) mViroView;
        final Vector cameraPos = viewARView.getLastCameraPositionRealtime();
      //  Vector lamePos = cameraPos.add( new Vector(5, 0, 0));
        Log.d(TAG2, " have a Vector " +  cameraPos.toString());
        //add3DDraggableObject(fileName,new  Vector(0,0,0));
        Random r = new Random();
        Boolean random = r.nextBoolean();
        Log.d(TAG2, " random " + random );
        add3DDraggableObject(fileName, cameraPos.add( new Vector(0, 0, random ? 1 : -1)));
    }

    /**
     * Add a 3D object with the given filename to the scene at the specified world position.
     */
    private void add3DDraggableObject(String filename, Vector position) {
        Draggable3DObject draggable3DObject = new Draggable3DObject(filename);
        mDraggableObjects.add(draggable3DObject);
        draggable3DObject.addModelToPosition(position);
    }


    public void showStar(View v) {
        placeObject("file:///android_asset/facestar.vrx");
    }
    /**
     * Private class that implements ARScene.Listener callbacks. In this example we use this to notify
     * the user when AR is initialized.
     */
    private static class ARSceneListener implements ARScene.Listener {
        private WeakReference<Activity> mCurrentActivityWeak;

        public ARSceneListener(Activity activity, View rootView) {
            mCurrentActivityWeak = new WeakReference<Activity>(activity);
        }

        @Override
        public void onTrackingInitialized() {
            Activity activity = mCurrentActivityWeak.get();
            if (activity == null) {
                return;
            }

            TextView initText = (TextView) activity.findViewById(R.id.initText);
            initText.setText("AR is initialized");
        }

        @Override
        public void onAmbientLightUpdate(float v, float v1) {

        }

        @Override
        public void onAnchorFound(ARAnchor arAnchor, ARNode arNode) {

        }

        @Override
        public void onAnchorRemoved(ARAnchor arAnchor, ARNode arNode) {

        }

        @Override
        public void onAnchorUpdated(ARAnchor arAnchor, ARNode arNode) {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mViroView.onActivityStarted(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mViroView.onActivityResumed(this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        mViroView.onActivityPaused(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mViroView.onActivityStopped(this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mViroView.onActivityDestroyed(this);
    }
}
