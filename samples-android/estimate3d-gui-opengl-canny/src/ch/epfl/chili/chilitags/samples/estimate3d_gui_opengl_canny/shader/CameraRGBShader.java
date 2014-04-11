package ch.epfl.chili.chilitags.samples.estimate3d_gui_opengl_canny.shader;

/**
 * Pass-through shader that can render using an ExternalOES texture
 * 
 * @author Ayberk Özgür
 */
public class CameraRGBShader extends Shader {
	
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
		return "#extension GL_OES_EGL_image_external : require		\n" +
				"#ifdef GL_ES										\n" +
				"precision highp float;								\n" +
				"#endif												\n" +

				"varying vec2 v_texCoord;							\n" +
				"uniform samplerExternalOES texture;				\n" +

				"void main (void){									\n" +
				"	gl_FragColor = texture2D(texture, v_texCoord);	\n" +
				"}													\n";
	}

	@Override
	protected void loadUniforms() { /*No auto uniforms*/ }

	@Override
	protected void getUniformHandles() { /*No auto uniforms*/	} 
}
