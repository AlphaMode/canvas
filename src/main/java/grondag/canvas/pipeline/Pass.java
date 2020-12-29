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

package grondag.canvas.pipeline;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.PassConfig;

abstract class Pass {
	final PassConfig config;
	boolean isValid = true;
	PipelineFramebuffer fbo;

	Pass(PassConfig config) {
		this.config = config;
		fbo = Pipeline.getFramebuffer(config.framebufferName);

		if (fbo == null) {
			CanvasMod.LOG.warn(String.format("Unable to find framebuffer %s for pass %s.  Pass will be skipped.", config.framebufferName, config.name));
			isValid = false;
		}
	}

	abstract void run(int width, int height);

	abstract void close();

	static Pass create(PassConfig config) {
		if (config.programName.equals(PassConfig.CLEAR_NAME)) {
			return new ClearPass(config);
		} else {
			return new ProgramPass(config);
		}
	}

	static Pass[] create(String logName, PassConfig[] configs) {
		if (configs == null || configs.length == 0) {
			return new Pass[0];
		}

		final ObjectArrayList<Pass> passes = new ObjectArrayList<>();

		for (final PassConfig c : configs) {
			final Pass p = create(c);

			if (p.isValid) {
				passes.add(p);
			}
		}

		return passes.toArray(new Pass[passes.size()]);
	}
}