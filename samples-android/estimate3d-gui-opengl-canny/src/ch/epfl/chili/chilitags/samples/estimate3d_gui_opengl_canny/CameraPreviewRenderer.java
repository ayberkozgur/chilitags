package ch.epfl.chili.chilitags.samples.estimate3d_gui_opengl_canny;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ch.epfl.chili.chilitags.Chilitags3D;
import ch.epfl.chili.chilitags.ObjectTransform;
import ch.epfl.chili.chilitags.samples.estimate3d_gui_opengl_canny.shader.GaussianBlurShader;
import ch.epfl.chili.chilitags.samples.estimate3d_gui_opengl_canny.shader.CameraRGBShader;
import ch.epfl.chili.chilitags.samples.estimate3d_gui_opengl_canny.shader.Shader;
import ch.epfl.chili.chilitags.samples.estimate3d_gui_opengl_canny.shader.YUV2RGBShader;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class CameraPreviewRenderer implements GLSurfaceView.Renderer {

	
	private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
	
	
	
	
	
	
	private int[] texName;
	
	
	
	
	
	
	
	
	private CameraController camController; //The camera controller object that will provide the background image
	private Chilitags3D chilitags; //The Chilitags object that detects the tags

	//The Y and UV buffers that will pass our image channel data to the textures
	//private ByteBuffer yBuffer;
	//private ByteBuffer uvBuffer;
	
	private byte[] buf;
	ByteBuffer bbuf;

	//The Y and UV texture objects
	private int yTextureHandle;
	private int uvTextureHandle;
	private int[] yuvTextureNames; //Index 0: Y texture, Index 1: UV texture
	private int[] ppTextureNames; //Pingpong texture names

	private int[] renderBufferName;
	private int[] frameBufferName;

	//Our line object
	private GLESLine line;

	//private int shaderProgramHandle; //The shader program object that will do the YUV-RGB conversion for us
	private Shader yuv2rgbShader;

	private int positionHandle; //The location of the a_position attribute object
	private int texCoordHandle; //The location of the a_texCoord attribute object

	//The vertices and indices of our mesh that we will draw the camera preview image on
	private final float[] verticesData = { 
			-1.f, 1.f, // Position 0
			0.0f, 0.0f, // TexCoord 0
			-1.f, -1.f, // Position 1
			0.0f, 1.0f, // TexCoord 1
			1.f, -1.f, // Position 2
			1.0f, 1.0f, // TexCoord 2
			1.f, 1.f, // Position 3
			1.0f, 0.0f // TexCoord 3
	};
	private final short[] indicesData = { 0, 1, 2, 0, 2, 3 };
	private FloatBuffer vertices;
	private ShortBuffer indices;

	private double[][] camMat; //The camera matrix
	private double xScale; //(Camera image width)/(Chilitags processing image width)
	private double yScale; //(Camera image height)/(Chilitags processing image height)

	/**
	 * Creates a new renderer for our GL surface. It will render the camera image on the background and the frames of all detected tags.
	 * 
	 * @param camController The camera controller that holds the camera image buffer
	 * @param chilitags The Chilitags3D object that detects tags and calculates their transforms w.r.t the camera
	 * @param camCalib Camera calibration matrix that was fed to Chilitags3D
	 * @param xScale The inverse of the downscale value that was induced to the Chilitags processing image width vs. the camera image width
	 * @param yScale The inverse of the downscale value that was induced to the Chilitags processing image height vs. the camera image height
	 */
	public CameraPreviewRenderer(CameraController camController, Chilitags3D chilitags, double[] camCalib, double xScale, double yScale){
		this.camController = camController;
		this.chilitags = chilitags;

		/*
		 * GLES stuff
		 */

		//Create our shaders
		//yuv2rgbShader = new GaussianBlurShader(camController.cameraWidth,camController.cameraHeight);
		yuv2rgbShader = new CameraRGBShader();
		
		//Allocate vertices of our mesh on native memory space
		vertices = ByteBuffer.allocateDirect(verticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertices.put(verticesData);
		vertices.position(0);

		//Allocate indices of our mesh on native memory space
		indices = ByteBuffer.allocateDirect(indicesData.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
		indices.put(indicesData);
		indices.position(0);

		//Allocate image channel buffers on the native memory space
		buf = new byte[camController.processingWidth*camController.processingHeight*4];
		bbuf = ByteBuffer.allocateDirect(4*camController.processingWidth*camController.processingHeight).order(ByteOrder.nativeOrder());//ByteBuffer.wrap(buf);
		//yBuffer = ByteBuffer.allocateDirect(camController.cameraWidth*camController.cameraHeight).order(ByteOrder.nativeOrder());
		//uvBuffer = ByteBuffer.allocateDirect(camController.cameraWidth*camController.cameraHeight/2).order(ByteOrder.nativeOrder()); //We have (width/2*height/2) pixels, each pixel is 2 bytes

		//Allocate our line object
		line = new GLESLine();

		/*
		 * Prepare the transforms we will use
		 */

		//Get the camera matrix
		camMat = new double[4][4];
		for(int i=0;i<3;i++)
			for(int j=0;j<3;j++)
				camMat[i][j] = camCalib[i*3+j];
		camMat[3][2] = 1; //This is for getting the Z coordinate on the last element of the vector when multiplied with the camera matrix

		//Get the scaling values
		this.xScale = xScale;
		this.yScale = yScale;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {

		/*
		 * Prepare our frame buffer and render buffer
		 */
		
		//Get a new handle for our render buffer
		renderBufferName = new int[1];
		GLES20.glGenRenderbuffers(1, renderBufferName, 0);
		
		//Allocate space for our render buffer
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBufferName[0]);
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGB565, camController.processingWidth, camController.processingHeight);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
		
		//Get a new handle for our framebuffer
		frameBufferName = new int[1];
		GLES20.glGenFramebuffers(1, frameBufferName, 0);
		
		//Attach our render buffer to our frame buffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferName[0]);
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, renderBufferName[0]);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		
		
		/*frameBuffer = IntBuffer.allocate(1);
		renderBuffer = IntBuffer.allocate(1);
		GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		GLES20.glGenFramebuffers(1, frameBuffer);
		GLES20.glGenRenderbuffers(1, renderBuffer);
		GLES20.glActiveTexture(GLES20.GL_ACTIVE_TEXTURE);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer.get(0));
		GLES20.glClear(0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffer.get(0));     
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, camController.cameraWidth, camController.cameraHeight);
		parameterBufferHeigth = IntBuffer.allocate(1);
		parameterBufferWidth = IntBuffer.allocate(1);
		GLES20.glGetRenderbufferParameteriv(GLES20.GL_RENDERBUFFER, GLES20.GL_RENDERBUFFER_WIDTH, parameterBufferWidth);
		GLES20.glGetRenderbufferParameteriv(GLES20.GL_RENDERBUFFER, GLES20.GL_RENDERBUFFER_HEIGHT, parameterBufferHeigth);
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, renderBuffer.get(0));
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glClear(0);*/

		/*
		 * Prepare the shader stuff
		 */

		//Compile and load our shaders
		yuv2rgbShader.load();
		line.load();

		//Get the attribute locations
		positionHandle = GLES20.glGetAttribLocation(yuv2rgbShader.getHandle(), "a_position");
		texCoordHandle = GLES20.glGetAttribLocation(yuv2rgbShader.getHandle(), "a_texCoord");

		//Get locations of uniforms that point to our textures
		yTextureHandle = GLES20.glGetUniformLocation(yuv2rgbShader.getHandle(), "texture");
		//yTextureHandle = GLES20.glGetUniformLocation(yuv2rgbShader.getHandle(), "y_texture");
		//uvTextureHandle = GLES20.glGetUniformLocation(yuv2rgbShader.getHandle(), "uv_texture");

		
		
		
		
		

		texName = new int[1];
		GLES20.glGenTextures(1, texName, 0);

		
		
		
		
		
		
		
		
		
		
		/*
		 * Prepare the texture stuff
		 */

		//Create the YUV texture object names
		/*GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		yuvTextureNames = new int[2];
		GLES20.glGenTextures(2, yuvTextureNames, 0);

		//Create the ping pong texture object names
		ppTextureNames = new int[2];
		GLES20.glGenTextures(2, ppTextureNames, 0);

		//Ping pong 0
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ppTextureNames[0]);

		//Use linear interpolation when magnifying/minifying the texture to areas larger/smaller than the texture size
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

		//Create the texture
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, camController.cameraWidth, camController.cameraHeight, 0, GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE, null);

		//Ping pong 1
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ppTextureNames[1]);

		//Use linear interpolation when magnifying/minifying the texture to areas larger/smaller than the texture size
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

		//Create the texture
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, camController.cameraWidth, camController.cameraHeight, 0, GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE, null);
*/
		//Clear the screen
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
		camController.startPreview(texName[0]);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		GLES20.glActiveTexture(GLES20.GL_ACTIVE_TEXTURE);
		GLES20.glViewport(0, 0, width, height);
	}














	/*
	byte[] blurred = new byte[1280*720];
	byte[] buf = new byte[1280*720];
	int[] grad = new int[1280*720];
	int[] grad2 = new int[1280*720];
	byte[] dir = new byte[1280*720];
	 */












	@Override
	public void onDrawFrame(GL10 unused) {

		//long ms = System.currentTimeMillis();
		
		//Clear the screen
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		//Get the camera image
		//byte[] cameraImage = camController.getPictureData();
		//if(cameraImage != null){

			/*System.arraycopy(cameraImage, 0, buf, 0, 1280*720);
			final int[] gaussian = {
					2,4,5,4,2,
					4,9,12,9,4,
					5,12,15,12,5,
					4,9,12,9,4,
					2,4,5,4,2};

			final int[] Gx = {
					-1, 0, 1,
					-2, 0, 2,
					-1, 0, 1
			};

			final int[] Gy = {
					-1, -2, -1,
					0, 0, 0,
					1, 2, 1
			};

			int t;

			//Gaussian blur
			for(int x=0;x<1280;x++)
				for(int y=0;y<720;y++){
					t = 0;
					for(int xf=-2;xf<=2;xf++)
						for(int yf=-2;yf<=2;yf++)
							if(x+xf>=0 && x+xf<1280 && y+yf>=0 && y+yf<720)
								t += gaussian[xf + 2 + (yf + 2)*5]*(0xFF & buf[(y + yf)*1280 + (x + xf)]);
					t /= 159;
					blurred[y*1280 + x] = (byte)t;	
				}


			//Gradient
			int gx,gy;
			double angle;
			for(int x=0;x<1280;x++)
				for(int y=0;y<720;y++){
					gx = 0;
					gy = 0;
					for(int xf=-1;xf<=1;xf++)
						for(int yf=-1;yf<=1;yf++)
							if(x+xf>=0 && x+xf<1280 && y+yf>=0 && y+yf<720){
								gx += Gx[xf + 1 + (yf + 1)*3]*(0xFF & blurred[(y + yf)*1280 + (x + xf)]);
								gy += Gy[xf + 1 + (yf + 1)*3]*(0xFF & blurred[(y + yf)*1280 + (x + xf)]);
							}

					grad[y*1280 + x] = (int)Math.hypot(gx, gy);	
					angle = Math.atan2(gy, gx);
					if(angle < 0)
						angle += Math.PI;

					if(angle <= Math.PI/8)
						dir[y*1280 + x] = 0;
					else if(Math.PI/8 < angle && angle <= 3*Math.PI/8)
						dir[y*1280 + x] = 1;
					else if(3*Math.PI/8 < angle && angle <= 5*Math.PI/8)
						dir[y*1280 + x] = 2;
					else if(5*Math.PI/8 < angle && angle <= 7*Math.PI/8)
						dir[y*1280 + x] = 3;
					else
						dir[y*1280 + x] = 0;
				}

			//Non-maximum suppression
			for(int x=1;x<1280 - 1;x++)
				for(int y=1;y<720 - 1;y++){
					grad2[y*1280 + x] = grad[y*1280 + x];
					switch(dir[y*1280 + x]){
					case 0:
						if(grad[y*1280 + x] < grad[y*1280 + x - 1] || grad[y*1280 + x] < grad[y*1280 + x + 1])
							grad2[y*1280 + x] = 0;
						break;
					case 1:
						if(grad[y*1280 + x] < grad[(y + 1)*1280 + x + 1] || grad[y*1280 + x] < grad[(y - 1)*1280 + x - 1])
							grad2[y*1280 + x] = 0;
						break;
					case 2:
						if(grad[y*1280 + x] < grad[(y + 1)*1280 + x] || grad[y*1280 + x] < grad[(y - 1)*1280 + x])
							grad2[y*1280 + x] = 0;
						break;
					case 3:
						if(grad[y*1280 + x] < grad[(y + 1)*1280 + x - 1] || grad[y*1280 + x] < grad[(y - 1)*1280 + x + 1])
							grad2[y*1280 + x] = 0;
						break;
					}
				}

			//Hysteresis/thresholding
			for(int x=1;x<1280 - 1;x++)
				for(int y=1;y<720 - 1;y++)
					if(grad2[y*1280 + x] < 100 )
						blurred[y*1280 + x] = 0;
					else if(grad2[y*1280 + x] > 200 )
						blurred[y*1280 + x] = (byte) 0xFF;
					else{
						if(
								grad2[y*1280 + x + 1] > 200 ||
								grad2[y*1280 + x - 1] > 200 ||
								grad2[(y + 1)*1280 + x + 1] > 200 ||
								grad2[(y + 1)*1280 + x - 1] > 200 ||
								grad2[(y - 1)*1280 + x + 1] > 200 ||
								grad2[(y - 1)*1280 + x - 1] > 200 ||
								grad2[(y + 1)*1280 + x] > 200 ||
								grad2[(y - 1)*1280 + x] > 200)
							blurred[y*1280 + x] = (byte) 0xFF;
						else
							blurred[y*1280 + x] = 0;
					}
			 */





			//Render the background that is the live camera preview
			
			
			
			//renderBackground(cameraImage);
			renderBackground(null);
			
			
			
			//Get the 3D tag poses from Chilitags
			//ObjectTransform[] tags = chilitags.estimate(cameraImage);

			//Render the tags' reference frames on the image
			//renderTagFrames(tags);
		//}
		
		//Log.i("time",(System.currentTimeMillis() - ms)+"");
	}

	/**
	 * Draws the tag frames as X,Y,Z arrows
	 * 
	 * @param tags The tag transforms estimated by Chilitags3D
	 */
	private void renderTagFrames(ObjectTransform[] tags){

		final double TAG_SIZE = 20.0; //The tag edges are assumed to be 20 mm
		final double[] WORLD_ARROW_ORIGIN = 	{0.0, 			0.0,		0.0,		1.0};
		final double[] WORLD_ARROW_X = 		{TAG_SIZE,		0.0,		0.0,		1.0};
		final double[] WORLD_ARROW_Y = 		{0.0,			TAG_SIZE,	0.0,		1.0};
		final double[] WORLD_ARROW_Z = 		{0.0,			0.0,		TAG_SIZE,	1.0};



		for(ObjectTransform tag : tags){

			/*
			 * Calculate line positions on the screen
			 */

			//Calculate the (unscaled) tag frame points in the screen frame: v_screen = cameraMatrix * tagTransform * v_world
			double[] screen_arrow_origin = GLESLine.multiply(camMat, GLESLine.multiply(tag.transform, WORLD_ARROW_ORIGIN));
			double[] screen_arrow_X = GLESLine.multiply(camMat, GLESLine.multiply(tag.transform, WORLD_ARROW_X));
			double[] screen_arrow_Y = GLESLine.multiply(camMat, GLESLine.multiply(tag.transform, WORLD_ARROW_Y));
			double[] screen_arrow_Z = GLESLine.multiply(camMat, GLESLine.multiply(tag.transform, WORLD_ARROW_Z));

			//Calculate the origin point on the screen frame
			Point screenPoints_origin = new Point();
			screenPoints_origin.x = (int)(xScale * screen_arrow_origin[0] / screen_arrow_origin[3]);
			screenPoints_origin.y = (int)(yScale * screen_arrow_origin[1] / screen_arrow_origin[3]);

			//Calculate the X arrow end on the screen frame
			Point screenPoints_X = new Point();
			screenPoints_X.x = (int)(xScale * screen_arrow_X[0] / screen_arrow_X[3]);
			screenPoints_X.y = (int)(yScale * screen_arrow_X[1] / screen_arrow_X[3]);

			//Calculate the Y arrow end on the screen frame
			Point screenPoints_Y = new Point();
			screenPoints_Y.x = (int)(xScale * screen_arrow_Y[0] / screen_arrow_Y[3]);
			screenPoints_Y.y = (int)(yScale * screen_arrow_Y[1] / screen_arrow_Y[3]);

			//Calculate the Z arrow end on the screen frame
			Point screenPoints_Z = new Point();
			screenPoints_Z.x = (int)(xScale * screen_arrow_Z[0] / screen_arrow_Z[3]);
			screenPoints_Z.y = (int)(yScale * screen_arrow_Z[1] / screen_arrow_Z[3]);

			/*
			 * Draw the arrows
			 */

			//Draw the X arrow
			line.setColor(1.0f, 0.0f, 0.0f, 1.0f);
			line.setVerts(
					((float)screenPoints_origin.x/camController.cameraWidth - 0.5f)*2.0f, -((float)screenPoints_origin.y/camController.cameraHeight - 0.5f)*2.0f, 
					((float)screenPoints_X.x/camController.cameraWidth - 0.5f)*2.0f, -((float)screenPoints_X.y/camController.cameraHeight - 0.5f)*2.0f);
			line.draw();

			//Draw the Y arrow
			line.setColor(0.0f, 1.0f, 0.0f, 1.0f);
			line.setVerts(
					((float)screenPoints_origin.x/camController.cameraWidth - 0.5f)*2.0f, -((float)screenPoints_origin.y/camController.cameraHeight - 0.5f)*2.0f,
					((float)screenPoints_Y.x/camController.cameraWidth - 0.5f)*2.0f, -((float)screenPoints_Y.y/camController.cameraHeight - 0.5f)*2.0f);
			line.draw();

			//Draw the Z arrow
			line.setColor(0.0f, 0.0f, 1.0f, 1.0f);
			line.setVerts(
					((float)screenPoints_origin.x/camController.cameraWidth - 0.5f)*2.0f, -((float)screenPoints_origin.y/camController.cameraHeight - 0.5f)*2.0f, 
					((float)screenPoints_Z.x/camController.cameraWidth - 0.5f)*2.0f, -((float)screenPoints_Z.y/camController.cameraHeight - 0.5f)*2.0f);
			line.draw();
		}
	}

	/**
	 * Draws the image to the background.
	 * 
	 * @param image The YUV-NV21 image to be drawn
	 */
	private void renderBackground(byte[] image){
		
		/*
		 * Because of Java's limitations, we can't reference the middle of an array and 
		 * we must copy the channels in our byte array into buffers before setting them to textures
		 */

		//Copy the Y channel of the image into its buffer, the first (width*height) bytes are the Y channel
		//yBuffer.put(image, 0, camController.cameraWidth*camController.cameraHeight);
		//yBuffer.position(0);

		//Copy the UV channels of the image into their buffer, the following (width*height/2) bytes are the UV channel; the U and V bytes are interspread
		//uvBuffer.put(image, camController.cameraWidth*camController.cameraHeight, camController.cameraWidth*camController.cameraHeight/2);
		//uvBuffer.position(0);

		
		
		//Load the shader and auto uniforms
		yuv2rgbShader.begin();

		//Load the vertex position
		vertices.position(0);
		GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 4*4, vertices);

		//Load the texture coordinate
		vertices.position(2);
		GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 4*4, vertices);

		//Load our vertex array into the shader
		GLES20.glEnableVertexAttribArray(positionHandle);
		GLES20.glEnableVertexAttribArray(texCoordHandle);

		/*
		 * Load the Y texture
		 */

		//Set texture slot 0 as active and bind our texture object to it
		camController.surf.updateTexImage();
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texName[0]);
		GLES20.glUniform1i(yTextureHandle, 0);
		
		/*GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureNames[0]);

		//Y texture is (width*height) in size and each pixel is one byte; by setting GL_LUMINANCE, OpenGL puts this byte into R,G and B components of the texture
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, camController.cameraWidth, camController.cameraHeight, 
				0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);

		//Use linear interpolation when magnifying/minifying the texture to areas larger/smaller than the texture size
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

		//Set the uniform y_texture object in the shader code to the texture at slot 0
		GLES20.glUniform1i(yTextureHandle, 0);*/

		/*
		 * Load the UV texture
		 */
		
