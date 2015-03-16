/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     annejubert
 */

package org.nuxeo.io.fsexporter;

import java.io.File;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;

public class CustomExporterPlugin extends DefaultExporterPlugin {

    @Override
    public File serialize(CoreSession session, DocumentModel docfrom, String fsPath) throws Exception {
        File folder = null;
        File newFolder = null;
        folder = new File(fsPath);

        // if target directory doesn't exist, create it
        if (!folder.exists()) {
            folder.mkdir();
        }

        if (docfrom.hasFacet("Folderish")) {
            newFolder = new File(fsPath + "/" + docfrom.getName());
            newFolder.mkdir();
        }

        // get all the blobs of the blobholder
        BlobHolder myblobholder = docfrom.getAdapter(BlobHolder.class);
        if (myblobholder != null) {
            Blob blob = myblobholder.getBlob();
        	if (blob != null) {
                // call the method to determine the name of the exported file
                String FileNameToExport = getFileName(blob, docfrom, folder, 1);
                // export the file to the target file system
                File target = new File(folder, FileNameToExport);
                blob.transferTo(target);
            }
        }
        if (newFolder != null) {
            folder = newFolder;
        }
        return folder;
    }

}
