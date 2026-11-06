package io.github.gaiusred.sourcepots.source;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OptionalApiLoadingTest {
    @Test
    void coreStorageLinksWithoutOptionalApis() {
        ClassLoader isolated = new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("mekanism.") || name.startsWith("alexthw.")) throw new ClassNotFoundException(name);
                if (!name.startsWith("io.github.gaiusred.sourcepots.")) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        String resource = name.replace('.', '/') + ".class";
                        try (var input = getParent().getResourceAsStream(resource)) {
                            if (input == null) throw new ClassNotFoundException(name);
                            byte[] bytes = input.readAllBytes();
                            loaded = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException failure) { throw new ClassNotFoundException(name, failure); }
                    }
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        };
        assertDoesNotThrow(() -> {
            Class<?> storage = isolated.loadClass("io.github.gaiusred.sourcepots.source.PotSourceAccess");
            storage.getDeclaredFields();
            storage.getDeclaredMethods();
        });
    }
}
