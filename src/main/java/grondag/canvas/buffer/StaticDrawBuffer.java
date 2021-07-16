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

package grondag.canvas.buffer;

import java.nio.IntBuffer;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.render.region.DrawableStorage;
import grondag.canvas.varia.GFX;

public class StaticDrawBuffer extends AbstractDrawBuffer implements DrawableStorage {
	TransferBuffer transferBuffer;

	public StaticDrawBuffer(int bytes, CanvasVertexFormat format) {
		super(bytes);
		transferBuffer = TransferBufferAllocator.claim(bytes);
		this.format = format;
	}

	@Override
	public void upload() {
		if (transferBuffer != null) {
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
			transferBuffer = transferBuffer.releaseToBuffer(GFX.GL_ARRAY_BUFFER, GFX.GL_STATIC_DRAW);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		}
	}

	public IntBuffer intBuffer() {
		return transferBuffer.asIntBuffer();
	}

	@Override
	protected void onClose() {
		if (transferBuffer != null) {
			transferBuffer = transferBuffer.release();
		}
	}
}