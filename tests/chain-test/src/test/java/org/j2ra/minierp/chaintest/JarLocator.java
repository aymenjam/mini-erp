package org.j2ra.minierp.chaintest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.Manifest;

/**
 * Finds the executable jar of an application.
 * 1. On the classpath: Maven puts the jar of a dependency there, and its manifest names the main class (Start-Class).
 * 2. In the repository: services/&lt;name&gt;/target/&lt;name&gt;.jar. An IDE often puts target/classes of a module on the classpath instead of its jar, so 1 finds nothing there.
 * 3. Neither: a clear message.
 */
final class JarLocator {

    static final String GATEWAY = "gateway";
    static final String CUSTOMERS = "customers";
    static final String GATEWAY_START_CLASS = "org.j2ra.minierp.gateway.GatewayApplication";
    static final String CUSTOMERS_START_CLASS = "org.j2ra.minierp.customers.CustomersApplication";

    private JarLocator() {
    }

    static Path gateway() {
        return find(GATEWAY, GATEWAY_START_CLASS, JarLocator.class.getClassLoader(), RepoFiles.moduleDir());
    }

    static Path customers() {
        return find(CUSTOMERS, CUSTOMERS_START_CLASS, JarLocator.class.getClassLoader(), RepoFiles.moduleDir());
    }

    static Path find(String name, String startClass, ClassLoader loader, Path startDir) {
        Path fromClasspath = fromManifests(startClass, loader);
        if (fromClasspath != null) {
            return fromClasspath;
        }
        String relative = "services/" + name + "/target/" + name + ".jar";
        for (Path dir = startDir; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(name + ".jar was not found: it is not on the classpath and " + relative
                + " does not exist (searched from " + startDir + " upwards). Run: ./mvnw -pl tests/chain-test -am verify");
    }

    private static Path fromManifests(String startClass, ClassLoader loader) {
        try {
            Enumeration<URL> manifests = loader.getResources("META-INF/MANIFEST.MF");
            while (manifests.hasMoreElements()) {
                URL url = manifests.nextElement();
                if (!"jar".equals(url.getProtocol())) {
                    continue;
                }
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                connection.setUseCaches(false);
                Manifest manifest = connection.getManifest();
                if (manifest != null && startClass.equals(manifest.getMainAttributes().getValue("Start-Class"))) {
                    return Path.of(connection.getJarFileURL().toURI());
                }
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
