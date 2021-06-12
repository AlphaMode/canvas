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

package grondag.canvas.render;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.terrain.region.RenderRegionStorage;

public class RegionCullingFrustum extends FastFrustum {
	private final RenderRegionStorage regions;

	public boolean enableRegionCulling = false;

	public RegionCullingFrustum(RenderRegionStorage regions) {
		this.regions = regions;
	}

	@Override
	public boolean isVisible(double x0, double y0, double z0, double x1, double y1, double z1) {
		if (super.isVisible(x0, y0, z0, x1, y1, z1)) {
			if (enableRegionCulling) {
				final int rx0 = MathHelper.floor(x0) & 0xFFFFFFF0;
				final int ry0 = MathHelper.floor(y0) & 0xFFFFFFF0;
				final int rz0 = MathHelper.floor(z0) & 0xFFFFFFF0;
				final int rx1 = MathHelper.floor(x1) & 0xFFFFFFF0;
				final int ry1 = MathHelper.floor(y1) & 0xFFFFFFF0;
				final int rz1 = MathHelper.floor(z1) & 0xFFFFFFF0;

				int flags = rx0 == rz1 ? 0 : 1;
				if (ry0 != ry1) flags |= 2;
				if (rz0 != rz1) flags |= 4;

				final RenderRegionStorage regions = this.regions;

				switch (flags) {
					case 0b000:
						return regions.wasSeen(rx0, ry0, rz0);

					case 0b001:
						return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx1, ry0, rz0);

					case 0b010:
						return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx0, ry1, rz0);

					case 0b011:
						return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx1, ry0, rz0)
								|| regions.wasSeen(rx0, ry1, rz0) || regions.wasSeen(rx1, ry1, rz0);

					case 0b100:
						return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx0, ry0, rz1);

					case 0b101:
						return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx1, ry0, rz0)
								|| regions.wasSeen(rx0, ry0, rz1) || regions.wasSeen(rx1, ry0, rz1);

					case 0b110:
						return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx0, ry1, rz0)
								|| regions.wasSeen(rx0, ry0, rz1) || regions.wasSeen(rx0, ry1, rz1);

					case 0b111:
						return regions.wasSeen(rx0, ry0, rz0) || regions.wasSeen(rx1, ry0, rz0)
								|| regions.wasSeen(rx0, ry1, rz0) || regions.wasSeen(rx1, ry1, rz0)
								|| regions.wasSeen(rx0, ry0, rz1) || regions.wasSeen(rx1, ry0, rz1)
								|| regions.wasSeen(rx0, ry1, rz1) || regions.wasSeen(rx1, ry1, rz1);
					default:
						assert false : "Pathological spatial test result in RegionCullingFrustum";
						return true;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}
}