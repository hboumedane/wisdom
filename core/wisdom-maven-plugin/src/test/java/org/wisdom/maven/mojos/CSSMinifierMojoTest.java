/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.maven.mojos;

import com.google.common.collect.ImmutableList;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CSSMinifierMojoTest {

    @Test
    public void testAccept() throws Exception {
        CSSMinifierMojo mojo = new CSSMinifierMojo();
        mojo.basedir = new File("junk");
        File file = new File(mojo.basedir, "src/main/resources/assets/foo.css");
        assertThat(mojo.accept(file)).isTrue();
        file = new File(mojo.basedir, "src/main/resources/assets/foo/foo.css");
        assertThat(mojo.accept(file)).isTrue();
        file = new File(mojo.basedir, "src/main/resources/assets/foo.js");
        assertThat(mojo.accept(file)).isFalse();
        // Not a valid resources
        file = new File(mojo.basedir, "src/main/foo.css");
        assertThat(mojo.accept(file)).isFalse();

        file = new File(mojo.basedir, "src/main/assets/foo.css");
        assertThat(mojo.accept(file)).isTrue();
        file = new File(mojo.basedir, "src/main/assets/foo/foo.css");
        assertThat(mojo.accept(file)).isTrue();
        file = new File(mojo.basedir, "src/main/assets/foo.js");
        assertThat(mojo.accept(file)).isFalse();
        // Not a valid resources
        file = new File(mojo.basedir, "src/main/foo.css");
        assertThat(mojo.accept(file)).isFalse();

    }

    @Test
    public void testIsNotMinified() throws Exception {
        CSSMinifierMojo mojo = new CSSMinifierMojo();
        mojo.cssMinifierSuffix = "-minified";

        File file = new File("foo.css");
        assertThat(mojo.isNotMinified(file)).isTrue();
        file = new File("foo-min.css");

        assertThat(mojo.isNotMinified(file)).isFalse();
        file = new File("foo.min.css");
        assertThat(mojo.isNotMinified(file)).isFalse();

        file = new File("foo-minified.css");
        assertThat(mojo.isNotMinified(file)).isFalse();
    }

    @Test
    public void testGetMinifiedFile() {
        CSSMinifierMojo mojo = new CSSMinifierMojo();
        mojo.cssMinifierSuffix = "-minified";
        mojo.basedir = new File("target/junk/root");
        mojo.buildDirectory = new File(mojo.basedir, "target");
        File file = new File(mojo.basedir,
                "src/main/resources/assets/foo.css");
        assertThat(mojo.getMinifiedFile(file).getAbsolutePath()).isEqualTo(new File(mojo.buildDirectory,
                "classes/assets/foo-minified.css").getAbsolutePath());

        file = new File(mojo.basedir,
                "src/main/assets/foo.css");
        assertThat(mojo.getMinifiedFile(file).getAbsolutePath()).isEqualTo(new File(mojo.buildDirectory,
                "wisdom/assets/foo-minified.css").getAbsolutePath());
    }

    @Test
    public void testGetDefaultOutputFile() {
        CSSMinifierMojo mojo = new CSSMinifierMojo();
        mojo.cssMinifierSuffix = "-min";
        mojo.basedir = new File("target/junk/root");
        mojo.buildDirectory = new File(mojo.basedir, "target");
        Stylesheets stylesheets = new Stylesheets();
        mojo.stylesheets = stylesheets;
        MavenProject project = mock(MavenProject.class);
        when(project.getArtifactId()).thenReturn("my-artifact");
        mojo.project = project;

        Aggregation aggregation = new Aggregation();
        aggregation.setMinification(true);
        stylesheets.setAggregations(ImmutableList.of(aggregation));

        assertThat(mojo.getDefaultOutputFile(aggregation).getAbsolutePath()).isEqualTo(new File(mojo.buildDirectory,
                "classes/assets/my-artifact-min.css").getAbsolutePath());
    }
}