
package com.esotericsoftware.scar;

import static com.esotericsoftware.minlog.Log.*;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.regex.*;
import java.util.zip.*;

import javax.tools.*;

import SevenZip.LzmaAlone;

import com.esotericsoftware.wildcard.Paths;

// BOZO - Add javadocs method.

/**
 * Provides utility methods for common Java build tasks.
 */
public class Scar extends Utils {

	/**
	 * List of project names that have been built. {@link #buildDependencies(Project)} will skip any projects with a matching name.
	 */
	static public final List<String> builtProjects = new ArrayList();

	static {
		Paths.setDefaultGlobExcludes("**/.svn/**");
	}

    /**
     * The command line arguments Scar was started with. Empty if Scar was started with no arguments or Scar was not started from
     * the command line.
     */
    public final Arguments args;

    public Scar(Arguments args)
    {
        this.args = (args != null) ? args : new Arguments();
    }

    /**
	 * Loads the specified project with default values and loads any other projects needed for the "include" property.
	 * @param path Path to a YAML project file, or a directory containing a "project.yaml" file.
	 */
	public Project project (String path) throws IOException {
		if (path == null) throw new IllegalArgumentException("path cannot be null.");

		Project defaults = new Project();

		File file = new File(canonical(path));
		if (file.isDirectory()) {
			String name = file.getName();
			defaults.set("name", name);
			defaults.set("target", file.getParent() + "/target/" + name + "/");
		} else {
			String name = file.getParentFile().getName();
			defaults.set("name", name);
			defaults.set("target", file.getParentFile().getParent() + "/target/" + name + "/");
		}
		defaults.set("classpath", "lib|**/*.jar");
		defaults.set("dist", "dist");

		ArrayList source = new ArrayList();
		source.add("src|**/*.java");
		source.add("src/main/java|**/*.java");
		defaults.set("source", source);

		ArrayList resources = new ArrayList();
		resources.add("resources");
		resources.add("src/main/resources");
		defaults.set("resources", resources);

		Project project = project(path, defaults);

		// Remove dependency if a JAR of the same name is on the classpath.
		Paths classpath = project.getPaths("classpath");
		classpath.add(dependencyClasspaths(project, classpath, false, false));
		for (String dependency : project.getList("dependencies")) {
			String dependencyName = project(project.path(dependency)).get("name");
			for (String classpathFile : classpath) {
				String name = fileWithoutExtension(classpathFile);
				int dashIndex = name.lastIndexOf('-');
				if (dashIndex != -1) name = name.substring(0, dashIndex);
				if (name.equals(dependencyName)) {
					if (DEBUG)
						debug("Ignoring " + project + " dependency: " + dependencyName + " (already on classpath: " + classpathFile
							+ ")");
					project.remove("dependencies", dependency);
					break;
				}
			}
		}

		if (TRACE) trace("scar", "Project: " + project + "\n" + project);

		return project;
	}

	/**
	 * Loads the specified project with the specified defaults and loads any other projects needed for the "include" property.
	 * @param path Path to a YAML project file, or a directory containing a "project.yaml" file.
	 */
	public Project project (String path, Project defaults) throws IOException {
		if (path == null) throw new IllegalArgumentException("path cannot be null.");
		if (defaults == null) throw new IllegalArgumentException("defaults cannot be null.");

		Project actualProject = new Project(path);

		Project project = new Project();
		project.replace(defaults);

		File parent = new File(actualProject.getDirectory()).getParentFile();
		while (parent != null) {
			File includeFile = new File(parent, "include.yaml");
			if (includeFile.exists()) {
				try {
					project.replace(project(includeFile.getAbsolutePath(), defaults));
				} catch (RuntimeException ex) {
					throw new RuntimeException("Error loading included project: " + includeFile.getAbsolutePath(), ex);
				}
			}
			parent = parent.getParentFile();
		}

		for (String include : actualProject.getList("include")) {
			try {
				project.replace(project(actualProject.path(include), defaults));
			} catch (RuntimeException ex) {
				throw new RuntimeException("Error loading included project: " + actualProject.path(include), ex);
			}
		}
		project.replace(actualProject);
		return project;
	}

	/**
	 * Deletes the "target" directory and all files and directories under it.
	 */
	public void clean (Project project) {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		if (INFO) info("scar", "Clean: " + project);
		if (TRACE) trace("scar", "Deleting: " + project.path("$target$"));
		new Paths(project.path("$target$")).delete();
	}

