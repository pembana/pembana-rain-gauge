package com.pembana.raingauge.dashboard;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AboutDataController {

	private final ObjectProvider<BuildProperties> buildProperties;

	public AboutDataController(ObjectProvider<BuildProperties> buildProperties) {
		this.buildProperties = buildProperties;
	}

	@GetMapping("/about-data")
	public String aboutData(Model model) {
		BuildProperties build = this.buildProperties.getIfAvailable();
		model.addAttribute("view", new AboutDataView(
				build == null ? "development" : build.getVersion(),
				build == null || build.getTime() == null ? "local build" : build.getTime().toString()));
		return "aboutData";
	}

}
