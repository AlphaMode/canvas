vec4 colorAndLightmap(vec4 fragmentColor,  int layerIndex, vec4 light) {
    return bitValue(v_flags.x, layerIndex) == 0 ? light * fragmentColor : u_emissiveColor * fragmentColor;
}

vec4 applyAo(vec4 baseColor) {
// Don't apply AO for item renders
#if CONTEXT_IS_BLOCK
    #if ENABLE_SMOOTH_LIGHT
        vec4 aotex = texture2D(u_utility, v_hd_ao);
	    float ao = aotex.r;
    #else
	    float ao = v_ao;
	#endif

	#if ENABLE_SUBTLE_AO
        // smooth the transition from 0.4 (should be the minimum) to 1.0
        ao = (ao - 0.4) / 0.6;
        ao = clamp(ao, 0.0, 1.0);
        ao = 1.0 - ao;
        ao = 1.0 - ao * ao;
        ao = 0.4 + ao * 0.6;
	#endif

	return baseColor * vec4(ao, ao, ao, 1.0);
#else
	return baseColor;
#endif
}

float effectModifier() {
    return u_world[WORLD_EFFECT_MODIFIER];
}

#if ENABLE_SMOOTH_LIGHT
vec4 combinedLight() {
    vec4 block = texture2D(u_utility, v_hd_blocklight);
    vec4 sky = texture2D(u_utility, v_hd_skylight);
    // PERF: return directly vs extra math below
    vec2 lightCoord = vec2(block.r, sky.r) * 15.0;
    // TODO: remove
//    lightCoord = vec2(15.0, 15.0);
#if ENABLE_LIGHT_NOISE
    vec4 dither = texture2D(u_dither, gl_FragCoord.xy / 8.0);
    lightCoord += dither.r / 64.0 - (1.0 / 128.0);
#endif
    return texture2D(u_lightmap, (lightCoord + 0.5) / 16.0);
}
#endif

vec4 diffuseColor() {

#if CONTEXT == CONTEXT_BLOCK_SOLID
	float non_mipped_0 = bitValue(v_flags.x, FLAG_UNMIPPED_0) * -4.0;
	vec4 a = texture2D(u_textures, v_texcoord_0, non_mipped_0);

	float cutout = bitValue(v_flags.x, FLAG_CUTOUT_0);
	if(cutout == 1.0 && a.a < 0.5) {
		discard;
	}
#else // alpha
	vec4 a = texture2D(u_textures, v_texcoord_0);
#endif

#if CONTEXT_IS_BLOCK
	#if ENABLE_SMOOTH_LIGHT
	    vec4 light = combinedLight();
    #else
	    vec4 light = texture2D(u_lightmap, v_lightcoord);
    #endif

#elif CONTEXT == CONTEXT_ITEM_GUI
	vec4 light = vec4(1.0, 1.0, 1.0, 1.0);
#else
	vec4 light = texture2D(u_lightmap, v_lightcoord);
#endif

	a *= colorAndLightmap(v_color_0, 0, light);

#if ENABLE_AO_SHADING
    if(bitValue(v_flags.x, FLAG_DISABLE_AO_0) == 0.0) {
    	a = applyAo(a);
    }
#endif

#if ENABLE_DIFFUSE
    if(bitValue(v_flags.x, FLAG_DISABLE_DIFFUSE_0) == 0.0) {
    	a *= vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
    }
#endif

#if LAYER_COUNT > 1
	float non_mipped_1 = bitValue(v_flags.y, FLAG_UNMIPPED_1) * -4.0;
	vec4 b = texture2D(u_textures, v_texcoord_1, non_mipped_1);
	float cutout_1 = bitValue(v_flags.y, FLAG_CUTOUT_1);
	if(cutout_1 != 1.0 || b.a >= 0.5) {
		b *= colorAndLightmap(v_color_1, 1, light);

#if ENABLE_AO_SHADING
		if(bitValue(v_flags.y, FLAG_DISABLE_AO_1) == 0.0) {
		    b = applyAo(b);
		}
#endif

#if ENABLE_DIFFUSE
		if(bitValue(v_flags.y, FLAG_DISABLE_DIFFUSE_1) == 0.0) {
			b *= vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
		}
#endif
		a = vec4(mix(a.rgb, b.rgb, b.a), a.a);
	}
#endif

#if LAYER_COUNT > 2
	float non_mipped_2 = bitValue(v_flags.y, FLAG_UNMIPPED_2) * -4.0;
	vec4 c = texture2D(u_textures, v_texcoord_2, non_mipped_2);
	float cutout_2 = bitValue(v_flags.y, FLAG_CUTOUT_2);
	if(cutout_2 != 1.0 || c.a >= 0.5) {
		c *= colorAndLightmap(v_color_2, 2, light);

#if ENABLE_AO_SHADING
		if(bitValue(v_flags.y, FLAG_DISABLE_AO_2) == 0.0) {
		    c = applyAo(c);
		}
#endif

#if ENABLE_DIFFUSE
		if(bitValue(v_flags.y, FLAG_DISABLE_DIFFUSE_2) == 0.0) {
			c *= vec4(v_diffuse, v_diffuse, v_diffuse, 1.0);
		}
#endif

		a = vec4(mix(a.rgb, c.rgb, c.a), a.a);
	}
#endif

	return a;
}

/**
 * Linear fog.  Is an inverse factor - 0 means full fog.
 */
float linearFogFactor() {
	float fogFactor = (gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale;
	return clamp( fogFactor, 0.0, 1.0 );
}

/**
 * Exponential fog.  Is really an inverse factor - 0 means full fog.
 */
float expFogFactor() {
	float f = gl_FogFragCoord * gl_Fog.density;
    float fogFactor = u_fogMode == FOG_EXP ? exp(f) : exp(f * f);
    return clamp( 1.0 / fogFactor, 0.0, 1.0 );
}

/**
 * Returns either linear or exponential fog depending on current uniform value.
 */
float fogFactor() {
	return u_fogMode == FOG_LINEAR ? linearFogFactor() : expFogFactor();
}

vec4 fog(vec4 diffuseColor) {
#if CONTEXT == CONTEXT_ITEM_GUI
	return diffuseColor;
#else
	return mix(vec4(gl_Fog.color.rgb, diffuseColor.a), diffuseColor, fogFactor());
#endif
}

