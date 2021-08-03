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

package grondag.canvas.render.terrain.cluster;

import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.ArrayVertexCollector.QuadDistanceFunc;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.render.terrain.TerrainRenderConfig;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.render.terrain.cluster.drawlist.RealmDrawList;
import grondag.canvas.render.world.WorldRenderState;

public class ClusteredRegionRenderConfig extends TerrainRenderConfig {
	public static final ClusteredRegionRenderConfig INSTANCE = new ClusteredRegionRenderConfig();

	private ClusteredRegionRenderConfig() {
		super(
			"CLUSTERED",
			"REGION",
			TerrainFormat.TERRAIN_MATERIAL,
			TerrainFormat.TERRAIN_MATERIAL.quadStrideInts,
			true,
			TerrainFormat.TERRAIN_TRANSCODER,
			RealmDrawList::build
		);
	}

	@Override
	public QuadDistanceFunc selectQuadDistanceFunction(ArrayVertexCollector arrayVertexCollector) {
		return arrayVertexCollector.quadDistanceStandard;
	}

	@Override
	public void prepareForDraw(WorldRenderState worldRenderState) {
		// WIP: need a way to set the deadline appropriately based on steady frame rate and time already elapsed.
		// Method must ensure we don't have starvation - task queue can't grow indefinitely.
		ClusterTaskManager.run(System.nanoTime() + 2000000);
	}

	@Override
	public UploadableRegion createUploadableRegion(VertexCollectorList vertexCollectorList, boolean sorted, int bytes, long packedOriginBlockPos, WorldRenderState worldRenderState) {
		return ClusteredDrawableRegion.uploadable(vertexCollectorList, sorted ? worldRenderState.translucentClusterRealm : worldRenderState.solidClusterRealm, bytes, packedOriginBlockPos);
	}
}