package ch.epfl.chili.chilitags.samples.estimate3d_gui_opengl_canny.shader;

import android.opengl.GLES20;

/**
 * Gaussian blur shader.
 * @author Ayberk Özgür
 */
public class GaussianBlurShader extends Shader {

	private final int kernelSize = 11;
	
	/**
	 * The handle to the relative coordinates uniform
	 */
	private int relativeCoordsHandle;

	/**
	 * The handle to the kernel uniform
	 */
	private int kernelHandle;

	/**
	 * The Gaussian blur kernel
	 */
	/*public final float[] kernel = new float[]{
			2.0f/159.0f,  4.0f/159.0f,  5.0f/159.0f,  4.0f/159.0f,  2.0f/159.0f,
			4.0f/159.0f,  9.0f/159.0f, 12.0f/159.0f,  9.0f/159.0f,  4.0f/159.0f,
			5.0f/159.0f, 12.0f/159.0f, 15.0f/159.0f, 12.0f/159.0f,  5.0f/159.0f,
			4.0f/159.0f,  9.0f/159.0f, 12.0f/159.0f,  9.0f/159.0f,  4.0f/159.0f,
			2.0f/159.0f,  4.0f/159.0f,  5.0f/159.0f,  4.0f/159.0f,  2.0f/159.0f};*/
	private final float[] kernel = new float[]{
			0.00125903864488915f, 0.00207580379446220f, 0.00306251764775964f, 0.00404311371302450f, 0.00477637448528137f, 0.00504923798775583f, 0.00477637448528137f, 0.00404311371302450f, 0.00306251764775964f, 0.00207580379446220f, 0.00125903864488915f,
			0.00207580379446220f, 0.00342242186972987f, 0.00504923798775583f, 0.00666596757852286f, 0.00787491021071278f, 0.00832478607124016f, 0.00787491021071278f, 0.00666596757852286f, 0.00504923798775583f, 0.00342242186972987f, 0.00207580379446220f,
			0.00306251764775964f, 0.00504923798775583f, 0.00744934588061432f, 0.00983457271013845f, 0.0116181748772064f, 0.0122818950061712f, 0.0116181748772064f, 0.00983457271013845f, 0.00744934588061432f, 0.00504923798775583f, 0.00306251764775964f,
			0.00404311371302450f, 0.00666596757852286f, 0.00983457271013845f, 0.0129835319692558f, 0.0153382306876543f, 0.0162144692154523f, 0.0153382306876543f, 0.0129835319692558f, 0.00983457271013845f, 0.00666596757852286f, 0.00404311371302450f,
			0.00477637448528137f, 0.00787491021071278f, 0.0116181748772064f, 0.0153382306876543f, 0.0181199785377958f, 0.0191551320467640f, 0.0181199785377958f, 0.0153382306876543f, 0.0116181748772064f, 0.00787491021071278f, 0.00477637448528137f,
			0.00504923798775583f, 0.00832478607124016f, 0.0122818950061712f, 0.0162144692154523f, 0.0191551320467640f, 0.0202494215411802f, 0.0191551320467640f, 0.0162144692154523f, 0.0122818950061712f, 0.00832478607124016f, 0.00504923798775583f,
			0.00477637448528137f, 0.00787491021071278f, 0.0116181748772064f, 0.0153382306876543f, 0.0181199785377958f, 0.0191551320467640f, 0.0181199785377958f, 0.0153382306876543f, 0.0116181748772064f, 0.00787491021071278f, 0.00477637448528137f,
			0.00404311371302450f, 0.00666596757852286f, 0.00983457271013845f, 0.0129835319692558f, 0.0153382306876543f, 0.0162144692154523f, 0.0153382306876543f, 0.0129835319692558f, 0.00983457271013845f, 0.00666596757852286f, 0.00404311371302450f,
			0.00306251764775964f, 0.00504923798775583f, 0.00744934588061432f, 0.00983457271013845f, 0.0116181748772064f, 0.0122818950061712f, 0.0116181748772064f, 0.00983457271013845f, 0.00744934588061432f, 0.00504923798775583f, 0.00306251764775964f,
			0.00207580379446220f, 0.00342242186972987f, 0.00504923798775583f, 0.00666596757852286f, 0.00787491021071278f, 0.00832478607124016f, 0.00787491021071278f, 0.00666596757852286f, 0.00504923798775583f, 0.00342242186972987f, 0.00207580379446220f,
			0.00125903864488915f, 0.00207580379446220f, 0.00306251764775964f, 0.00404311371302450f, 0.00477637448528137f, 0.00504923798775583f, 0.00477637448528137f, 0.00404311371302450f, 0.00306251764775964f, 0.00207580379446220f, 0.00125903864488915f
	};