	/**
	 * Computes the classpath for the specified project and all its dependency projects, recursively.
	 */
	public Paths classpath (Project project, boolean errorIfDepenenciesNotBuilt) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		Paths classpath = project.getPaths("classpath");
		classpath.add(dependencyClasspaths(project, classpath, true, errorIfDepenenciesNotBuilt));
		return classpath;
	}

	/**
	 * Computes the classpath for all the dependencies of the specified project, recursively.
	 */
	private Paths dependencyClasspaths (Project project, Paths paths, boolean includeDependencyJAR,
		boolean errorIfDepenenciesNotBuilt) throws IOException {
		for (String dependency : project.getList("dependencies")) {
			Project dependencyProject = project(project.path(dependency));
			String dependencyTarget = dependencyProject.path("$target$/");
			if (errorIfDepenenciesNotBuilt && !fileExists(dependencyTarget))
				throw new RuntimeException("Dependency has not been built: " + dependency + "\nAbsolute dependency path: "
					+ canonical(dependency) + "\nMissing dependency target: " + canonical(dependencyTarget));
			if (includeDependencyJAR) paths.glob(dependencyTarget, "*.jar");
			paths.add(classpath(dependencyProject, errorIfDepenenciesNotBuilt));
		}
		return paths;
	}

	/**
	 * Collects the source files using the "source" property and compiles them into a "classes" directory under the target
	 * directory. It uses "classpath" and "dependencies" to find the libraries required to compile the source.
	 * <p>
	 * Note: Each dependency project is not built automatically. Each needs to be built before the dependent project.
	 * @return The path to the "classes" directory.
	 */
	public String compile (Project project) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		Paths classpath = classpath(project, true);
		Paths source = project.getPaths( "source" );

		if (INFO) info("scar", "Compile: " + project);
		if (DEBUG) {
			debug("scar", "Source: " + source.count() + " files");
			debug("scar", "Classpath: " + classpath);
		}

		String classesDir = mkdir(project.path("$target$/classes/"));

		if (source.isEmpty()) {
			if (WARN) warn("No source files found.");
			return classesDir;
		}

		File tempFile = File.createTempFile("scar", "compile");

		ArrayList<String> args = new ArrayList();
		if (TRACE) args.add("-verbose");
		args.add( "-d" );
		args.add(classesDir);
		args.add("-g:source,lines");
		args.add("-target");
		args.add("1.5");
		args.addAll(source.getPaths());
		if (!classpath.isEmpty()) {
			args.add("-classpath");
			args.add(classpath.toString(";"));
		}

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null)
			throw new RuntimeException("No compiler available. Ensure you are running from a 1.6+ JDK, and not a JRE.");
		if (compiler.run(null, null, null, args.toArray(new String[args.size()])) != 0) {
			throw new RuntimeException("Error during compilation of project: " + project + "\nSource: " + source.count()
				+ " files\nClasspath: " + classpath);
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException ex) {
		}
		return classesDir;
	}

	/**
	 * Collects the class files from the "classes" directory and all the resource files using the "resources" property and encodes
	 * them into a JAR file.
	 * 
	 * If the resources don't contain a META-INF/MANIFEST.MF file, one is generated. If the project has a main property, the
	 * generated manifest will include "Main-Class" and "Class-Path" entries to allow the main class to be run with "java -jar".
	 * @return The path to the created JAR file.
	 */
	public String jar (Project project) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		if (INFO) info("scar", "JAR: " + project);

		String jarDir = mkdir(project.path("$target$/jar/"));

		String classesDir = project.path("$target$/classes/");
		new Paths(classesDir, "**/*.class").copyTo(jarDir);
		project.getPaths("resources").copyTo(jarDir);

		String jarFile;
		if (project.has("version"))
			jarFile = project.path("$target$/$name$-$version$.jar");
		else
			jarFile = project.path("$target$/$name$.jar");

		File manifestFile = new File(jarDir, "META-INF/MANIFEST.MF");
		if (!manifestFile.exists()) {
			if (DEBUG) debug("scar", "Generating JAR manifest: " + manifestFile);
			mkdir(manifestFile.getParent());
			Manifest manifest = new Manifest();
			Attributes attributes = manifest.getMainAttributes();
			attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
			if (project.has("main")) {
				if (DEBUG) debug("scar", "Main class: " + project.get("main"));
				attributes.putValue(Attributes.Name.MAIN_CLASS.toString(), project.get("main"));
				StringBuilder buffer = new StringBuilder(512);
				buffer.append(fileName(jarFile));
				buffer.append(" .");
				Paths classpath = classpath(project, true);
				for (String name : classpath.getRelativePaths()) {
					buffer.append(' ');
					buffer.append(name);
				}
				attributes.putValue(Attributes.Name.CLASS_PATH.toString(), buffer.toString());
			}
			FileOutputStream output = new FileOutputStream(manifestFile);
			try {
				manifest.write(output);
			} finally {
				try {
					output.close();
				} catch (Exception ignored) {
				}
			}
		}

		jar(jarFile, new Paths(jarDir));
		return jarFile;
	}

	/**
	 * Encodes the specified paths into a JAR file.
	 * @return The path to the JAR file.
	 */
	public String jar (String jarFile, Paths paths) throws IOException {
		if (jarFile == null) throw new IllegalArgumentException("jarFile cannot be null.");
		if (paths == null) throw new IllegalArgumentException("paths cannot be null.");

		paths = paths.filesOnly();

		if (DEBUG) debug("scar", "Creating JAR (" + paths.count() + " entries): " + jarFile);

		List<String> fullPaths = paths.getPaths();
		List<String> relativePaths = paths.getRelativePaths();
		// Ensure MANIFEST.MF is first.
		int manifestIndex = relativePaths.indexOf("META-INF/MANIFEST.MF");
		if (manifestIndex > 0) {
			relativePaths.remove(manifestIndex);
			relativePaths.add(0, "META-INF/MANIFEST.MF");
			String manifestFullPath = fullPaths.get(manifestIndex);
			fullPaths.remove(manifestIndex);
			fullPaths.add(0, manifestFullPath);
		}
		JarOutputStream output = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)));
		try {
			for (int i = 0, n = fullPaths.size(); i < n; i++) {
				output.putNextEntry(new JarEntry(relativePaths.get(i).replace('\\', '/')));
				FileInputStream input = new FileInputStream(fullPaths.get(i));
				try {
					byte[] buffer = new byte[4096];
					while (true) {
						int length = input.read(buffer);
						if (length == -1) break;
						output.write(buffer, 0, length);
					}
				} finally {
					try {
						input.close();
					} catch (Exception ignored) {
					}
				}
			}
		} finally {
			try {
				output.close();
			} catch (Exception ignored) {
			}
		}
		return jarFile;
	}

	/**
	 * Collects the distribution files using the "dist" property, the project's JAR file, and everything on the project's classpath
	 * (including dependency project classpaths) and places them into a "dist" directory under the "target" directory. This is also
	 * done for depenency projects, recursively. This is everything the application needs to be run from JAR files.
	 * @return The path to the "dist" directory.
	 */
	public String dist (Project project) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		if (INFO) info("scar", "Dist: " + project);

		String distDir = mkdir(project.path("$target$/dist/"));
		classpath(project, true).copyTo(distDir);
		Paths distPaths = project.getPaths("dist");
		dependencyDistPaths(project, distPaths);
		distPaths.copyTo(distDir);
		new Paths(project.path("$target$"), "*.jar").copyTo(distDir);
		return distDir;
	}

	private Paths dependencyDistPaths (Project project, Paths paths) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		for (String dependency : project.getList("dependencies")) {
			Project dependencyProject = project(project.path(dependency));
			String dependencyTarget = dependencyProject.path("$target$/");
			if (!fileExists(dependencyTarget)) throw new RuntimeException("Dependency has not been built: " + dependency);
			paths.glob(dependencyTarget + "dist", "!*/**.jar");
			paths.add(dependencyDistPaths(dependencyProject, paths));
		}
		return paths;
	}

	/**
	 * Removes any code signatures on the specified JAR. Removes any signature files in the META-INF directory and removes any
	 * signature entries from the JAR's manifest.
	 * @return The path to the JAR file.
	 */
	public String unsign (String jarFile) throws IOException {
		if (jarFile == null) throw new IllegalArgumentException("jarFile cannot be null.");

		if (DEBUG) debug("scar", "Removing signature from JAR: " + jarFile);

		File tempFile = File.createTempFile("scar", "removejarsig");
		JarOutputStream jarOutput = null;
		JarInputStream jarInput = null;
		try {
			jarOutput = new JarOutputStream(new FileOutputStream(tempFile));
			jarInput = new JarInputStream(new FileInputStream(jarFile));
			Manifest manifest = jarInput.getManifest();
			if (manifest != null) {
				// Remove manifest file entries.
				manifest.getEntries().clear();
				jarOutput.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
				manifest.write(jarOutput);
			}
			byte[] buffer = new byte[4096];
			while (true) {
				JarEntry entry = jarInput.getNextJarEntry();
				if (entry == null) break;
				String name = entry.getName();
				// Skip signature files.
				if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA")))
					continue;
				jarOutput.putNextEntry(new JarEntry(name));
				while (true) {
					int length = jarInput.read(buffer);
					if (length == -1) break;
					jarOutput.write(buffer, 0, length);
				}
			}
			jarInput.close();
			jarOutput.close();
			copyFile(tempFile.getAbsolutePath(), jarFile);
		} catch (IOException ex) {
			throw new IOException("Error unsigning JAR file: " + jarFile, ex);
		} finally {
			try {
				if (jarInput != null) jarInput.close();
			} catch (Exception ignored) {
			}
			try {
				if (jarOutput != null) jarOutput.close();
			} catch (Exception ignored) {
			}
			tempFile.delete();
		}
		return jarFile;
	}

	/**
	 * Signs the specified JAR.
	 * @return The path to the JAR.
	 */
	public String sign (String jarFile, String keystoreFile, String alias, String password) throws IOException {
		if (jarFile == null) throw new IllegalArgumentException("jarFile cannot be null.");
		if (keystoreFile == null) throw new IllegalArgumentException("keystoreFile cannot be null.");
		if (alias == null) throw new IllegalArgumentException("alias cannot be null.");
		if (password == null) throw new IllegalArgumentException("password cannot be null.");
		if (password.length() < 6) throw new IllegalArgumentException("password must be 6 or more characters.");

		if (DEBUG) debug("scar", "Signing JAR (" + keystoreFile + ", " + alias + ":" + password + "): " + jarFile);

		shell( "jarsigner", "-keystore", keystoreFile, "-storepass", password, "-keypass", password, jarFile, alias );
		return jarFile;
	}

	/**
	 * Encodes the specified file with pack200. The resulting filename is the filename plus ".pack". The file is deleted after
	 * encoding.
	 * @return The path to the encoded file.
	 */
	public String pack200 (String jarFile) throws IOException {
		String packedFile = pack200(jarFile, jarFile + ".pack");
		delete( jarFile );
		return packedFile;
	}

	/**
	 * Encodes the specified file with pack200.
	 * @return The path to the encoded file.
	 */
	public String pack200 (String jarFile, String packedFile) throws IOException {
		if (jarFile == null) throw new IllegalArgumentException("jarFile cannot be null.");
		if (packedFile == null) throw new IllegalArgumentException("packedFile cannot be null.");

		if (DEBUG) debug("scar", "Packing JAR: " + jarFile + " -> " + packedFile);

		shell( "pack200", "--no-gzip", "--segment-limit=-1", "--no-keep-file-order", "--effort=7", "--modification-time=latest", packedFile, jarFile );
		return packedFile;
	}

	/**
	 * Decodes the specified file with pack200. The filename must end in ".pack" and the resulting filename has this stripped. The
	 * encoded file is deleted after decoding.
	 * @return The path to the decoded file.
	 */
	public String unpack200 (String packedFile) throws IOException {
		if (packedFile == null) throw new IllegalArgumentException("packedFile cannot be null.");
		if (!packedFile.endsWith(".pack")) throw new IllegalArgumentException("packedFile must end with .pack: " + packedFile);

		String jarFile = unpack200(packedFile, substring(packedFile, 0, -5));
		delete( packedFile );
		return jarFile;
	}

	/**
	 * Decodes the specified file with pack200.
	 * @return The path to the decoded file.
	 */
	public String unpack200 (String packedFile, String jarFile) throws IOException {
		if (packedFile == null) throw new IllegalArgumentException("packedFile cannot be null.");
		if (jarFile == null) throw new IllegalArgumentException("jarFile cannot be null.");

		if (DEBUG) debug("scar", "Unpacking JAR: " + packedFile + " -> " + jarFile);

		shell( "unpack200", packedFile, jarFile );
		return jarFile;
	}

	/**
	 * Encodes the specified file with GZIP. The resulting filename is the filename plus ".gz". The file is deleted after encoding.
	 * @return The path to the encoded file.
	 */
	public String gzip (String file) throws IOException {
		String gzipFile = gzip(file, file + ".gz");
		delete(file);
		return gzipFile;
	}

	/**
	 * Encodes the specified file with GZIP.
	 * @return The path to the encoded file.
	 */
	public String gzip (String file, String gzipFile) throws IOException {
		if (file == null) throw new IllegalArgumentException("file cannot be null.");
		if (gzipFile == null) throw new IllegalArgumentException("gzipFile cannot be null.");

		if (DEBUG) debug("scar", "GZIP encoding: " + file + " -> " + gzipFile);

		InputStream input = new FileInputStream(file);
		try {
			copyStream(input, new GZIPOutputStream(new FileOutputStream(gzipFile)));
		} finally {
			try {
				input.close();
			} catch (Exception ignored) {
			}
		}
		return gzipFile;
	}

	/**
	 * Decodes the specified GZIP file. The filename must end in ".gz" and the resulting filename has this stripped. The encoded
	 * file is deleted after decoding.
	 * @return The path to the decoded file.
	 */
	public String ungzip (String gzipFile) throws IOException {
		if (gzipFile == null) throw new IllegalArgumentException("gzipFile cannot be null.");
		if (!gzipFile.endsWith(".gz")) throw new IllegalArgumentException("gzipFile must end with .gz: " + gzipFile);

		String file = ungzip(gzipFile, substring(gzipFile, 0, -3));
		delete(gzipFile);
		return file;
	}

	/**
	 * Decodes the specified GZIP file.
	 * @return The path to the decoded file.
	 */
	public String ungzip (String gzipFile, String file) throws IOException {
		if (gzipFile == null) throw new IllegalArgumentException("gzipFile cannot be null.");
		if (file == null) throw new IllegalArgumentException("file cannot be null.");

		if (DEBUG) debug("scar", "GZIP decoding: " + gzipFile + " -> " + file);

		InputStream input = new GZIPInputStream(new FileInputStream(gzipFile));
		try {
			copyStream(input, new FileOutputStream(file));
		} finally {
			try {
				input.close();
			} catch (Exception ignored) {
			}
		}
		return file;
	}

	/**
	 * Encodes the specified files with ZIP.
	 * @return The path to the encoded file.
	 */
	public String zip (Paths paths, String zipFile) throws IOException {
		if (paths == null) throw new IllegalArgumentException("paths cannot be null.");
		if (zipFile == null) throw new IllegalArgumentException("zipFile cannot be null.");

		if (DEBUG) debug("scar", "Creating ZIP (" + paths.count() + " entries): " + zipFile);

		paths.zip(zipFile);
		return zipFile;
	}

	/**
	 * Decodes the specified ZIP file.
	 * @return The path to the output directory.
	 */
	public String unzip (String zipFile, String outputDir) throws IOException {
		if (zipFile == null) throw new IllegalArgumentException("zipFile cannot be null.");
		if (outputDir == null) throw new IllegalArgumentException("outputDir cannot be null.");

		if (DEBUG) debug("scar", "ZIP decoding: " + zipFile + " -> " + outputDir);

		ZipInputStream input = new ZipInputStream(new FileInputStream(zipFile));
		try {
			while (true) {
				ZipEntry entry = input.getNextEntry();
				if (entry == null) break;
				File file = new File(outputDir, entry.getName());
				if (entry.isDirectory()) {
					mkdir(file.getPath());
					continue;
				}
				mkdir(file.getParent());
				FileOutputStream output = new FileOutputStream(file);
				try {
					byte[] buffer = new byte[4096];
					while (true) {
						int length = input.read(buffer);
						if (length == -1) break;
						output.write(buffer, 0, length);
					}
				} finally {
					try {
						output.close();
					} catch (Exception ignored) {
					}
				}
			}
		} finally {
			try {
				input.close();
			} catch (Exception ignored) {
			}
		}
		return outputDir;
	}

	/**
	 * Encodes the specified file with LZMA. The resulting filename is the filename plus ".lzma". The file is deleted after
	 * encoding.
	 * @return The path to the encoded file.
	 */
	public String lzma (String file) throws IOException {
		String lzmaFile = lzma(file, file + ".lzma");
		delete(file);
		return lzmaFile;
	}

	/**
	 * Encodes the specified file with LZMA.
	 * @return The path to the encoded file.
	 */
	public String lzma (String file, String lzmaFile) throws IOException {
		if (file == null) throw new IllegalArgumentException("file cannot be null.");
		if (lzmaFile == null) throw new IllegalArgumentException("lzmaFile cannot be null.");

		if (DEBUG) debug("scar", "LZMA encoding: " + file + " -> " + lzmaFile);

		try {
			LzmaAlone.main(new String[] {"e", file, lzmaFile});
		} catch (Exception ex) {
			throw new IOException("Error lzma compressing file: " + file, ex);
		}
		return lzmaFile;
	}

	/**
	 * Decodes the specified LZMA file. The filename must end in ".lzma" and the resulting filename has this stripped. The encoded
	 * file is deleted after decoding.
	 * @return The path to the decoded file.
	 */
	public String unlzma (String lzmaFile) throws IOException {
		if (lzmaFile == null) throw new IllegalArgumentException("lzmaFile cannot be null.");
		if (!lzmaFile.endsWith(".lzma")) throw new IllegalArgumentException("lzmaFile must end with .lzma: " + lzmaFile);

		String file = unlzma(lzmaFile, substring(lzmaFile, 0, -5));
		delete(lzmaFile);
		return file;
	}

	/**
	 * Decodes the specified LZMA file.
	 * @return The path to the decoded file.
	 */
	public String unlzma (String lzmaFile, String file) throws IOException {
		if (lzmaFile == null) throw new IllegalArgumentException("lzmaFile cannot be null.");
		if (file == null) throw new IllegalArgumentException("file cannot be null.");

		if (DEBUG) debug("scar", "LZMA decoding: " + lzmaFile + " -> " + file);

		try {
			LzmaAlone.main(new String[] {"d", lzmaFile, file});
		} catch (Exception ex) {
			throw new IOException("Error lzma decompressing file: " + file, ex);
		}
		return file;
	}

	/**
	 * Copies all the JAR and JNLP files from the "dist" directory to a "jws" directory under the "target" directory. It then uses
	 * the specified keystore to sign each JAR. If the "pack" parameter is true, it also compresses each JAR using pack200 and
	 * GZIP.
	 */
	public void jws (Project project, boolean pack, String keystoreFile, String alias, String password) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");
		if (keystoreFile == null) throw new IllegalArgumentException("keystoreFile cannot be null.");
		if (alias == null) throw new IllegalArgumentException("alias cannot be null.");
		if (password == null) throw new IllegalArgumentException("password cannot be null.");
		if (password.length() < 6) throw new IllegalArgumentException("password must be 6 or more characters.");

		if (INFO) info("scar", "JWS: " + project);

		String jwsDir = mkdir(project.path("$target$/jws/"));
		String distDir = project.path("$target$/dist/");
		new Paths(distDir, "*.jar", "*.jnlp").copyTo(jwsDir);
		for (String file : new Paths(jwsDir, "*.jar"))
			sign(unpack200(pack200(unsign(file))), keystoreFile, alias, password);
		if (pack) {
			String unpackedDir = mkdir(jwsDir + "unpacked/");
			String packedDir = mkdir(jwsDir + "packed/");
			for (String file : new Paths(jwsDir, "*.jar", "!*native*")) {
				String fileName = fileName(file);
				String unpackedFile = unpackedDir + fileName;
				moveFile(file, unpackedFile);
				String packedFile = packedDir + fileName;
				gzip(pack200(copyFile(unpackedFile, packedFile)));
			}
		}
	}

	/**
	 * Generates ".htaccess" and "type map" VAR files in the "jws" directory. These files allow Apache to serve both pack200/GZIP
	 * JARs and regular JARs, based on capability of the client requesting the JAR.
	 */
	public void jwsHtaccess (Project project) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		if (INFO) info("scar", "JWS htaccess: " + project);

		String jwsDir = mkdir(project.path("$target$/jws/"));
		for (String packedFile : new Paths(jwsDir + "packed", "*.jar.pack.gz")) {
			String packedFileName = fileName(packedFile);
			String jarFileName = substring(packedFileName, 0, -8);
			FileWriter writer = new FileWriter(jwsDir + jarFileName + ".var");
			try {
				writer.write("URI: packed/" + packedFileName + "\n");
				writer.write("Content-Type: x-java-archive\n");
				writer.write("Content-Encoding: pack200-gzip\n");
				writer.write("URI: unpacked/" + jarFileName + "\n");
				writer.write("Content-Type: x-java-archive\n");
			} finally {
				try {
					writer.close();
				} catch (Exception ignored) {
				}
			}
		}
		FileWriter writer = new FileWriter(jwsDir + ".htaccess");
		try {
			writer.write("AddType application/x-java-jnlp-file .jnlp"); // JNLP mime type.
			writer.write("AddType application/x-java-archive .jar\n"); // JAR mime type.
			writer.write("AddHandler application/x-type-map .var\n"); // Enable type maps.
			writer.write("Options +MultiViews\n");
			writer.write("MultiViewsMatch Any\n"); // Apache 2.0 only.
			writer.write("<Files *.pack.gz>\n");
			writer.write("AddEncoding pack200-gzip .jar\n"); // Enable Content-Encoding header for .jar.pack.gz files.
			writer.write("RemoveEncoding .gz\n"); // Prevent mod_gzip from messing with the Content-Encoding response.
			writer.write("</Files>\n");
		} finally {
			try {
				writer.close();
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Generates a JNLP file in the "jws" directory. JARs in the "jws" directory are included in the JNLP. JARs containing "native"
	 * and "win", "mac", "linux", or "solaris" are properly included in the native section of the JNLP. The "main" property is used
	 * for the main class in the JNLP.
	 * @param splashImage Can be null.
	 */
	public void jnlp (Project project, String url, String company, String title, String splashImage) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");
		if (url == null) throw new IllegalArgumentException("url cannot be null.");
		if (!url.startsWith("http")) throw new RuntimeException("Invalid url: " + url);
		if (company == null) throw new IllegalArgumentException("company cannot be null.");
		if (title == null) throw new IllegalArgumentException("title cannot be null.");

		if (DEBUG)
			debug( "scar", "JNLP: " + project + " (" + url + ", " + company + ", " + title + ", " + splashImage + ")" );
		else if (INFO) //
			info("scar", "JNLP: " + project);

		if (!project.has("main")) throw new RuntimeException("Unable to generate JNLP: project has no main class");

		int firstSlash = url.indexOf( "/", 7 );
		int lastSlash = url.lastIndexOf("/");
		if (firstSlash == -1 || lastSlash == -1) throw new RuntimeException("Invalid url: " + url);
		String domain = url.substring(0, firstSlash + 1);
		String path = url.substring(firstSlash + 1, lastSlash + 1);
		String jnlpFile = url.substring(lastSlash + 1);

		String jwsDir = mkdir(project.path("$target$/jws/"));
		FileWriter writer = new FileWriter(jwsDir + jnlpFile);
		try {
			writer.write("<?xml version='1.0' encoding='utf-8'?>\n");
			writer.write("<jnlp spec='1.0+' codebase='" + domain + "' href='" + path + jnlpFile + "'>\n");
			writer.write("<information>\n");
			writer.write("\t<title>" + title + "</title>\n");
			writer.write("\t<vendor>" + company + "</vendor>\n");
			writer.write("\t<homepage href='" + domain + "'/>\n");
			writer.write("\t<description>" + title + "</description>\n");
			writer.write("\t<description kind='short'>" + title + "</description>\n");
			if (splashImage != null) writer.write("\t<icon kind='splash' href='" + path + splashImage + "'/>\n");
			writer.write("</information>\n");
			writer.write("<security>\n");
			writer.write("\t<all-permissions/>\n");
			writer.write("</security>\n");
			writer.write("<resources>\n");
			writer.write("\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n");

			// JAR with main class first.
			String projectJarName;
			if (project.has("version"))
				projectJarName = project.format("$name$-$version$.jar");
			else
				projectJarName = project.format("$name$.jar");
			writer.write("\t<jar href='" + path + projectJarName + "'/>\n");

			// Rest of JARs, except natives.
			for (String file : new Paths(jwsDir, "**/*.jar", "!*native*", "!**/" + projectJarName))
				writer.write("\t<jar href='" + path + fileName(file) + "'/>\n");

			writer.write("</resources>\n");
			Paths nativePaths = new Paths(jwsDir, "*native*win*", "*win*native*");
			if (nativePaths.count() == 1) {
				writer.write("<resources os='Windows'>\n");
				writer.write("\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n");
				writer.write("\t<nativelib href='" + path + nativePaths.getNames().get(0) + "'/>\n");
				writer.write("</resources>\n");
			}
			nativePaths = new Paths(jwsDir, "*native*mac*", "*mac*native*");
			if (nativePaths.count() == 1) {
				writer.write("<resources os='Mac'>\n");
				writer.write("\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n");
				writer.write("\t<nativelib href='" + path + nativePaths.getNames().get(0) + "'/>\n");
				writer.write("</resources>\n");
			}
			nativePaths = new Paths(jwsDir, "*native*linux*", "*linux*native*");
			if (nativePaths.count() == 1) {
				writer.write("<resources os='Linux'>\n");
				writer.write("\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n");
				writer.write("\t<nativelib href='" + path + nativePaths.getNames().get(0) + "'/>\n");
				writer.write("</resources>\n");
			}
			nativePaths = new Paths(jwsDir, "*native*solaris*", "*solaris*native*");
			if (nativePaths.count() == 1) {
				writer.write("<resources os='SunOS'>\n");
				writer.write("\t<j2se href='http://java.sun.com/products/autodl/j2se' version='1.5+' max-heap-size='128m'/>\n");
				writer.write("\t<nativelib href='" + path + nativePaths.getNames().get(0) + "'/>\n");
				writer.write("</resources>\n");
			}
			writer.write("<application-desc main-class='" + project.get("main") + "'/>\n");
			writer.write("</jnlp>");
		} finally {
			try {
				writer.close();
			} catch (Exception ignored) {
			}
		}
	}

	public String lwjglApplet (Project project, String keystoreFile, String alias, String password) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");
		if (keystoreFile == null) throw new IllegalArgumentException("keystoreFile cannot be null.");
		if (alias == null) throw new IllegalArgumentException("alias cannot be null.");
		if (password == null) throw new IllegalArgumentException("password cannot be null.");
		if (password.length() < 6) throw new IllegalArgumentException("password must be 6 or more characters.");

		if (INFO) info("scar", "LWJGL applet: " + project);

		String appletDir = mkdir(project.path("$target$/applet-lwjgl/"));
		String distDir = project.path("$target$/dist/");
		new Paths(distDir, "**/*.jar", "*.html", "*.htm").flatten().copyTo( appletDir );
		for (String jarFile : new Paths(appletDir, "*.jar")) {
			sign(unpack200(pack200(unsign(jarFile))), keystoreFile, alias, password);
			String fileName = fileName(jarFile);
			if (fileName.equals("lwjgl_util_applet.jar") || fileName.equals("lzma.jar")) continue;
			if (fileName.contains("native"))
				lzma(jarFile);
			else
				lzma(pack200(jarFile));
		}

		if (!new Paths(appletDir, "*.html", "*.htm").isEmpty()) return appletDir;
		if (!project.has("main")) {
			if (DEBUG) debug("Unable to generate applet.html: project has no main class");
			return appletDir;
		}
		if (INFO) info("scar", "Generating: applet.html");
		FileWriter writer = new FileWriter(appletDir + "applet.html");
		try {
			writer.write("<html>\n");
			writer.write("<head><title>Applet</title></head>\n");
			writer.write("<body>\n");
			writer
				.write("<applet code='org.lwjgl.util.applet.AppletLoader' archive='lwjgl_util_applet.jar, lzma.jar' codebase='.' width='640' height='480'>\n");
			if (project.has("version")) writer.write("<param name='al_version' value='" + project.getInt("version") + "'>\n");
			writer.write("<param name='al_title' value='" + project + "'>\n");
			writer.write("<param name='al_main' value='" + project.get("main") + "'>\n");
			writer.write("<param name='al_jars' value='");
			int i = 0;
			for (String name : new Paths(appletDir, "*.jar.pack.lzma").getNames()) {
				if (i++ > 0) writer.write(", ");
				writer.write(name);
			}
			writer.write("'>\n");
			Paths nativePaths = new Paths(appletDir, "*native*win*.jar.lzma", "*win*native*.jar.lzma");
			if (nativePaths.count() == 1) writer.write("<param name='al_windows' value='" + nativePaths.getNames().get(0) + "'>\n");
			nativePaths = new Paths(appletDir, "*native*mac*.jar.lzma", "*mac*native*.jar.lzma");
			if (nativePaths.count() == 1) writer.write("<param name='al_mac' value='" + nativePaths.getNames().get(0) + "'>\n");
			nativePaths = new Paths(appletDir, "*native*linux*.jar.lzma", "*linux*native*.jar.lzma");
			if (nativePaths.count() == 1) writer.write("<param name='al_linux' value='" + nativePaths.getNames().get(0) + "'>\n");
			nativePaths = new Paths(appletDir, "*native*solaris*.jar.lzma", "*solaris*native*.jar.lzma");
			if (nativePaths.count() == 1) writer.write("<param name='al_solaris' value='" + nativePaths.getNames().get(0) + "'>\n");
			writer.write("<param name='al_logo' value='appletlogo.png'>\n");
			writer.write("<param name='al_progressbar' value='appletprogress.gif'>\n");
			writer.write("<param name='separate_jvm' value='true'>\n");
			writer
				.write("<param name='java_arguments' value='-Dsun.java2d.noddraw=true -Dsun.awt.noerasebackground=true -Dsun.java2d.d3d=false -Dsun.java2d.opengl=false -Dsun.java2d.pmoffscreen=false'>\n");
			writer.write("</applet>\n");
			writer.write("</body></html>\n");
		} finally {
			try {
				writer.close();
			} catch (Exception ignored) {
			}
		}
		return appletDir;
	}

	/**
	 * Unzips all JARs in the "dist" directory and creates a single JAR containing those files in the "dist/onejar" directory. The
	 * manifest from the project's JAR is used. Putting everything into a single JAR makes it harder to see what libraries are
	 * being used, but makes it easier for end users to distribute the application.
	 * <p>
	 * Note: Files with the same path in different JARs will be overwritten. Files in the project's JAR will never be overwritten,
	 * but may overwrite other files.
	 * @param excludeJARs The names of any JARs to exclude.
	 */
	public void oneJAR (Project project, String... excludeJARs) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		if (INFO) info("scar", "One JAR: " + project);

		String onejarDir = mkdir( project.path( "$target$/onejar/" ) );
		String distDir = project.path("$target$/dist/");
		String projectJarName;
		if (project.has("version"))
			projectJarName = project.format("$name$-$version$.jar");
		else
			projectJarName = project.format("$name$.jar");

		ArrayList<String> processedJARs = new ArrayList();
		outer:
		for (String jarFile : new Paths(distDir, "*.jar", "!" + projectJarName)) {
			String jarName = fileName(jarFile);
			for (String exclude : excludeJARs)
				if (jarName.equals(exclude)) continue outer;
			unzip(jarFile, onejarDir);
			processedJARs.add(jarFile);
		}
		unzip(distDir + projectJarName, onejarDir);

		String onejarFile;
		if (project.has("version"))
			onejarFile = project.path("$target$/dist/onejar/$name$-$version$-all.jar");
		else
			onejarFile = project.path("$target$/dist/onejar/$name$-all.jar");
		mkdir(parent(onejarFile));
		jar( onejarFile, new Paths( onejarDir ) );
	}

	/**
	 * Compiles and executes the specified Java code. The code is compiled as if it were a Java method body.
	 * <p>
	 * Imports statements can be used at the start of the code. These imports are automatically used:<br>
	 * import com.esotericsoftware.scar.Scar;<br>
	 * import com.esotericsoftware.wildcard.Paths;<br>
	 * import com.esotericsoftware.minlog.Log;<br>
	 * import static com.esotericsoftware.scar.Scar.*;<br>
	 * import static com.esotericsoftware.minlog.Log.*;<br>
	 * <p>
	 * Entries can be added to the classpath by using "classpath [url];" statements at the start of the code. These classpath
	 * entries are checked before the classloader that loaded the Scar class is checked. Examples:<br>
	 * classpath someTools.jar;<br>
	 * classpath some/directory/of/class/files;<br>
	 * classpath http://example.com/someTools.jar;<br>
	 * @param parameters These parameters will be available in the scope where the code is executed.
	 */
	public void executeCode (Project project, String code, HashMap<String, Object> parameters) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null)
			throw new RuntimeException("No compiler available. Ensure you are running from a 1.6+ JDK, and not a JRE.");

		try {
			// Wrap code in a class.
			StringBuilder classBuffer = new StringBuilder(2048);
			classBuffer.append("import com.esotericsoftware.scar.*;\n");
			classBuffer.append("import com.esotericsoftware.minlog.Log;\n");
			classBuffer.append("import com.esotericsoftware.wildcard.Paths;\n");
			classBuffer.append("import static com.esotericsoftware.scar.Scar.*;\n");
			classBuffer.append("import static com.esotericsoftware.minlog.Log.*;\n");
			classBuffer.append("public class Generated {\n");
			int templateStartLines = 6;
			classBuffer.append("public void execute (");
			int i = 0;
			for (Entry<String, Object> entry : parameters.entrySet()) {
				if (i++ > 0) classBuffer.append(',');
				classBuffer.append('\n');
				templateStartLines++;
				classBuffer.append(entry.getValue().getClass().getName());
				classBuffer.append(' ');
				classBuffer.append(entry.getKey());
			}
			classBuffer.append("\n) throws Exception {\n");
			templateStartLines += 2;

			// Append code, collecting imports statements and classpath URLs.
			StringBuilder importBuffer = new StringBuilder(512);
			ArrayList<URL> classpathURLs = new ArrayList();
			BufferedReader reader = new BufferedReader(new StringReader(code));
			boolean header = true;
			while (true) {
				String line = reader.readLine();
				if (line == null) break;
				String trimmed = line.trim();
				if (header && trimmed.startsWith("import ") && trimmed.endsWith(";")) {
					importBuffer.append(line);
					importBuffer.append('\n');
				} else if (header && trimmed.startsWith("classpath ") && trimmed.endsWith(";")) {
					String path = substring(line.trim(), 10, -1);
					try {
						classpathURLs.add(new URL(path));
					} catch (MalformedURLException ex) {
						classpathURLs.add(new File(project.path(path)).toURI().toURL());
					}
				} else {
					if (trimmed.length() > 0) header = false;
					classBuffer.append(line);
					classBuffer.append('\n');
				}
			}
			classBuffer.append("}}");

			final String classCode = importBuffer.append(classBuffer).toString();
			if (TRACE) trace("scar", "Executing code:\n" + classCode);

			// Compile class.
			final ByteArrayOutputStream output = new ByteArrayOutputStream(32 * 1024);
			final SimpleJavaFileObject javaObject = new SimpleJavaFileObject(URI.create("Generated.java"), JavaFileObject.Kind.SOURCE) {
                @Override
				public OutputStream openOutputStream () {
					return output;
				}

                @Override
				public CharSequence getCharContent (boolean ignoreEncodingErrors) {
					return classCode;
				}
			};
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector();
			compiler.getTask(null, new ForwardingJavaFileManager(compiler.getStandardFileManager(null, null, null)) {
                @Override
				public JavaFileObject getJavaFileForOutput (Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
					return javaObject;
				}
			}, diagnostics, null, null, Arrays.asList(new JavaFileObject[] {javaObject})).call();

			if (!diagnostics.getDiagnostics().isEmpty()) {
				StringBuilder buffer = new StringBuilder(1024);
				for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
					if (buffer.length() > 0) buffer.append("\n");
					buffer.append("Line ");
					buffer.append(diagnostic.getLineNumber() - templateStartLines);
					buffer.append(": ");
					buffer.append(diagnostic.getMessage(null).replaceAll("^Generated.java:\\d+:\\d* ", ""));
				}
				throw new RuntimeException("Compilation errors:\n" + buffer);
			}

			// Load class.
			Class generatedClass = new URLClassLoader(classpathURLs.toArray(new URL[classpathURLs.size()]),
				Scar.class.getClassLoader()) {
                @Override
				protected synchronized Class<?> loadClass (String name, boolean resolve) throws ClassNotFoundException {
					// Look in this classloader before the parent.
					Class c = findLoadedClass(name);
					if (c == null) {
						try {
							c = findClass(name);
						} catch (ClassNotFoundException e) {
							return super.loadClass(name, resolve);
						}
					}
					if (resolve) resolveClass(c);
					return c;
				}

                @Override
				protected Class<?> findClass (String name) throws ClassNotFoundException {
					if (name.equals("Generated")) {
						byte[] bytes = output.toByteArray();
						return defineClass(name, bytes, 0, bytes.length);
					}
					return super.findClass(name);
				}
			}.loadClass("Generated");

			// Execute.
			Class[] parameterTypes = new Class[parameters.size()];
			Object[] parameterValues = new Object[parameters.size()];
			i = 0;
			for (Object object : parameters.values()) {
				parameterValues[i] = object;
				parameterTypes[i++] = object.getClass();
			}
			generatedClass.getMethod("execute", parameterTypes).invoke(generatedClass.newInstance(), parameterValues);
		} catch (Throwable ex) {
			throw new RuntimeException("Error executing code:\n" + code.trim(), ex);
		}
	}

	/**
	 * Calls {@link #build(Project)} for each dependency project in the specified project.
	 */
	public void buildDependencies (Project project) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		for (String dependency : project.getList("dependencies")) {
			Project dependencyProject = project(project.path(dependency));

			if (builtProjects.contains(dependencyProject.get("name"))) {
				if (DEBUG) debug("scar", "Dependency project already built: " + dependencyProject);
				return;
			}

			String jarFile;
			if (dependencyProject.has("version"))
				jarFile = dependencyProject.path("$target$/$name$-$version$.jar");
			else
				jarFile = dependencyProject.path("$target$/$name$.jar");

			if (DEBUG) debug("Building dependency: " + dependencyProject);
			if (!executeDocument(dependencyProject)) build(dependencyProject);
		}
	}

	/**
	 * Calls {@link #project(String)} and then {@link #build(Project)}.
	 */
	public void build (String path) throws IOException {
		build( project( path ) );
	}

	/**
	 * Executes the buildDependencies, clean, compile, jar, and dist utility metshods.
	 */
	public void build (Project project) throws IOException {
		if (project == null) throw new IllegalArgumentException("project cannot be null.");

		buildDependencies(project);
		clean(project);
		compile( project );
		jar( project );
		dist(project);

		builtProjects.add(project.get("name"));
	}

	/**
	 * Executes Java code in the specified project's document, if any.
	 * @return true if code was executed.
	 */
	public boolean executeDocument (Project project) throws IOException {
		String code = project.getDocument();
		if (code == null || code.trim().isEmpty()) return false;
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("project", project);
		executeCode(project, code, parameters);
		return true;
	}

	public static void main (String[] args) throws Exception {
		Arguments arguments = new Arguments(args);

        setLoggingLevel( arguments );

        Scar scar = new Scar( arguments );
        Project project = scar.project( arguments.get( "file", "." ) );
		if (!scar.executeDocument( project )) scar.build( project );
	}

    private static void setLoggingLevel( Arguments pArguments )
    {
        if ( pArguments.has("trace"))
            TRACE();
        else if ( pArguments.has("debug"))
            DEBUG();
        else if ( pArguments.has("info"))
            INFO();
        else if ( pArguments.has("warn"))
            WARN();
        else if ( pArguments.has("error"))
            ERROR();
    }
}
