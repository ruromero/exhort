package com.redhat.exhort.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class PackageRefTest {
    
    @Test
    public void testNamespace() {
       PackageRef ref = new PackageRef("pkg:golang/google.golang.org/genproto#googleapis/api/annotations");
       assertEquals("google.golang.org/genproto", ref.name());

       ref = new PackageRef("pkg:golang/go.opencensus.io@v0.21.0");
       assertEquals("go.opencensus.io", ref.name());

       ref = new PackageRef("pkg:npm/foobar@12.3.1");
       assertEquals("foobar", ref.name());

       ref = new PackageRef("pkg:maven/org.apache.xmlgraphics/batik-anim@1.9.1?packaging=sources");
       assertEquals("org.apache.xmlgraphics:batik-anim", ref.name());
    }

    @Test
    public void testVersion() {
       PackageRef ref = new PackageRef("pkg:golang/google.golang.org/genproto#googleapis/api/annotations");
       assertNull(ref.version());

       ref = new PackageRef("pkg:golang/go.opencensus.io@v0.21.0");
       assertEquals("v0.21.0", ref.version());
    }
}
