/*
 * Copyright 2017 Google LLC
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

package com.google.ar.core.examples.java.helloar;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.Image;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.ArCoreApk.Availability;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.GLError;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer;
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {

  private Texture[] artworkTextures;
  private Mesh[] frameMeshes;
  private static final String TAG = HelloArActivity.class.getSimpleName();

  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
  private static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";

  // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
  // constants.
  private static final float[] sphericalHarmonicFactors = {
          0.282095f,
          -0.325735f,
          0.325735f,
          -0.325735f,
          0.273137f,
          -0.273137f,
          0.078848f,
          -0.273137f,
          0.136569f,
  };

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100f;

  private static final int CUBEMAP_RESOLUTION = 16;
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;


  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;
  private SampleRender render;

  private PlaneRenderer planeRenderer;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;

  private final DepthSettings depthSettings = new DepthSettings();
  private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];

  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
  private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
  // Assumed distance from the device camera to the surface on which user will try to place objects.
  // This value affects the apparent scale of objects while the tracking method of the
  // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
  // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
  // values for AR experiences where users are expected to place objects on surfaces close to the
  // camera. Use larger values for experiences where the user will likely be standing and trying to
  // place an object on the ground or floor in front of them.
  private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

  // Point Cloud
  private VertexBuffer pointCloudVertexBuffer;
  private Mesh pointCloudMesh;
  private Shader pointCloudShader;
  // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
  // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
  private long lastPointCloudTimestamp = 0;

  // Virtual objects (ARCore pawn)
  private Mesh virtualObjectMesh;
  private Shader virtualObjectShader;
  private Texture virtualObjectAlbedoTexture;
  private Texture virtualObjectAlbedoInstantPlacementTexture;

  private Texture frameAlbedoTexture;
  private Texture framePbrTexture;
  private Mesh frameMesh;
  private Shader frameShader;

  private int selectedArtwork = 0;

  private float scale = 1.0f;

  private final String[] ARTWORK_TEXTURE_PATHS = {
          "models/artwork_texture_1.png",
          "models/artwork_texture_2.png",
          "models/artwork_texture_3.png",
          "models/artwork_texture_4.png"
  };

    private final String[] FRAME_MODEL_PATHS = {
            "models/frame.obj",
            "models/classic_wood_frame.obj",
            "models/modern_frame.obj",
            "models/pawn.obj"
    };


    private final List<WrappedAnchor> wrappedAnchors = new ArrayList<>();

  // Environmental HDR
  private Texture dfgTexture;
  private SpecularCubemapFilter cubemapFilter;

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] modelMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] projectionMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16]; // view x model
  private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
  private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
  private final float[] viewInverseMatrix = new float[16];
  private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
  private final float[] viewLightDirection = new float[4]; // view x world light direction

  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      // --- Remove or Comment Out the Existing Artwork Button Setup ---
    /*
    ImageButton artworkButton1 = findViewById(R.id.artwork_button_1);
    ImageButton artworkButton2 = findViewById(R.id.artwork_button_2);
    ImageButton artworkButton3 = findViewById(R.id.artwork_button_3);
    ImageButton artworkButton4 = findViewById(R.id.artwork_button_4);

    View.OnClickListener artworkButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.artwork_button_1:
                    setArtworkTexture(0);
                    break;
                case R.id.artwork_button_2:
                    setArtworkTexture(1);
                    break;
                case R.id.artwork_button_3:
                    setArtworkTexture(2);
                    break;
                case R.id.artwork_button_4:
                    setArtworkTexture(3);
                    break;
            }
        }
    };

    artworkButton1.setOnClickListener(artworkButtonClickListener);
    artworkButton2.setOnClickListener(artworkButtonClickListener);
    artworkButton3.setOnClickListener(artworkButtonClickListener);
    artworkButton4.setOnClickListener(artworkButtonClickListener);
    */
      // --------------------------------------------------------------

      // --- Insert the Code to Set Up ViewPager2 and TabLayout ---

      // Initialize ViewPager2 and TabLayout
      ViewPager2 viewPager = findViewById(R.id.view_pager);
      TabLayout tabLayout = findViewById(R.id.tab_layout);

      // Create a list of fragments
      List<Fragment> fragments = new ArrayList<>();
      fragments.add(ArtworkButtonsFragment.newInstance());
      fragments.add(FrameButtonsFragment.newInstance());

      // Titles for the tabs
      List<String> fragmentTitles = new ArrayList<>();
      fragmentTitles.add("Artwork");
      fragmentTitles.add("Frame");

      // Set up the adapter
      ViewPagerAdapter adapter = new ViewPagerAdapter(this, fragments);
      viewPager.setAdapter(adapter);

      // Link the TabLayout and ViewPager2
      new TabLayoutMediator(tabLayout, viewPager,
              new TabLayoutMediator.TabConfigurationStrategy() {
                  @Override
                  public void onConfigureTab(TabLayout.Tab tab, int position) {
                      tab.setText(fragmentTitles.get(position));
                  }
              }).attach();

      // --------------------------------------------------------------

      // The rest of your existing onCreate code remains the same
      surfaceView = findViewById(R.id.surfaceview);
      displayRotationHelper = new DisplayRotationHelper(/* context= */ this);

      // Set up touch listener.
      tapHelper = new TapHelper(/* context= */ this);
      surfaceView.setOnTouchListener(tapHelper);

      // Set up renderer.
      render = new SampleRender(surfaceView, this, getAssets());

      installRequested = false;

      depthSettings.onCreate(this);
      instantPlacementSettings.onCreate(this);

      ImageButton settingsButton = findViewById(R.id.settings_button);
      settingsButton.setOnClickListener(
              new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                      PopupMenu popup = new PopupMenu(HelloArActivity.this, v);
                      popup.setOnMenuItemClickListener(HelloArActivity.this::settingsMenuClick);
                      popup.inflate(R.menu.settings_menu);
                      popup.show();
                  }
              });

      SeekBar scaleSeekBar = findViewById(R.id.scale_seekbar);
      scaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
              scale = 0.5f + (progress / 100.0f) * 1.5f; // Scale from 0.5 to 2.0
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
      });
  }


    private void updateAnchorTexture(WrappedAnchor wrappedAnchor) {
    if (virtualObjectAlbedoTexture == null) {
      Log.e(TAG, "Attempted to use a null texture");
      return;
    }

    Anchor anchor = wrappedAnchor.getAnchor();
    Trackable trackable = wrappedAnchor.getTrackable();
    if (anchor.getTrackingState() == TrackingState.TRACKING) {
      anchor.getPose().toMatrix(modelMatrix, 0);
      Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

      virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
      virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
      virtualObjectShader.setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture);
      render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer);
    }
  }



    public void setArtworkTexture(int index) {
        selectedArtwork = index;

        // Update the reference to the preloaded texture
        virtualObjectAlbedoTexture = artworkTextures[selectedArtwork];
        virtualObjectAlbedoInstantPlacementTexture = virtualObjectAlbedoTexture;

        // Update the shader's texture
        virtualObjectShader.setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture);
    }







  /** Menu button to launch feature specific settings. */
  protected boolean settingsMenuClick(MenuItem item) {
    if (item.getItemId() == R.id.depth_settings) {
      launchDepthSettingsMenuDialog();
      return true;
    } else if (item.getItemId() == R.id.instant_placement_settings) {
      launchInstantPlacementSettingsMenuDialog();
      return true;
    }
    return false;
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      session.close();
      session = null;
    }

      // Release all preloaded frame meshes
      if (frameMeshes != null) {
          for (Mesh mesh : frameMeshes) {
              if (mesh != null) {
                  mesh.close();
              }
          }
          frameMeshes = null;
      }

    // Release all preloaded textures
    if (artworkTextures != null) {
      for (Texture texture : artworkTextures) {
        if (texture != null) {
          texture.close();
        }
      }
      artworkTextures = null;
    }

    super.onDestroy();
  }


  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        // Always check the latest availability.
        Availability availability = ArCoreApk.getInstance().checkAvailability(this);

        // In all other cases, try to install ARCore and handle installation failures.
        if (availability != Availability.SUPPORTED_INSTALLED) {
          switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
            case INSTALL_REQUESTED:
              installRequested = true;
              return;
            case INSTALLED:
              break;
          }
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
      } catch (UnavailableArcoreNotInstalledException
               | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      configureSession();
      // To record a live camera session for later playback, call
      // `session.startRecording(recordingConfig)` at anytime. To playback a previously recorded AR
      // session instead of using the live camera feed, call
      // `session.setPlaybackDatasetUri(Uri)` before calling `session.resume()`. To
      // learn more about recording and playback, see:
      // https://developers.google.com/ar/develop/java/recording-and-playback
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }

    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

    @Override
    public void onSurfaceCreated(SampleRender render) {
        // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
        // an IOException.

        // Constants for asset paths
        final String DFG_RAW_PATH = "models/dfg.raw";
        final String POINT_CLOUD_VERTEX_SHADER_PATH = "shaders/point_cloud.vert";
        final String POINT_CLOUD_FRAGMENT_SHADER_PATH = "shaders/point_cloud.frag";
        // Remove this line since we'll use the array instead
        // final String ARTWORK_ALBEDO_TEXTURE_PATH = "models/artwork_texture_1.png";
        final String ARTWORK_PBR_TEXTURE_PATH = "models/artwork_texture_1.png";
        final String ARTWORK_MODEL_PATH = "models/artwork_plane.obj";
        // Remove FRAME_MODEL_PATH since we'll use an array
        // final String FRAME_MODEL_PATH = "models/frame.obj";
        final String FRAME_ALBEDO_TEXTURE_PATH = "models/frame_texture3.png";
        final String FRAME_PBR_TEXTURE_PATH = "models/frame_texture.png";
        final String ENVIRONMENTAL_HDR_VERTEX_SHADER_PATH = "shaders/environmental_hdr.vert";
        final String ENVIRONMENTAL_HDR_FRAGMENT_SHADER_PATH = "shaders/environmental_hdr.frag";

        try {
            planeRenderer = new PlaneRenderer(render);
            backgroundRenderer = new BackgroundRenderer(render);
            virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

            cubemapFilter = new SpecularCubemapFilter(render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);

            // Load DFG lookup table for environmental lighting
            dfgTexture = new Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE, /* useMipmaps= */ false);
            final int dfgResolution = 64;
            final int dfgChannels = 2;
            final int halfFloatSize = 2;

            ByteBuffer buffer = ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
            try (InputStream is = getAssets().open(DFG_RAW_PATH)) {
                is.read(buffer.array());
            }
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
            GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, /* level= */ 0, GLES30.GL_RG16F,
                    /* width= */ dfgResolution, /* height= */ dfgResolution, /* border= */ 0,
                    GLES30.GL_RG, GLES30.GL_HALF_FLOAT, buffer);
            GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

            // Point cloud
            pointCloudShader = Shader.createFromAssets(render,
                            POINT_CLOUD_VERTEX_SHADER_PATH, POINT_CLOUD_FRAGMENT_SHADER_PATH, /* defines= */ null)
                    .setVec4("u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f,
                            210.0f / 255.0f, 1.0f})
                    .setFloat("u_PointSize", 5.0f);
            pointCloudVertexBuffer = new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
            final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
            pointCloudMesh = new Mesh(render, Mesh.PrimitiveMode.POINTS,
                    /* indexBuffer= */ null, pointCloudVertexBuffers);

            // --- Inserted Code Starts Here ---

            // Preload all artwork textures
            artworkTextures = new Texture[ARTWORK_TEXTURE_PATHS.length];
            for (int i = 0; i < ARTWORK_TEXTURE_PATHS.length; i++) {
                artworkTextures[i] = Texture.createFromAsset(
                        render,
                        ARTWORK_TEXTURE_PATHS[i],
                        Texture.WrapMode.CLAMP_TO_EDGE,
                        Texture.ColorFormat.SRGB);
            }

            // Set the default texture to the first artwork
            virtualObjectAlbedoTexture = artworkTextures[0];
            virtualObjectAlbedoInstantPlacementTexture = artworkTextures[0];

            // Load PBR texture for the artwork (assuming it's the same for all artworks)
            Texture virtualObjectPbrTexture = Texture.createFromAsset(
                    render,
                    ARTWORK_PBR_TEXTURE_PATH,
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.LINEAR);

            // Load the virtual object mesh
            virtualObjectMesh = Mesh.createFromAsset(render, ARTWORK_MODEL_PATH);

            // Create the shader for the virtual object
            virtualObjectShader = Shader.createFromAssets(render,
                            ENVIRONMENTAL_HDR_VERTEX_SHADER_PATH,
                            ENVIRONMENTAL_HDR_FRAGMENT_SHADER_PATH,
                            /* defines= */ new HashMap<String, String>() {
                                {
                                    put("NUMBER_OF_MIPMAP_LEVELS",
                                            Integer.toString(cubemapFilter.getNumberOfMipmapLevels()));
                                }
                            })
                    .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
                    .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
                    .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
                    .setTexture("u_DfgTexture", dfgTexture);

            // --- Inserted Code Ends Here ---

            // --- New Inserted Code Starts Here ---

            // Preload all frame meshes
            frameMeshes = new Mesh[FRAME_MODEL_PATHS.length];
            for (int i = 0; i < FRAME_MODEL_PATHS.length; i++) {
                frameMeshes[i] = Mesh.createFromAsset(render, FRAME_MODEL_PATHS[i]);
            }

            // Set the default frame mesh
            frameMesh = frameMeshes[0];

            // --- New Inserted Code Ends Here ---

            // Load frame textures
            frameAlbedoTexture = Texture.createFromAsset(render,
                    FRAME_ALBEDO_TEXTURE_PATH, Texture.WrapMode.CLAMP_TO_EDGE, Texture.ColorFormat.SRGB);
            framePbrTexture = Texture.createFromAsset(render,
                    FRAME_PBR_TEXTURE_PATH, Texture.WrapMode.CLAMP_TO_EDGE, Texture.ColorFormat.LINEAR);

            // Create the shader for the frame
            frameShader = Shader.createFromAssets(render,
                            ENVIRONMENTAL_HDR_VERTEX_SHADER_PATH,
                            ENVIRONMENTAL_HDR_FRAGMENT_SHADER_PATH,
                            /* defines= */ new HashMap<String, String>() {
                                {
                                    put("NUMBER_OF_MIPMAP_LEVELS",
                                            Integer.toString(cubemapFilter.getNumberOfMipmapLevels()));
                                }
                            })
                    .setTexture("u_AlbedoTexture", frameAlbedoTexture)
                    .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", framePbrTexture)
                    .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
                    .setTexture("u_DfgTexture", dfgTexture);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read a required asset file", e);
            messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
        }
    }



    public void setFrameModel(int index) {
        if (index >= 0 && index < frameMeshes.length) {
            frameMesh = frameMeshes[index];
        } else {
            Log.e(TAG, "Invalid frame index");
        }
    }





    @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

    @Override
    public void onDrawFrame(SampleRender render) {
        if (session == null) {
            return;
        }

        if (!hasSetTextureNames) {
            session.setCameraTextureNames(new int[]{backgroundRenderer.getCameraColorTexture().getTextureId()});
            hasSetTextureNames = true;
        }

        displayRotationHelper.updateSessionIfNeeded(session);

        Frame frame;
        try {
            frame = session.update();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onDrawFrame", e);
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            return;
        }
        Camera camera = frame.getCamera();

        try {
            backgroundRenderer.setUseDepthVisualization(render, depthSettings.depthColorVisualizationEnabled());
            backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
        } catch (IOException e) {
            Log.e(TAG, "Failed to read a required asset file", e);
            messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
            return;
        }

        backgroundRenderer.updateDisplayGeometry(frame);

        if (camera.getTrackingState() == TrackingState.TRACKING
                && (depthSettings.useDepthForOcclusion() || depthSettings.depthColorVisualizationEnabled())) {
            try (Image depthImage = frame.acquireDepthImage16Bits()) {
                backgroundRenderer.updateCameraDepthTexture(depthImage);
            } catch (NotYetAvailableException e) {
                // This normally means that depth data is not available yet. This is normal so we will not
                // spam the logcat with this.
            }
        }

        handleTap(frame, camera);
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        String message = null;
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
                message = SEARCHING_PLANE_MESSAGE;
            } else {
                message = TrackingStateHelper.getTrackingFailureReasonString(camera);
            }
        } else if (hasTrackingPlane()) {
            if (wrappedAnchors.isEmpty()) {
                message = WAITING_FOR_TAP_MESSAGE;
            }
        } else {
            message = SEARCHING_PLANE_MESSAGE;
        }
        if (message == null) {
            messageSnackbarHelper.hide(this);
        } else {
            messageSnackbarHelper.showMessage(this, message);
        }

        if (frame.getTimestamp() != 0) {
            backgroundRenderer.drawBackground(render);
        }

        if (camera.getTrackingState() == TrackingState.PAUSED) {
            return;
        }

        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);
        camera.getViewMatrix(viewMatrix, 0);

        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
                pointCloudVertexBuffer.set(pointCloud.getPoints());
                lastPointCloudTimestamp = pointCloud.getTimestamp();
            }
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(pointCloudMesh, pointCloudShader);
        }

        planeRenderer.drawPlanes(render, session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projectionMatrix);

        updateLightEstimation(frame.getLightEstimate(), viewMatrix);

        // Clear the framebuffer
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);

        // Loop over anchors and render virtual objects
        for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
            Anchor anchor = wrappedAnchor.getAnchor();
            Trackable trackable = wrappedAnchor.getTrackable();
            if (anchor.getTrackingState() != TrackingState.TRACKING) {
                continue;
            }

            // Compute the model matrix for the anchor
            anchor.getPose().toMatrix(modelMatrix, 0);

            // Apply scaling to the model matrix
            float[] scaleMatrix = new float[16];
            Matrix.setIdentityM(scaleMatrix, 0);
            Matrix.scaleM(scaleMatrix, 0, scale, scale, 1.0f);
            Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);

            // Compute model-view and model-view-projection matrices
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

            // Render the virtual artwork object
            if (virtualObjectAlbedoTexture == null) {
                Log.e(TAG, "Attempted to use a null texture");
                return;
            }

            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            if (trackable instanceof InstantPlacementPoint
                    && ((InstantPlacementPoint) trackable).getTrackingMethod()
                    == InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE) {
                virtualObjectShader.setTexture("u_AlbedoTexture", virtualObjectAlbedoInstantPlacementTexture);
            } else {
                virtualObjectShader.setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture);
            }
            render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer);

            // --- Rendering the Frame Using frameMesh ---
            // This is where the frame is rendered using the current frameMesh
            frameShader.setMat4("u_ModelView", modelViewMatrix);
            frameShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(frameMesh, frameShader, virtualSceneFramebuffer);
            // --------------------------------------------

            // The frameMesh used above is dynamically updated when setFrameModel(int index) is called
        }

        // Draw the virtual scene with the virtual objects and frame
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
    }








    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      List<HitResult> hitResultList;
      if (instantPlacementSettings.isInstantPlacementEnabled()) {
        hitResultList = frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS);
      } else {
        hitResultList = frame.hitTest(tap);
      }
      for (HitResult hit : hitResultList) {
        Trackable trackable = hit.getTrackable();
        if ((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()) && PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0)
                || (trackable instanceof Point && ((Point) trackable).getOrientationMode() == OrientationMode.ESTIMATED_SURFACE_NORMAL)
                || (trackable instanceof InstantPlacementPoint)
                || (trackable instanceof DepthPoint)) {

          // Ensure only one object is placed at a time
          if (!wrappedAnchors.isEmpty()) {
            wrappedAnchors.get(0).getAnchor().detach();
            wrappedAnchors.clear();
          }

          // Adding a new anchor
          wrappedAnchors.add(new WrappedAnchor(hit.createAnchor(), trackable));
          this.runOnUiThread(this::showOcclusionDialogIfNeeded);

          break; // Only handle the closest hit
        }
      }
    }
  }



  /**
   * Shows a pop-up dialog on the first call, determining whether the user wants to enable
   * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
   */
  private void showOcclusionDialogIfNeeded() {
    boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
    if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
      return; // Don't need to show dialog.
    }

    // Asks the user whether they want to use depth-based occlusion.
    new AlertDialog.Builder(this)
            .setTitle(R.string.options_title_with_depth)
            .setMessage(R.string.depth_use_explanation)
            .setPositiveButton(
                    R.string.button_text_enable_depth,
                    (DialogInterface dialog, int which) -> {
                      depthSettings.setUseDepthForOcclusion(true);
                    })
            .setNegativeButton(
                    R.string.button_text_disable_depth,
                    (DialogInterface dialog, int which) -> {
                      depthSettings.setUseDepthForOcclusion(false);
                    })
            .show();
  }

  private void launchInstantPlacementSettingsMenuDialog() {
    resetSettingsMenuDialogCheckboxes();
    Resources resources = getResources();
    new AlertDialog.Builder(this)
            .setTitle(R.string.options_title_instant_placement)
            .setMultiChoiceItems(
                    resources.getStringArray(R.array.instant_placement_options_array),
                    instantPlacementSettingsMenuDialogCheckboxes,
                    (DialogInterface dialog, int which, boolean isChecked) ->
                            instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked)
            .setPositiveButton(
                    R.string.done,
                    (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
            .setNegativeButton(
                    android.R.string.cancel,
                    (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
            .show();
  }

  /** Shows checkboxes to the user to facilitate toggling of depth-based effects. */
  private void launchDepthSettingsMenuDialog() {
    // Retrieves the current settings to show in the checkboxes.
    resetSettingsMenuDialogCheckboxes();

    // Shows the dialog to the user.
    Resources resources = getResources();
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      // With depth support, the user can select visualization options.
      new AlertDialog.Builder(this)
              .setTitle(R.string.options_title_with_depth)
              .setMultiChoiceItems(
                      resources.getStringArray(R.array.depth_options_array),
                      depthSettingsMenuDialogCheckboxes,
                      (DialogInterface dialog, int which, boolean isChecked) ->
                              depthSettingsMenuDialogCheckboxes[which] = isChecked)
              .setPositiveButton(
                      R.string.done,
                      (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
              .setNegativeButton(
                      android.R.string.cancel,
                      (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
              .show();
    } else {
      // Without depth support, no settings are available.
      new AlertDialog.Builder(this)
              .setTitle(R.string.options_title_without_depth)
              .setPositiveButton(
                      R.string.done,
                      (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
              .show();
    }
  }

  private void applySettingsMenuDialogCheckboxes() {
    depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0]);
    depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1]);
    instantPlacementSettings.setInstantPlacementEnabled(
            instantPlacementSettingsMenuDialogCheckboxes[0]);
    configureSession();
  }

  private void resetSettingsMenuDialogCheckboxes() {
    depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion();
    depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled();
    instantPlacementSettingsMenuDialogCheckboxes[0] =
            instantPlacementSettings.isInstantPlacementEnabled();
  }

  /** Checks if we detected at least one plane. */
  private boolean hasTrackingPlane() {
    for (Plane plane : session.getAllTrackables(Plane.class)) {
      if (plane.getTrackingState() == TrackingState.TRACKING) {
        return true;
      }
    }
    return false;
  }

  /** Update state based on the current frame's light estimation. */
  private void updateLightEstimation(LightEstimate lightEstimate, float[] viewMatrix) {
    if (lightEstimate.getState() != LightEstimate.State.VALID) {
      virtualObjectShader.setBool("u_LightEstimateIsValid", false);
      return;
    }
    virtualObjectShader.setBool("u_LightEstimateIsValid", true);

    Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0);
    virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix);

    updateMainLight(
            lightEstimate.getEnvironmentalHdrMainLightDirection(),
            lightEstimate.getEnvironmentalHdrMainLightIntensity(),
            viewMatrix);
    updateSphericalHarmonicsCoefficients(
            lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics());
    cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap());
  }

  private void updateMainLight(float[] direction, float[] intensity, float[] viewMatrix) {
    // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
    worldLightDirection[0] = direction[0];
    worldLightDirection[1] = direction[1];
    worldLightDirection[2] = direction[2];
    Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0);
    virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection);
    virtualObjectShader.setVec3("u_LightIntensity", intensity);
  }

  private void updateSphericalHarmonicsCoefficients(float[] coefficients) {
    // Pre-multiply the spherical harmonics coefficients before passing them to the shader. The
    // constants in sphericalHarmonicFactors were derived from three terms:
    //
    // 1. The normalized spherical harmonics basis functions (y_lm)
    //
    // 2. The lambertian diffuse BRDF factor (1/pi)
    //
    // 3. A <cos> convolution. This is done to so that the resulting function outputs the irradiance
    // of all incoming light over a hemisphere for a given surface normal, which is what the shader
    // (environmental_hdr.frag) expects.
    //
    // You can read more details about the math here:
    // https://google.github.io/filament/Filament.html#annex/sphericalharmonics

    if (coefficients.length != 9 * 3) {
      throw new IllegalArgumentException(
              "The given coefficients array must be of length 27 (3 components per 9 coefficients");
    }

    // Apply each factor to every component of each coefficient
    for (int i = 0; i < 9 * 3; ++i) {
      sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3];
    }
    virtualObjectShader.setVec3Array(
            "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients);
  }

  /** Configures the session with feature settings. */
  private void configureSession() {
    Config config = session.getConfig();
    config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
    } else {
      config.setDepthMode(Config.DepthMode.DISABLED);
    }
    if (instantPlacementSettings.isInstantPlacementEnabled()) {
      config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP);
    } else {
      config.setInstantPlacementMode(InstantPlacementMode.DISABLED);
    }
    session.configure(config);
    config.setPlaneFindingMode(Config.PlaneFindingMode.VERTICAL);
  }
}



/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
 */
class WrappedAnchor {
  private Anchor anchor;
  private Trackable trackable;

  public WrappedAnchor(Anchor anchor, Trackable trackable) {
    this.anchor = anchor;
    this.trackable = trackable;
  }

  public Anchor getAnchor() {
    return anchor;
  }

  public Trackable getTrackable() {
    return trackable;
  }
}