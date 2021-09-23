/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderBuffers;

import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.mixinterface.RenderBuffersExt;

@Mixin(RenderBuffers.class)
public class MixinRenderBuffers implements RenderBuffersExt {
	@Shadow private BufferSource bufferSource;

	private BufferSource activeBufferSource;

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void onNew(CallbackInfo ci) {
		activeBufferSource = bufferSource;
	}

	/**
	 * @author grondag
	 * @reason simple and reliable
	 */
	@Overwrite
	public MultiBufferSource.BufferSource bufferSource() {
		return activeBufferSource;
	}

	@Override
	public void canvas_setEntityConsumers(CanvasImmediate consumers) {
		activeBufferSource = consumers == null ? bufferSource : consumers;
	}
}