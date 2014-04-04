package ch.epfl.chili.chilitags.samples.estimate3d_gui_opengl_canny.shader;

/**
 * 5x5 Gaussian blur shader.
 * @author Ayberk Özgür
 */
public class GaussianBlurShader extends Shader {
	
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
				"uniform float kernel[25];							\n" +
				//"const float kernel[25] = float[25](				\n" +
				//"	2.0,  4.0,  5.0,  4.0,  2.0,					\n" +
				//"	4.0,  9.0, 12.0,  9.0,  4.0,					\n" +
				//"	5.0, 12.0, 15.0, 12.0,  5.0,					\n" +
				//"	4.0,  9.0, 12.0,  9.0,  4.0,					\n" +
				//"	2.0,  4.0,  5.0,  4.0,  2.0);					\n" +
				"const float kernel_scale = 159.0;					\n" +
				
				//Our grayscale input texture
				"uniform sampler2D y_texture;						\n" +
				
				//The relative coordinates to apply the kernel, in texel space
				"uniform vec2 relative_coords[25];					\n" +
				
				"varying vec2 v_texCoord;							\n" +

				"void main (void){									\n" +
				"	float y = 0.0;									\n" +
				"	int i;											\n" +
				
				//Apply kernel
				"	for(i=0;i<25;i++)								\n" +
				//We had put the Y values of each pixel to the R,G,B components by GL_LUMINANCE, 
				//that's why we're pulling it from the R component, we could also use G or B
				"		y += kernel[i]*texture2D(y_texture, v_texCoord + relative_coords[i]).r;	\n" + 
				"	y /= kernel_scale;								\n" +

				//We finally set the RGB color of our pixel
				"	gl_FragColor = vec4(y, y, y, 1.0);				\n" +
				"}													\n";
	} 
}
