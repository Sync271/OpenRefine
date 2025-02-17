/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.exporters;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectManagerStub;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OdsExporterTests extends RefineTest {

    private static final String TEST_PROJECT_NAME = "ods exporter test project";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    ByteArrayOutputStream stream;
    ProjectMetadata projectMetadata;
    Project project;
    Engine engine;
    Properties options;

    // System Under Test
    StreamExporter SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new OdsExporter();
        stream = new ByteArrayOutputStream();
        ProjectManager.singleton = new ProjectManagerStub();
        projectMetadata = new ProjectMetadata();
        project = new Project();
        projectMetadata.setName(TEST_PROJECT_NAME);
        ProjectManager.singleton.registerProject(project, projectMetadata);
        engine = new Engine(project);
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
        stream = null;
        ProjectManager.singleton.deleteProject(project.id);
        project = null;
        engine = null;
        options = null;
    }

    @Test
    public void getContentType() {
        Assert.assertEquals(SUT.getContentType(), "application/vnd.oasis.opendocument.spreadsheet");
    }

    @Test
    public void exportSimpleOds() throws IOException {
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, stream);
        } catch (IOException e) {
            Assert.fail();
        }

        try {
            OdfDocument odfDoc = OdfDocument.loadDocument(new ByteArrayInputStream(stream.toByteArray()));
            List<OdfTable> tables = odfDoc.getTableList();
            Assert.assertEquals(tables.size(), 2); // don't know how the first sheet generate yet
            OdfTable odfTab = tables.get(1);
            Assert.assertEquals(odfTab.getTableName(), "ods exporter test project");
            Assert.assertEquals(odfTab.getRowCount(), 3); // first row is header
            Assert.assertEquals(odfTab.getRowByIndex(1).getCellByIndex(0).getStringValue(), "row0cell0");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    protected void CreateColumns(int noOfColumns) {
        for (int i = 0; i < noOfColumns; i++) {
            try {
                project.columnModel.addColumn(i, new Column(i, "column" + i), true);
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
    }

    protected void CreateGrid(int noOfRows, int noOfColumns) {
        CreateColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell("row" + i + "cell" + j, null));
            }
            project.rows.add(row);
        }
    }

    private void createDateGrid(int noOfRows, int noOfColumns, OffsetDateTime now) {
        CreateColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell(now, null));
            }
            project.rows.add(row);
        }
    }
}
