package org.json.junit;

/*
Copyright (c) 2020 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import static org.junit.Assert.*;

import java.util.*;

import org.json.*;

/**
 * These are helpful utility methods that perform basic comparisons
 * between various objects. In most cases, the comparisons are not
 * order-dependent, or else the order is known.
 */
public class Util {

    /**
     * Compares two JSONArrays for equality.
     * The arrays need not be in the same order.
     * @param jsonArray created by the code to be tested
     * @param expectedJsonArray created specifically for comparing
     */
    public static void compareActualVsExpectedJsonArrays(JSONArray jsonArray,
            JSONArray expectedJsonArray) {
        assertTrue("jsonArray lengths should be equal",
                jsonArray.length() == expectedJsonArray.length());
        for (int i = 0; i < jsonArray.length(); ++i) {
            Object value = jsonArray.get(i);
            Object expectedValue = expectedJsonArray.get(i);
            compareActualVsExpectedObjects(value, expectedValue);
        }
    }

    /**
     * Compares two JSONObjects for equality. The objects need not be
     * in the same order 
     * @param jsonObject created by the code to be tested
     * @param expectedJsonObject created specifically for comparing
     */
    public static void compareActualVsExpectedJsonObjects(
            JSONObject jsonObject, JSONObject expectedJsonObject) {
        assertTrue("jsonObjects should have the same length",
                jsonObject.length() == expectedJsonObject.length());
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            Object expectedValue = expectedJsonObject.get(key);
            compareActualVsExpectedObjects(value, expectedValue);
        }
    }

    /**
     * Compare two objects for equality. Might be JSONArray, JSONObject,
     * or something else.
     * @param value created by the code to be tested
     * @param expectedValue created specifically for comparing
     * @param key key to the jsonObject entry to be compared
     */
    private static void compareActualVsExpectedObjects(Object value,
            Object expectedValue) {
        if (value instanceof JSONObject && expectedValue instanceof JSONObject) {
            // Compare JSONObjects
            JSONObject jsonObject = (JSONObject)value;
            JSONObject expectedJsonObject = (JSONObject)expectedValue;
            compareActualVsExpectedJsonObjects(
                    jsonObject, expectedJsonObject);
        } else if (value instanceof JSONArray && expectedValue instanceof JSONArray) {
            // Compare JSONArrays
            JSONArray jsonArray = (JSONArray)value;
            JSONArray expectedJsonArray = (JSONArray)expectedValue;
            compareActualVsExpectedJsonArrays(
                    jsonArray, expectedJsonArray);
        } else {
            /**
             * Compare all other types using toString(). First, the types must
             * also be equal, unless both are Number type. Certain helper
             * classes (e.g. XML) may create Long instead of Integer for small
             * int values.
             */
            if (!(value instanceof Number && expectedValue instanceof Number)) {
                // Non-Number and non-matching types
                assertEquals("object types should be equal ",
                    expectedValue.getClass().toString(),
                    value.getClass().toString()
                );
            }
            /**
             * Same types or both Numbers, compare by toString()
             */
            assertEquals("values should be equal",
                expectedValue.toString(),
                value.toString()
            );
        }
    }

    /**
     * Stack size, in bytes, of the thread used by {@link #throwableFromLargeStack(Runnable)}.
     */
    private static final long LARGE_STACK_SIZE = 8L * 1024L * 1024L;

    /**
     * Builds a malformed XML document made of the given number of unclosed
     * <code>&lt;a&gt;</code> opening tags.
     * @param depth the number of nested opening tags
     * @return the XML text
     */
    public static String deeplyNestedOpeningTags(int depth) {
        return new String(new char[depth]).replace("\0", "<a>");
    }

    /**
     * Builds a well-formed XML document made of the given number of nested
     * <code>&lt;a&gt;</code> elements.
     * @param depth the number of nested elements
     * @return the XML text
     */
    public static String nestedWellFormedXml(int depth) {
        return deeplyNestedOpeningTags(depth) + new String(new char[depth]).replace("\0", "</a>");
    }

    /**
     * Runs the given body on a dedicated thread that has a large (8 MiB) stack,
     * and returns whatever the body threw.
     * <p>
     * The test JVM may be run with a small stack (for example -Xss512k). On such
     * a stack a recursive parser descending to its default maximum nesting depth
     * may run out of stack before its depth guard is reached, which would make it
     * impossible to tell "rejected by the depth guard" apart from "ran out of
     * stack". Running on a large stack keeps the two outcomes distinct.
     *
     * @param body the code to run
     * @return the Throwable thrown by the body, or <code>null</code> if the body
     *     completed normally
     * @throws IllegalStateException if interrupted while waiting for the body
     */
    public static Throwable throwableFromLargeStack(final Runnable body) {
        final Throwable[] thrown = new Throwable[1];
        Thread worker = new Thread(null, new Runnable() {
            @Override
            public void run() {
                try {
                    body.run();
                } catch (Throwable t) {
                    thrown[0] = t;
                }
            }
        }, "large-stack-test-worker", LARGE_STACK_SIZE);
        worker.start();
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the large stack worker", e);
        }
        return thrown[0];
    }

    /**
     * Runs the given body on a large stack (see {@link #throwableFromLargeStack(Runnable)})
     * and asserts that it threw a JSONException reporting that the given maximum
     * nesting depth was reached.
     * @param maxNestingDepth the maximum nesting depth expected in the message
     * @param body the parsing code to run
     */
    public static void assertMaxNestingDepthReached(int maxNestingDepth, Runnable body) {
        Throwable thrown = throwableFromLargeStack(body);
        if (thrown == null) {
            fail("Expecting a JSONException");
        }
        assertTrue("Expecting a JSONException but got " + thrown,
            thrown instanceof JSONException);
        String message = thrown.getMessage();
        assertTrue("Wrong throwable thrown: not expecting message <" + message + ">",
            message != null && message.startsWith("Maximum nesting depth of " + maxNestingDepth));
    }
}
