/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.standard;

import org.apache.nifi.processors.standard.EvaluateRegularExpression;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.nifi.processor.Relationship;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;

import org.junit.Test;

public class TestEvaluateRegularExpression {

    final String SAMPLE_STRING = "foo\r\nbar1\r\nbar2\r\nbar3\r\nhello\r\nworld\r\n";

    @Test
    public void testProcessor() throws Exception {

        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());

        testRunner.setProperty("regex.result1", "(?s)(.*)");
        testRunner.setProperty("regex.result2", "(?s).*(bar1).*");
        testRunner.setProperty("regex.result3", "(?s).*?(bar\\d).*");	// reluctant gets first
        testRunner.setProperty("regex.result4", "(?s).*?(?:bar\\d).*?(bar\\d).*"); // reluctant w/ repeated pattern gets second
        testRunner.setProperty("regex.result5", "(?s).*(bar\\d).*");	// greedy gets last
        testRunner.setProperty("regex.result6", "(?s)^(.*)$");
        testRunner.setProperty("regex.result7", "(?s)(XXX)");

        testRunner.enqueue(SAMPLE_STRING.getBytes("UTF-8"));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(EvaluateRegularExpression.REL_MATCH, 1);
        final MockFlowFile out = testRunner.getFlowFilesForRelationship(EvaluateRegularExpression.REL_MATCH).get(0);
        out.assertAttributeEquals("regex.result1", SAMPLE_STRING);
        out.assertAttributeEquals("regex.result2", "bar1");
        out.assertAttributeEquals("regex.result3", "bar1");
        out.assertAttributeEquals("regex.result4", "bar2");
        out.assertAttributeEquals("regex.result5", "bar3");
        out.assertAttributeEquals("regex.result6", SAMPLE_STRING);
        out.assertAttributeEquals("regex.result7", null);
    }

    @Test
    public void testProcessorWithDotall() throws Exception {

        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());

        testRunner.setProperty(EvaluateRegularExpression.DOTALL, "true");

        testRunner.setProperty("regex.result1", "(.*)");
        testRunner.setProperty("regex.result2", ".*(bar1).*");
        testRunner.setProperty("regex.result3", ".*?(bar\\d).*");	// reluctant gets first
        testRunner.setProperty("regex.result4", ".*?(?:bar\\d).*?(bar\\d).*"); // reluctant w/ repeated pattern gets second
        testRunner.setProperty("regex.result5", ".*(bar\\d).*");	// greedy gets last
        testRunner.setProperty("regex.result6", "^(.*)$");
        testRunner.setProperty("regex.result7", "^(XXX)$");

        testRunner.enqueue(SAMPLE_STRING.getBytes("UTF-8"));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(EvaluateRegularExpression.REL_MATCH, 1);
        final MockFlowFile out = testRunner.getFlowFilesForRelationship(EvaluateRegularExpression.REL_MATCH).get(0);
        out.assertAttributeEquals("regex.result1", SAMPLE_STRING);
        out.assertAttributeEquals("regex.result2", "bar1");
        out.assertAttributeEquals("regex.result3", "bar1");
        out.assertAttributeEquals("regex.result4", "bar2");
        out.assertAttributeEquals("regex.result5", "bar3");
        out.assertAttributeEquals("regex.result6", SAMPLE_STRING);
        out.assertAttributeEquals("regex.result7", null);

    }

    @Test
    public void testProcessorWithMultiline() throws Exception {

        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());

        testRunner.setProperty(EvaluateRegularExpression.MULTILINE, "true");

        testRunner.setProperty("regex.result1", "(.*)");
        testRunner.setProperty("regex.result2", "(bar1)");
        testRunner.setProperty("regex.result3", ".*?(bar\\d).*");
        testRunner.setProperty("regex.result4", ".*?(?:bar\\d).*?(bar\\d).*");
        testRunner.setProperty("regex.result4b", "bar\\d\\r\\n(bar\\d)");
        testRunner.setProperty("regex.result5", ".*(bar\\d).*");
        testRunner.setProperty("regex.result5b", "(?:bar\\d\\r?\\n)*(bar\\d)");
        testRunner.setProperty("regex.result6", "^(.*)$");
        testRunner.setProperty("regex.result7", "^(XXX)$");

        testRunner.enqueue(SAMPLE_STRING.getBytes("UTF-8"));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(EvaluateRegularExpression.REL_MATCH, 1);
        final MockFlowFile out = testRunner.getFlowFilesForRelationship(EvaluateRegularExpression.REL_MATCH).get(0);
        out.assertAttributeEquals("regex.result1", "foo"); 	// matches everything on the first line
        out.assertAttributeEquals("regex.result2", "bar1");
        out.assertAttributeEquals("regex.result3", "bar1");
        out.assertAttributeEquals("regex.result4", null);	// null because no line has two bar's
        out.assertAttributeEquals("regex.result4b", "bar2"); // included newlines in regex
        out.assertAttributeEquals("regex.result5", "bar1");	//still gets first because no lines with multiple bar's 
        out.assertAttributeEquals("regex.result5b", "bar3");// included newlines in regex
        out.assertAttributeEquals("regex.result6", "foo");	// matches all of first line
        out.assertAttributeEquals("regex.result7", null);	// no match
    }

    @Test
    public void testProcessorWithMultilineAndDotall() throws Exception {

        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());

        testRunner.setProperty(EvaluateRegularExpression.MULTILINE, "true");
        testRunner.setProperty(EvaluateRegularExpression.DOTALL, "true");

        testRunner.setProperty("regex.result1", "(.*)");
        testRunner.setProperty("regex.result2", "(bar1)");
        testRunner.setProperty("regex.result3", ".*?(bar\\d).*");
        testRunner.setProperty("regex.result4", ".*?(?:bar\\d).*?(bar\\d).*");
        testRunner.setProperty("regex.result4b", "bar\\d\\r\\n(bar\\d)");
        testRunner.setProperty("regex.result5", ".*(bar\\d).*");
        testRunner.setProperty("regex.result5b", "(?:bar\\d\\r?\\n)*(bar\\d)");
        testRunner.setProperty("regex.result6", "^(.*)$");
        testRunner.setProperty("regex.result7", "^(XXX)$");

        testRunner.enqueue(SAMPLE_STRING.getBytes("UTF-8"));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(EvaluateRegularExpression.REL_MATCH, 1);
        final MockFlowFile out = testRunner.getFlowFilesForRelationship(EvaluateRegularExpression.REL_MATCH).get(0);

        out.assertAttributeEquals("regex.result1", SAMPLE_STRING);
        out.assertAttributeEquals("regex.result2", "bar1");
        out.assertAttributeEquals("regex.result3", "bar1");
        out.assertAttributeEquals("regex.result4", "bar2");
        out.assertAttributeEquals("regex.result4b", "bar2");
        out.assertAttributeEquals("regex.result5", "bar3");
        out.assertAttributeEquals("regex.result5b", "bar3");
        out.assertAttributeEquals("regex.result6", SAMPLE_STRING);
        out.assertAttributeEquals("regex.result7", null);
    }

    @Test
    public void testProcessorWithNoMatches() throws Exception {

        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());

        testRunner.setProperty(EvaluateRegularExpression.MULTILINE, "true");
        testRunner.setProperty(EvaluateRegularExpression.DOTALL, "true");

        testRunner.setProperty("regex.result2", "(bar1)");
        testRunner.setProperty("regex.result3", ".*?(bar\\d).*");
        testRunner.setProperty("regex.result4", ".*?(?:bar\\d).*?(bar\\d).*");
        testRunner.setProperty("regex.result4b", "bar\\d\\r\\n(bar\\d)");
        testRunner.setProperty("regex.result5", ".*(bar\\d).*");
        testRunner.setProperty("regex.result5b", "(?:bar\\d\\r?\\n)*(bar\\d)");
        testRunner.setProperty("regex.result7", "^(XXX)$");

        testRunner.enqueue("YYY".getBytes("UTF-8"));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(EvaluateRegularExpression.REL_NO_MATCH, 1);
        final MockFlowFile out = testRunner.getFlowFilesForRelationship(EvaluateRegularExpression.REL_NO_MATCH).get(0);

        out.assertAttributeEquals("regex.result1", null);
        out.assertAttributeEquals("regex.result2", null);
        out.assertAttributeEquals("regex.result3", null);
        out.assertAttributeEquals("regex.result4", null);
        out.assertAttributeEquals("regex.result4b", null);
        out.assertAttributeEquals("regex.result5", null);
        out.assertAttributeEquals("regex.result5b", null);
        out.assertAttributeEquals("regex.result6", null);
        out.assertAttributeEquals("regex.result7", null);
    }

    @Test(expected = java.lang.AssertionError.class)
    public void testNoCaptureGroups() throws UnsupportedEncodingException {
        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());
        testRunner.setProperty("regex.result1", ".*");
        testRunner.enqueue(SAMPLE_STRING.getBytes("UTF-8"));
        testRunner.run();
    }

    @Test
    public void testNoFlowFile() throws UnsupportedEncodingException {
        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());
        testRunner.run();
        testRunner.assertAllFlowFilesTransferred(EvaluateRegularExpression.REL_MATCH, 0);

    }

    @Test(expected = java.lang.AssertionError.class)
    public void testTooManyCaptureGroups() throws UnsupportedEncodingException {
        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());
        testRunner.setProperty("regex.result1", "(.)(.)");
        testRunner.enqueue(SAMPLE_STRING.getBytes("UTF-8"));
        testRunner.run();
    }

    @Test
    public void testMatchOutsideBuffer() throws Exception {
        final TestRunner testRunner = TestRunners.newTestRunner(new EvaluateRegularExpression());

        testRunner.setProperty(EvaluateRegularExpression.MAX_BUFFER_SIZE, "3 B");//only read the first 3 chars ("foo")

        testRunner.setProperty("regex.result1", "(foo)");
        testRunner.setProperty("regex.result2", "(world)");

        testRunner.enqueue(SAMPLE_STRING.getBytes("UTF-8"));
        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(EvaluateRegularExpression.REL_MATCH, 1);
        final MockFlowFile out = testRunner.getFlowFilesForRelationship(EvaluateRegularExpression.REL_MATCH).get(0);

        out.assertAttributeEquals("regex.result1", "foo");
        out.assertAttributeEquals("regex.result2", null); 	// null because outsk
    }

    @Test
    public void testGetCompileFlags() {

        final EvaluateRegularExpression processor = new EvaluateRegularExpression();
        TestRunner testRunner;
        int flags;

        // NONE
        testRunner = TestRunners.newTestRunner(processor);
        flags = processor.getCompileFlags(testRunner.getProcessContext());
        assertEquals(0, flags);

        // UNIX_LINES
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.UNIX_LINES, "true");
        assertEquals(Pattern.UNIX_LINES, processor.getCompileFlags(testRunner.getProcessContext()));

        // CASE_INSENSITIVE
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.CASE_INSENSITIVE, "true");
        assertEquals(Pattern.CASE_INSENSITIVE, processor.getCompileFlags(testRunner.getProcessContext()));

        // COMMENTS
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.COMMENTS, "true");
        assertEquals(Pattern.COMMENTS, processor.getCompileFlags(testRunner.getProcessContext()));

        // MULTILINE
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.MULTILINE, "true");
        assertEquals(Pattern.MULTILINE, processor.getCompileFlags(testRunner.getProcessContext()));

        // LITERAL
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.LITERAL, "true");
        assertEquals(Pattern.LITERAL, processor.getCompileFlags(testRunner.getProcessContext()));

        // DOTALL
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.DOTALL, "true");
        assertEquals(Pattern.DOTALL, processor.getCompileFlags(testRunner.getProcessContext()));

        // UNICODE_CASE
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.UNICODE_CASE, "true");
        assertEquals(Pattern.UNICODE_CASE, processor.getCompileFlags(testRunner.getProcessContext()));

        // CANON_EQ
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.CANON_EQ, "true");
        assertEquals(Pattern.CANON_EQ, processor.getCompileFlags(testRunner.getProcessContext()));

        // UNICODE_CHARACTER_CLASS
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.UNICODE_CHARACTER_CLASS, "true");
        assertEquals(Pattern.UNICODE_CHARACTER_CLASS, processor.getCompileFlags(testRunner.getProcessContext()));

        // DOTALL and MULTILINE
        testRunner = TestRunners.newTestRunner(processor);
        testRunner.setProperty(EvaluateRegularExpression.DOTALL, "true");
        testRunner.setProperty(EvaluateRegularExpression.MULTILINE, "true");
        assertEquals(Pattern.DOTALL | Pattern.MULTILINE, processor.getCompileFlags(testRunner.getProcessContext()));
    }

    @Test
    public void testGetRelationShips() throws Exception {

        final EvaluateRegularExpression processor = new EvaluateRegularExpression();
        final TestRunner testRunner = TestRunners.newTestRunner(processor);

//		testRunner.setProperty("regex.result1", "(.*)");
        testRunner.enqueue("foo".getBytes("UTF-8"));
        testRunner.run();

        Set<Relationship> relationships = processor.getRelationships();
        assertTrue(relationships.contains(EvaluateRegularExpression.REL_MATCH));
        assertTrue(relationships.contains(EvaluateRegularExpression.REL_NO_MATCH));
        assertEquals(2, relationships.size());
    }

}
