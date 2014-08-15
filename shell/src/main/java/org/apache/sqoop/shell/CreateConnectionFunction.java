/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sqoop.shell;

import jline.ConsoleReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.sqoop.model.MConnection;
import org.apache.sqoop.shell.core.Constants;
import org.apache.sqoop.shell.utils.ConnectionDynamicFormOptions;
import org.apache.sqoop.shell.utils.FormDisplayer;
import org.apache.sqoop.shell.utils.FormOptions;
import org.apache.sqoop.validation.Status;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import static org.apache.sqoop.shell.ShellEnvironment.*;
import static org.apache.sqoop.shell.utils.FormFiller.*;

/**
 *
 */
@SuppressWarnings("serial")
public class CreateConnectionFunction extends SqoopFunction {
  @SuppressWarnings("static-access")
  public CreateConnectionFunction() {
    this.addOption(OptionBuilder
      .withDescription(resourceString(Constants.RES_CONNECTOR_ID))
      .withLongOpt(Constants.OPT_CID)
      .isRequired()
      .hasArg()
      .create(Constants.OPT_CID_CHAR));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object executeFunction(CommandLine line, boolean isInteractive) throws IOException {
    return createConnection(getLong(line, Constants.OPT_CID), line.getArgList(), isInteractive);
  }

  private Status createConnection(long connectorId, List<String> args, boolean isInteractive) throws IOException {
    printlnResource(Constants.RES_CREATE_CREATING_CONN, connectorId);

    ConsoleReader reader = new ConsoleReader();

    MConnection connection = client.newConnection(connectorId);

    ResourceBundle connectorBundle = client.getResourceBundle(connectorId);
    ResourceBundle frameworkBundle = client.getFrameworkResourceBundle();

    Status status = Status.FINE;

    if (isInteractive) {
      printlnResource(Constants.RES_PROMPT_FILL_CONN_METADATA);

      do {
        // Print error introduction if needed
        if( !status.canProceed() ) {
          errorIntroduction();
        }

        // Fill in data from user
        if(!fillConnection(reader, connection, connectorBundle, frameworkBundle)) {
          return null;
        }

        // Try to create
        status = client.createConnection(connection);
      } while(!status.canProceed());
    } else {
      ConnectionDynamicFormOptions options = new ConnectionDynamicFormOptions();
      options.prepareOptions(connection);
      CommandLine line = FormOptions.parseOptions(options, 0, args, false);
      if (fillConnection(line, connection)) {
        status = client.createConnection(connection);
        if (!status.canProceed()) {
          printConnectionValidationMessages(connection);
          return null;
        }
      } else {
        printConnectionValidationMessages(connection);
        return null;
      }
    }

    FormDisplayer.displayFormWarning(connection);
    printlnResource(Constants.RES_CREATE_CONN_SUCCESSFUL, status.name(), connection.getPersistenceId());

    return status;
  }
}
