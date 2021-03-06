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
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;


public class CustomExporterPlugin extends DefaultExporterPlugin {

	private static final Log log = LogFactory.getLog(CustomExporterPlugin.class);
	
    @Override
    public File serialize(CoreSession session, DocumentModel docfrom, String fsPath) throws Exception {
    	
    	log.debug("Exporting from document " + docfrom.getTitle() + " " + docfrom.toString());
    	
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
            if (blob != null && blob.getMimeType().equals("application/pdf")) {
        	// call the method to determine the name of the exported file
                String FileNameToExport = getFileName(blob, docfrom, folder, 1);
                // export the file to the target file system
                try {
                    File target = new File(folder, FileNameToExport);
                    blob.transferTo(target);
                    log.debug("Wrote file " + target.getAbsolutePath());
                }
                catch (FileNotFoundException e) {
                    log.debug("Could not write file");                	
                }
            } else {
                log.debug("No files found");                	
            }
        }
        if (newFolder != null) {
            folder = newFolder;
        }
        return folder;
    }

    @Override
    public DocumentModelList getChildren(CoreSession session, DocumentModel doc, String myPageProvider)
            throws ClientException, Exception {

        PageProviderService ppService = null;
        try {
            ppService = Framework.getService(PageProviderService.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<DocumentModel> pp = null;
        String query = "";

        // if the user gives a query, we build a new Page Provider with the
        // query provided
        if (myPageProvider != null) {
            if (myPageProvider.contains("WHERE")) {
                query = myPageProvider + " AND ecm:parentId = ?";
            } else {
                query = myPageProvider + " where ecm:parentId = ?";
            }
        } else {
            query = "SELECT * FROM Document WHERE ecm:parentId = ? AND ecm:mixinType !='HiddenInNavigation' AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState !='deleted'";
        }
        CoreQueryPageProviderDescriptor desc = new CoreQueryPageProviderDescriptor();
        desc.setPattern(query);

        // set this up with pageSize 100
        pp = (PageProvider<DocumentModel>) ppService.getPageProvider("customPP", desc, null, null, (long)100, null, props,
                new Object[] { doc.getId() });
        
        int countPages = 1;
        // get all the documents of the first page
        DocumentModelList children = new DocumentModelListImpl(pp.getCurrentPage());
        // if there is more than one page, get the children of all the other
        // pages and put into one list
        List<DocumentModel> childrenTemp = new ArrayList<DocumentModel>();
        
        if (pp.getNumberOfPages() > 1) {
            while (countPages < pp.getNumberOfPages()) {
                pp.nextPage();
                childrenTemp = pp.getCurrentPage();
                for (DocumentModel childTemp : childrenTemp) {
                    children.add(childTemp);
                }
                countPages++;
            }
        }
        // return the complete list of documents
        return children;
    }
    
}
