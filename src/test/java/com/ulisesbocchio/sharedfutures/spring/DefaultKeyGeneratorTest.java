package com.ulisesbocchio.sharedfutures.spring;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @author ulisesbocchio
 */
public class DefaultKeyGeneratorTest {

    @Test
    public void testKeyNoParams() throws NoSuchMethodException {
        DefaultKeyGenerator generator = new DefaultKeyGenerator();
        Method method = String.class.getMethod("toLowerCase");
        String expectedKey = "java.lang.String#toLowerCase()";
        Assert.assertEquals(DigestUtils.md5Hex(expectedKey), generator.generate("", method));
    }

    @Test
    public void testSimpleKey() throws NoSuchMethodException {
        DefaultKeyGenerator generator = new DefaultKeyGenerator();
        Method method = String.class.getMethod("endsWith", String.class);
        String expectedKey = "java.lang.String#endsWith(blah!)";
        Assert.assertEquals(DigestUtils.md5Hex(expectedKey), generator.generate("", method, "blah!"));
    }

    @Test
    public void testSharedFutureKey() throws NoSuchMethodException {
        DefaultKeyGenerator generator = new DefaultKeyGenerator();
        Method method = this.getClass().getMethod("someMethod", SharedFutureKey.class);
        String expectedKey = this.getClass().getName() + "#someMethod(blah!)";
        Assert.assertEquals(DigestUtils.md5Hex(expectedKey), generator.generate(this, method, (SharedFutureKey)() -> "blah!"));
    }

    @Test
     public void testPrimitives() throws NoSuchMethodException {
        DefaultKeyGenerator generator = new DefaultKeyGenerator();
        Method method = this.getClass().getMethod("primitives", long.class, Long.class, int.class, boolean.class, Boolean.class);
        String expectedKey = this.getClass().getName() + "#primitives(1,2,3,true,false)";
        Assert.assertEquals(DigestUtils.md5Hex(expectedKey), generator.generate(this, method, 1L, 2L, 3, true, Boolean.FALSE));
    }

    @Test
    public void testCollection() throws NoSuchMethodException {
        DefaultKeyGenerator generator = new DefaultKeyGenerator();
        Method method = this.getClass().getMethod("collection", List.class);
        String expectedKey = this.getClass().getName() + "#collection([one,two,three])";
        Assert.assertEquals(DigestUtils.md5Hex(expectedKey), generator.generate(this, method, ImmutableList.of("one", "two", "three")));
    }

    @Test
    public void testCollectionOfSharedFutureKey() throws NoSuchMethodException {
        DefaultKeyGenerator generator = new DefaultKeyGenerator();
        Method method = this.getClass().getMethod("keyCollection", List.class);
        String expectedKey = this.getClass().getName() + "#keyCollection([one,two,three])";
        Assert.assertEquals(DigestUtils.md5Hex(expectedKey), generator.generate(this, method,
                ImmutableList.<SharedFutureKey>of(() -> "one", () -> "two", () -> "three")));
    }

    @Test
    public void testMap() throws NoSuchMethodException {
        DefaultKeyGenerator generator = new DefaultKeyGenerator();
        Method method = this.getClass().getMethod("map", Map.class);
        String expectedKey = this.getClass().getName() + "#map([1=one,2=two,3=three])";
        Assert.assertEquals(DigestUtils.md5Hex(expectedKey), generator.generate(this, method,
                ImmutableMap.of(1, "one", 2, "two", 3, "three")));
    }

    @Test
    public void testMapOfSharedKeys() throws NoSuchMethodException {
        DefaultKeyGenerator generator = new DefaultKeyGenerator();
        Method method = this.getClass().getMethod("keyMap", Map.class);
        String expectedKey = this.getClass().getName() + "#keyMap([1=one,2=two,3=three])";
        Assert.assertEquals(DigestUtils.md5Hex(expectedKey), generator.generate(this, method,
                ImmutableMap.<SharedFutureKey, String>of(() -> "1", "one", () -> "2", "two", () -> "3", "three")));
    }

    public void primitives(long a, Long b, int c, boolean d, Boolean e) {

    }

    public void collection(List<String> list) {

    }

    public void keyCollection(List<SharedFutureKey> list) {

    }

    public void someMethod(SharedFutureKey key) {
    }

    public void map(Map<Integer, String> map) {
    }

    public void keyMap(Map<SharedFutureKey, String> map) {
    }
}
