package org.polyfrost.oneconfig.loader.relaunch;

import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.function.BiFunction;
import java.util.jar.Manifest;

import com.google.common.io.ByteStreams;

class RelaunchClassLoader extends IsolatedClassLoader {
    static { registerAsParallelCapable(); }

    private final BiFunction<String, byte[], byte[]> transformer;

    public RelaunchClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);

        this.transformer = new LegacyRelaunchTransformer();
    }

    @Override
    protected Class<?> findClassImpl(String name) throws ClassNotFoundException {
        URL jarUrl;
        Manifest jarManifest;
        byte[] bytes;
        try {
            URL url = getResource(name.replace('.', '/') + ".class");
            if (url == null) {
                throw new ClassNotFoundException(name);
            }
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                // usually the case
                JarURLConnection jarConnection = (JarURLConnection) urlConnection;
                jarUrl = jarConnection.getJarFileURL();
                jarManifest = jarConnection.getManifest();
            } else {
                // only in strange setups (like our integration tests), just use some url as fallback
                jarUrl = url;
                jarManifest = null;
            }
            try (InputStream in = urlConnection.getInputStream()) {
                bytes = ByteStreams.toByteArray(in);
            }
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }

        // If the class has a package, define that based on the manifest
        int pkgIndex = name.lastIndexOf('.');
        if (pkgIndex > 0) {
            String pkgName = name.substring(0, pkgIndex);
            if (getPackage(pkgName) == null) {
                try {
                    if (jarManifest != null) {
                        definePackage(pkgName, jarManifest, jarUrl);
                    } else {
                        definePackage(pkgName, null, null, null, null, null, null, jarUrl);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        bytes = transformer.apply(name, bytes);

        return defineClass(name, bytes, 0, bytes.length, new CodeSource(jarUrl, (CodeSigner[]) null));
    }
}
