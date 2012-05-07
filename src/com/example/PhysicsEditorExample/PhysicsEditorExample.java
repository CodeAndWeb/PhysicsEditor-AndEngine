package com.example.PhysicsEditorExample;
 /**
 * by Adrian Nilsson (ade at ade dot se)
 * BIG IRON GAMES (bigirongames.org)
 *
 * modified from original by Nicolas Gramlich - http://code.google.com/p/andengineexamples/source/browse/src/org/anddev/andengine/examples/PhysicsExample.java
 */

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.physics.box2d.PhysicsConnector;
import org.anddev.andengine.extension.physics.box2d.PhysicsFactory;
import org.anddev.andengine.extension.physics.box2d.PhysicsWorld;
import org.anddev.andengine.extension.physics.box2d.util.Vector2Pool;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;
import org.anddev.andengine.ui.activity.BaseGameActivity;

import android.hardware.SensorManager;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

import java.util.HashMap;
import java.util.Random;

public class PhysicsEditorExample extends BaseGameActivity implements IAccelerometerListener, IOnSceneTouchListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 800;
	private static final int CAMERA_HEIGHT = 480;

	// ===========================================================
	// Fields
	// ===========================================================

	private BitmapTextureAtlas mBitmapTextureAtlas;
    private PhysicsEditorShapeLibrary physicsEditorShapeLibrary;
	private Scene mScene;
	private PhysicsWorld mPhysicsWorld;

    //Texture regions
    private TextureRegion textureBurger;
    private TextureRegion textureDrink;
    private TextureRegion textureHotdog;
    private TextureRegion textureIcecream;
    private TextureRegion textureIcecream2;
    private TextureRegion textureIcecream3;
    private HashMap<String, TextureRegion> textureRegionHashMap = new HashMap<String, TextureRegion>();


	public Engine onLoadEngine() {
		Toast.makeText(this, "Touch the screen to add objects.", Toast.LENGTH_LONG).show();
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
		engineOptions.getTouchOptions().setRunOnUpdateThread(true);
		return new Engine(engineOptions);
	}

	public void onLoadResources() {
	    /* Manual layout of TextureRegions. */
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		this.mBitmapTextureAtlas = new BitmapTextureAtlas(256, 256, TextureOptions.DEFAULT);

		this.textureDrink = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "drink.png", 0, 0);
		this.textureBurger = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "hamburger.png", 80, 89);
		this.textureHotdog = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "hotdog.png", 0, 189);
        this.textureIcecream = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "icecream.png", 80, 0);
		this.textureIcecream2 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "icecream2.png", 207, 0);
        this.textureIcecream3 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "icecream3.png", 183, 102);
        textureRegionHashMap.put("drink", this.textureDrink);
        textureRegionHashMap.put("hamburger", this.textureBurger);
        textureRegionHashMap.put("hotdog", this.textureHotdog);
        textureRegionHashMap.put("icecream", this.textureIcecream);
        textureRegionHashMap.put("icecream2", this.textureIcecream2);
        textureRegionHashMap.put("icecream3", this.textureIcecream3);
		this.mEngine.getTextureManager().loadTexture(this.mBitmapTextureAtlas);

        //Create the shape importer and open our shape definition file
        this.physicsEditorShapeLibrary = new PhysicsEditorShapeLibrary();
        this.physicsEditorShapeLibrary.open(this, "shapes/shapes.xml");

	}

	public Scene onLoadScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		this.mScene = new Scene();
		this.mScene.setBackground(new ColorBackground(255, 255, 255));
		this.mScene.setOnSceneTouchListener(this);

		this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);

		final Shape ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2);
		final Shape roof = new Rectangle(0, 0, CAMERA_WIDTH, 2);
		final Shape left = new Rectangle(0, 0, 2, CAMERA_HEIGHT);
		final Shape right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		this.mScene.attachChild(ground);
		this.mScene.attachChild(roof);
		this.mScene.attachChild(left);
		this.mScene.attachChild(right);

		this.mScene.registerUpdateHandler(this.mPhysicsWorld);

		return this.mScene;
	}

	public void onLoadComplete() {

	}

	public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
		if(this.mPhysicsWorld != null) {
			if(pSceneTouchEvent.isActionDown()) {
				this.addFace(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
				return true;
			}
		}
		return false;
	}

	public void onAccelerometerChanged(final AccelerometerData pAccelerometerData) {
		final Vector2 gravity = Vector2Pool.obtain(pAccelerometerData.getX(), pAccelerometerData.getY());
		this.mPhysicsWorld.setGravity(gravity);
		Vector2Pool.recycle(gravity);
	}

	@Override
	public void onResumeGame() {
		super.onResumeGame();

		this.enableAccelerometerSensor(this);
	}

	@Override
	public void onPauseGame() {
		super.onPauseGame();

		this.disableAccelerometerSensor();
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void addFace(final float pX, final float pY) {
        Random rnd = new Random();

        //Specify a shape name to import
        String[] shapes = new String[] {"drink", "hamburger", "icecream", "icecream2", "icecream3"};
        String shapeName = shapes[rnd.nextInt(shapes.length-1)];

        //Create the graphics
        Sprite sprite = new Sprite(0, 0, this.textureRegionHashMap.get(shapeName));
        sprite.setPosition(pX - sprite.getWidth()/2, pY - sprite.getHeight()/2);

        //Get physics body
        Body body = this.physicsEditorShapeLibrary.createBody(shapeName, sprite, this.mPhysicsWorld);

        //Attach to physics world & scene
		this.mScene.attachChild(sprite);
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(sprite, body, true, true));
	}
}
