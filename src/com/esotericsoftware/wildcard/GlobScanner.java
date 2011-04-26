
package com.esotericsoftware.wildcard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class GlobScanner {
	private final File rootDir;
	private final List<String> matches = new ArrayList<String>(128);

	public GlobScanner (File rootDir, List<String> includes, List<String> excludes) {
		if (rootDir == null) throw new IllegalArgumentException("rootDir cannot be null.");
		if (!rootDir.exists()) throw new IllegalArgumentException("rootDir does not exist: " + rootDir);
		if (!rootDir.isDirectory()) throw new IllegalArgumentException("rootDir not a directory: " + rootDir);
		try {
			this.rootDir = rootDir.getCanonicalFile();
		} catch (IOException ex) {
			throw new RuntimeException("OS error determining canonical path: " + rootDir, ex);
		}

        new Scanner( buildPatterns( "includes", includes, "**"), buildPatterns( "excludes", excludes, null ) ).scanDir( this.rootDir );
    }

    private class Scanner
    {
        private List<Pattern> includePatterns, excludePatterns;

        public Scanner( List<Pattern> pIncludePatterns, List<Pattern> pExcludePatterns )
        {
            includePatterns = pIncludePatterns;
            excludePatterns = pExcludePatterns;
        }

        public void scanDir (File dir ) {
            scanDir( "", dir );
        }

        private void scanDir (String pParentPath, File dir ) {
            if (!dir.canRead()) return;
        }
    }



    {
		if (!allExcludePatterns.isEmpty()) {
			// For each file, see if any exclude patterns match.
			outerLoop:
			//
			for (Iterator matchIter = matches.iterator(); matchIter.hasNext();) {
				String filePath = (String)matchIter.next();
				List<Pattern> excludePatterns = new ArrayList<Pattern>(allExcludePatterns);
				try {
					// Shortcut for excludes that are "**/XXX", just check file name.
					for (Iterator excludeIter = excludePatterns.iterator(); excludeIter.hasNext();) {
						Pattern exclude = (Pattern)excludeIter.next();
						if (exclude.values.length == 2 && exclude.values[0].equals("**")) {
							exclude.incr();
							String fileName = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1);
							if (exclude.matchesFilePath( fileName )) {
								matchIter.remove();
								continue outerLoop;
							}
							excludeIter.remove();
						}
					}
					// Get the file names after the root dir.
					String[] fileNames = filePath.split("\\" + File.separator);
					for (String fileName : fileNames) {
						for (Iterator excludeIter = excludePatterns.iterator(); excludeIter.hasNext();) {
							Pattern exclude = (Pattern)excludeIter.next();
							if (!exclude.matchesFilePath( fileName )) {
								excludeIter.remove();
								continue;
							}
							exclude.incr(fileName);
							if (exclude.wasFinalMatch()) {
								// Exclude pattern matched.
								matchIter.remove();
								continue outerLoop;
							}
						}
						// Stop processing the file if none of the exclude patterns matched.
						if (excludePatterns.isEmpty()) continue outerLoop;
					}
				} finally {
					for (Pattern exclude : allExcludePatterns)
						exclude.reset();
				}
			}
		}
	}

    private List<Pattern> buildPatterns( String what, List<String> list, String defaultIfEmpty )
    {
        if (list == null) throw new IllegalArgumentException(what + " cannot be null.");
        List<Pattern> patterns = new ArrayList<Pattern>(Math.max(1, list.size()));
        if ( !list.isEmpty() ){
            for (String entry : list)
                patterns.add(new Pattern(entry));
        } else if (defaultIfEmpty != null) {
            patterns.add( new Pattern(defaultIfEmpty) );
        }
        return patterns;
    }

    private void scanDir (File dir, List<Pattern> includes) {
		if (!dir.canRead()) return;

		// See if patterns are specific enough to avoid scanning every file in the directory.
		boolean scanAll = false;
		for (Pattern include : includes) {
			if (include.value.indexOf('*') != -1 || include.value.indexOf('?') != -1) {
				scanAll = true;
				break;
			}
		}

		if (!scanAll) {
			// If not scanning all the files, we know exactly which ones to include.
			List<Pattern> matchingIncludes = new ArrayList<Pattern>(1);
			for (Pattern include : includes) {
				if (matchingIncludes.isEmpty())
					matchingIncludes.add(include);
				else
					matchingIncludes.set(0, include);
				process(dir, include.value, matchingIncludes);
			}
		} else {
			// Scan every file.
			for (String fileName : dir.list()) {
				// Get all include patterns that match.
				List<Pattern> matchingIncludes = new ArrayList<Pattern>(includes.size());
				for (Pattern include : includes)
					if (include.matchesFilePath( fileName )) matchingIncludes.add(include);
				if (matchingIncludes.isEmpty()) continue;
				process(dir, fileName, matchingIncludes);
			}
		}
	}

	private void process (File dir, String fileName, List<Pattern> matchingIncludes) {
		// Increment patterns that need to move to the next token.
		boolean isFinalMatch = false;
		List<Pattern> incrementedPatterns = new ArrayList<Pattern>();
		for (Iterator iter = matchingIncludes.iterator(); iter.hasNext();) {
			Pattern include = (Pattern)iter.next();
			if (include.incr(fileName)) {
				incrementedPatterns.add(include);
				if (include.isExhausted()) iter.remove();
			}
			if (include.wasFinalMatch()) isFinalMatch = true;
		}

		File file = new File(dir, fileName);
		if (isFinalMatch) {
			int length = rootDir.getPath().length();
			if (length > 1)
				matches.add(file.getPath().substring(length + 1));
			else
				matches.add(file.getPath());
		}
		if (!matchingIncludes.isEmpty() && file.isDirectory()) scanDir(file, matchingIncludes);

		// Decrement patterns.
		for (Pattern include : incrementedPatterns)
			include.decr();
	}

	public List<String> matches () {
		return matches;
	}

	public File rootDir () {
		return rootDir;
	}

	public static void main (String[] args) {
		// System.out.println(new Paths("C:\\Java\\ls", "**"));
		List<String> includes = new ArrayList<String>();
		includes.add("website/in*");
		// includes.add("**/lavaserver/**");
		List<String> excludes = new ArrayList<String>();
		// excludes.add("**/*.php");
		// excludes.add("website/**/doc**");
		long start = System.nanoTime();
		List<String> files = new GlobScanner(new File(".."), includes, excludes).matches();
		long end = System.nanoTime();
		System.out.println(files.toString().replaceAll(", ", "\n").replaceAll("[\\[\\]]", ""));
		System.out.println((end - start) / 1000000f);
	}
}
