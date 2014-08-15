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
import org.apache.sqoop.model.MPersistableEntity;
import org.apache.sqoop.shell.core.Constants;
import org.apache.sqoop.shell.utils.ConnectionDynamicFormOptions;
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
public class CloneConnectionFunction extends SqoopFunction {
  @SuppressWarnings("static-access")
  public CloneConnectionFunction() {
    this.addOption(OptionBuilder
      .withDescription(resourceString(Constants.RES_PROMPT_CONN_ID))
      .withLongOpt(Constants.OPT_XID)
      .hasArg()
      .isRequired()
      .create(Constants.OPT_XID_CHAR)
    );
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object executeFunction(CommandLine line, boolean isInteractive) throws IOException {
    return cloneConnection(getLong(line, Constants.OPT_XID), line.getArgList(), isInteractive);
  }

  private Status cloneConnection(Long connectionId, List<String> args, boolean isInteractive) throws IOException {
    printlnResource(Constants.RES_CLONE_CLONING_CONN, connectionId);

    ConsoleReader reader = new ConsoleReader();

    MConnection connection = client.getConnection(connectionId);
    // Remove persistent id as we're making a clone
    connection.setPersistenceId(MPersistableEntity.PERSISTANCE_ID_DEFAULT);

    Status status = Status.FINE;

    ResourceBundle connectorBundle = client.getResourceBundle(connection.getConnectorId());
    ResourceBundle frameworkBundle = client.getFrameworkResourceBundle();

    if (isInteractive) {
      printlnResource(Constants.RES_PROMPT_UPDATE_CONN_METADATA);

      do {
        // Print error introduction if needed
        if( !status.canProceed() ) {
          errorIntroduction();
        }

        // Fill in data from user
        if(!fillConnection(reader, connection, connectorBundle, frameworkBundle)) {
          return null;
        }

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

    printlnResource(Constants.RES_CLONE_CONN_SUCCESSFUL, status.name(), connection.getPersistenceId());

    return status;
  }
}