	/**
	 * The coordinates to apply the kernel to (in texel scale: [0,1]) relative to the current pixel
	 */
	private float[] relativeCoords = new float[kernelSize*kernelSize*2];

	/*= new float[]{
			-2.0f/1280,-2.0f/720,		-1.0f/1280,-2.0f/720,		0.0f/1280,-2.0f/720,		1.0f/1280,-2.0f/720,		2.0f/1280,-2.0f/720,
			-2.0f/1280,-1.0f/720,		-1.0f/1280,-1.0f/720,		0.0f/1280,-1.0f/720,		1.0f/1280,-1.0f/720,		2.0f/1280,-1.0f/720,
			-2.0f/1280,0.0f/720,		-1.0f/1280,0.0f/720,		0.0f/1280,0.0f/720,			1.0f/1280,0.0f/720,			2.0f/1280,0.0f/720,
			-2.0f/1280,1.0f/720,		-1.0f/1280,1.0f/720,		0.0f/1280,1.0f/720,			1.0f/1280,1.0f/720,			2.0f/1280,1.0f/720,
			-2.0f/1280,2.0f/720,		-1.0f/1280,2.0f/720,		0.0f/1280,2.0f/720,			1.0f/1280,2.0f/720,			2.0f/1280,2.0f/720
	};*/

	/**
	 * Creates a new Gaussian blur shader
	 * 
	 * @param imageWidth The image width
	 * @param imageHeight The image height
	 */
	public GaussianBlurShader(int imageWidth, int imageHeight){
		for(int i=0;i<kernelSize;i++)
			for(int j=0;j<kernelSize;j++){
				relativeCoords[2*(j*kernelSize + i)] = (float)(i - kernelSize/2)/imageWidth;
				relativeCoords[2*(j*kernelSize + i) + 1] = (float)(j - kernelSize/2)/imageHeight;
			}
	}
	
	@Override
	protected void loadUniforms() {

		//Load the uniforms
		GLES20.glUniform2fv(relativeCoordsHandle, kernelSize*kernelSize, relativeCoords, 0);
		GLES20.glUniform1fv(kernelHandle, kernelSize*kernelSize, kernel, 0);
	}

	@Override
	protected void getUniformHandles() {
		relativeCoordsHandle = GLES20.glGetUniformLocation(getHandle(), "relative_coords");
		kernelHandle = GLES20.glGetUniformLocation(getHandle(), "kernel");
	}

	@Override
	protected String getVertexShader() {
		return "attribute vec4 a_position;							\n" + 
				"attribute vec2 a_texCoord;							\n" + 
				"varying vec2 v_texCoord;							\n" + 

				"void main(){										\n" + 
				"   gl_Position = a_position;						\n" + 
				"   v_texCoord = a_texCoord;						\n" +
				"}													\n";
	}

	@Override
	protected String getFragmentShader() {
		return "#ifdef GL_ES										\n" +
				"precision highp float;								\n" +
				"#endif												\n" +

				//The Gaussian kernel
				"uniform float kernel["+(kernelSize*kernelSize)+"];	\n" +

				//Our grayscale input texture
				"uniform sampler2D y_texture;						\n" +

				//The relative coordinates to apply the kernel, in texel space
				"uniform vec2 relative_coords["+(kernelSize*kernelSize)+"];					\n" +

				"varying vec2 v_texCoord;							\n" +

				"void main (void){									\n" +
				"	float y = 0.0;									\n" +
				"	int i;											\n" +

				//Apply kernel
				"	for(i=0;i<"+(kernelSize*kernelSize)+";i++)								\n" +
				//We had put the Y values of each pixel to the R,G,B components by GL_LUMINANCE, 
				//that's why we're pulling it from the R component, we could also use G or B
				"		y += kernel[i]*texture2D(y_texture, v_texCoord + relative_coords[i]).r;	\n" + 

				//We finally set the RGB color of our pixel
				"	gl_FragColor = vec4(y, y, y, 1.0);				\n" +
				"}													\n";
	}
}
