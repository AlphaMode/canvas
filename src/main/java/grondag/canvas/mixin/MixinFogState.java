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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.varia.FogStateExt;
import grondag.canvas.varia.FogStateExtHolder;

//TODO: Enable
@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager$FogState")
public abstract class MixinFogState implements FogStateExt {
	@Shadow public int mode;

	@Override
	public int getMode() {
		return mode;
	}

	@Inject(method = "<init>()V", require = 1, at = @At("RETURN"))
	private void onConstructed(CallbackInfo ci) {
		FogStateExtHolder.INSTANCE = (this);
	}
}
