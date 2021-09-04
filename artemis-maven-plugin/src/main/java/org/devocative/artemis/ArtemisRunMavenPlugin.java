package org.devocative.artemis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class ArtemisRunMavenPlugin extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(ArtemisRunMavenPlugin.class);

	@Parameter(defaultValue = "artemis")
	private String name;

	@Parameter(defaultValue = "http://localhost:8080")
	private String baseUrl;

	@Parameter(defaultValue = "false")
	private Boolean devMode;

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	// ------------------------------

	@Override
	public void execute() throws MojoExecutionException {
		logger.info("Run Artemis: Name=[{}], DevMode=[{}], BaseUrl=[{}]", name, devMode, baseUrl);

		updateClassLoader();

		ArtemisExecutor.run(name, new Config()
			.setBaseUrl(baseUrl)
			.setDevMode(devMode));
	}

	// ------------------------------

	private void updateClassLoader() throws MojoExecutionException {
		final List<URL> urls = new ArrayList<>();

		try {
			addAll(urls, project.getTestClasspathElements());
			/*
			addAll(urls, project.getRuntimeClasspathElements());
			addAll(urls, project.getCompileClasspathElements());
			*/

			final Class<URLClassLoader> urlClass = URLClassLoader.class;
			final Method method = urlClass.getDeclaredMethod("addURL", URL.class);
			final URLClassLoader urlClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
			method.setAccessible(true);
			for (URL url : urls) {
				method.invoke(urlClassLoader, url);
			}
			logger.debug("ArtemisRunMavenPlugin.updateClassLoader: URLS = {}", urls);
		} catch (Exception e) {
			throw new MojoExecutionException("UpdateClassLoader", e);
		}
	}

	private void addAll(List<URL> urls, List<String> elements) throws MalformedURLException {
		for (String element : elements) {
			URL url = new File(element).toURI().toURL();
			if (!urls.contains(url)) {
				urls.add(url);
			}
		}
	}
}
