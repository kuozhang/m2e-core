/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - refactored lifecycle mapping discovery out
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.WorkingSets;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;


/**
 * Maven Import Wizard
 * 
 * @author Eugene Kuleshov
 */
public class MavenImportWizard extends AbstractMavenProjectWizard implements IImportWizard {

  //private static final Logger LOG = LoggerFactory.getLogger(MavenImportWizard.class);

  private MavenImportWizardPage page;

  private List<String> locations;

  private boolean showLocation = true;

  private boolean basedirRemameRequired = false;

  private boolean initialized = false;

  public MavenImportWizard() {
    setNeedsProgressMonitor(true);
    setWindowTitle(Messages.MavenImportWizard_title);
  }

  public MavenImportWizard(ProjectImportConfiguration importConfiguration, List<String> locations) {
    this();
    this.locations = locations;
    this.showLocation = false;
  }

  public void setBasedirRemameRequired(boolean basedirRemameRequired) {
    this.basedirRemameRequired = basedirRemameRequired;
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);

    initialized = true;

    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=341047
    // prepopulate from workspace selection, 
    // allows convenient import of nested projects by right-click->import on a workspace project or folder
    if(locations == null || locations.isEmpty()) {
      IPath location = SelectionUtil.getSelectedLocation(selection);
      if(location != null) {
        locations = Collections.singletonList(location.toOSString());
      }
    }
  }

  public void addPages() {
    if(!initialized) {
      init(null, null);
    }
    page = new MavenImportWizardPage(importConfiguration);
    page.setLocations(locations);
    page.setShowLocation(showLocation);
    page.setBasedirRemameRequired(basedirRemameRequired);
    addPage(page);

  }

  public boolean performFinish() {
    //mkleint: this sounds wrong.
    if(!page.isPageComplete()) {
      return false;
    }

    Collection<MavenProjectInfo> projects = getProjects();
    List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>(); // ignore any preselected working set
    if(page.shouldCreateWorkingSet() && !projects.isEmpty()) {
      IWorkingSet workingSet = WorkingSets.getOrCreateWorkingSet(page.getWorkingSetName());
      if(!workingSets.contains(workingSet)) {
        workingSets.add(workingSet);
      }
    }

    ImportMavenProjectsJob job = new ImportMavenProjectsJob(projects, workingSets, importConfiguration);
    job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
    job.schedule();

    return true;
  }

  public Collection<MavenProjectInfo> getProjects() {
    return page.getProjects();
  }

}
