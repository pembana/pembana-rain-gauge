/*
 * Copyright 2026 Gunnar Hillert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pembana.raingauge.dashboard;

import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles about data HTTP requests.
 * @author Gunnar Hillert
 */
@Controller
public class AboutDataController {

	private final ObjectProvider<BuildProperties> buildProperties;

	/**
	 * Creates a new {@code AboutDataController}.
	 * @param buildProperties the build properties
	 */
	public AboutDataController(ObjectProvider<BuildProperties> buildProperties) {
		this.buildProperties = buildProperties;
	}

	/**
	 * Renders the data provenance page.
	 * @param model the MVC model to populate
	 * @return the resulting about data
	 */
	@GetMapping("/about-data")
	public String aboutData(Model model) {
		BuildProperties build = this.buildProperties.getIfAvailable();
		String applicationVersion = (build != null) ? build.getVersion() : null;
		Instant buildTime = (build != null) ? build.getTime() : null;
		model.addAttribute("view", new AboutDataView(
				(applicationVersion != null) ? applicationVersion : "development",
				(buildTime != null) ? buildTime.toString() : "local build"));
		return "aboutData";
	}

}