/*
		//Set texture slot 1 as active and bind our texture object to it
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureNames[1]);

		//UV texture is (width/2*height/2) in size (downsampled by 2 in both dimensions, each pixel corresponds to 4 pixels of the Y channel) 
		//and each pixel is two bytes. By setting GL_LUMINANCE_ALPHA, OpenGL puts first byte (V) into R,G and B components and of the texture
		//and the second byte (U) into the A component of the texture. That's why we find U and V at A and R respectively in the fragment shader code.
		//Note that we could have also found V at G or B as well. 
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, camController.cameraWidth/2, camController.cameraHeight/2, 
				0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uvBuffer);

		//Use linear interpolation when magnifying/minifying the texture to areas larger/smaller than the texture size
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

		//Set the uniform uv_texture object in the shader code to the texture at slot 1
		GLES20.glUniform1i(uvTextureHandle, 1);*/

		/*
		 * Actual rendering
		 */
		
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indices);
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferName[0]);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indices);
		
		
		

		
		
		
		
		
		long ms = System.currentTimeMillis();
		GLES20.glReadPixels(0, 0, camController.processingWidth, camController.processingHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bbuf);
		Log.i("time",""+(System.currentTimeMillis() - ms));
		
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		
		//Log.i("time",(buf[0] & 0xFF)+"");
		
		//Unload our vertex array
		GLES20.glDisableVertexAttribArray(positionHandle);
		GLES20.glDisableVertexAttribArray(texCoordHandle);
	}
}
