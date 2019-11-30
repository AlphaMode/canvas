/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.mixin;

import java.util.EnumSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import grondag.canvas.chunk.ChunkRenderDataExt;
import grondag.canvas.chunk.ChunkRendererDispatcherExt;
import grondag.canvas.chunk.ChunkRendererListExt;
import grondag.canvas.chunk.occlusion.ChunkOcclusionBuilderAccessHelper;
import grondag.canvas.chunk.occlusion.ChunkOcclusionMap;
import grondag.canvas.material.ShaderManager;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
	@Shadow private ChunkRendererList chunkRendererList;
	@Shadow private ChunkRenderDispatcher chunkRenderDispatcher;

	@Inject(method = "setUpTerrain", at = @At("HEAD"), cancellable = false, require = 1)
	private void onPrepareTerrain(Camera camera, VisibleRegion region, int int_1, boolean boolean_1, CallbackInfo ci) {
		ShaderManager.INSTANCE.prepareForFrame(camera);
	}

	//    private static final ConcurrentPerformanceCounter counter = new ConcurrentPerformanceCounter();
	//    private static long start;
	//    private static void end() {
	//        counter.endRun(start);
	//        counter.addCount(1);
	//        if(counter.runCount() >= 200) {
	//            CanvasMod.LOG.info(counter.stats());
	//            counter.clearStats();
	//        }
	//    }

	/**
	 * Use pre-computed visibility stored during render chunk rebuild vs computing on fly each time.
	 * Seems to be about 50 to 100X faster but doesn't matter as much as it did in 1.12.
	 */
	@SuppressWarnings("unchecked")
	@Inject(method = "getOpenChunkFaces", at = @At("HEAD"), cancellable = true, require = 1)
	private void onGetOpenChunkFaces(BlockPos pos, CallbackInfoReturnable<Set<Direction>> ci) {
		//        start = counter.startRun();
		final ChunkRenderer renderChunk = ((ChunkRendererDispatcherExt)chunkRenderDispatcher).canvas_chunkRenderer(pos);
		if(renderChunk != null)
		{
			final Object visData = ((ChunkRenderDataExt)renderChunk.data).canvas_chunkVisibility().canvas_visibilityData();
			// unbuilt chunks won't have extended info
			if(visData != null) {
				// note we return copies because result may be modified
				final EnumSet<Direction> result = EnumSet.noneOf(Direction.class);
				if (visData instanceof Set) {
					result.addAll((Set<Direction>)visData);
				} else {
					result.addAll(((ChunkOcclusionMap) visData).getFaceSet(ChunkOcclusionBuilderAccessHelper.PACK_FUNCTION.applyAsInt(pos)));
				}
				//                end();
				ci.setReturnValue(result);
			}
		}
	}

	//    @Inject(method = "getOpenChunkFaces", at = @At("TAIL"), cancellable = false, require = 1)
	//    private void afterGetOpenChunkFaces(BlockPos pos, CallbackInfoReturnable<Set<Direction>> ci) {
	//        end();
	//    }

	@Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, require = 1)
	private void onRenderLayer(BlockRenderLayer layer, Camera camera, CallbackInfoReturnable<Integer> ci) {
		switch (layer) {

		case CUTOUT:
		case CUTOUT_MIPPED:
			ci.setReturnValue(0);
			break;

		case SOLID:
			// Must happen after camera transform is set up and before chunk render
			((ChunkRendererListExt)chunkRendererList).canvas_prepareForFrame();
			break;

		case TRANSLUCENT:
		default:
			// nothing
			break;
		}
	}
}
