/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.idea.svn.branchConfig;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.svn.SvnConfiguration;
import org.jetbrains.idea.svn.SvnVcs;
import org.jetbrains.idea.svn.api.Depth;
import org.jetbrains.idea.svn.browse.DirectoryEntry;
import org.jetbrains.idea.svn.browse.DirectoryEntryConsumer;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Konstantin Kolosovsky.
 */
public class BranchesLoader implements Runnable {
  @NotNull private final Project myProject;
  @NotNull private final NewRootBunch myBunch;
  @NotNull private final VirtualFile myRoot;
  @Nullable private final Runnable myCallback;
  @NotNull private final String myUrl;
  @NotNull private final InfoReliability myInfoReliability;
  private final boolean myPassive;

  public BranchesLoader(@NotNull Project project,
                        @NotNull NewRootBunch bunch,
                        @NotNull String url,
                        @NotNull InfoReliability infoReliability,
                        @NotNull VirtualFile root,
                        @Nullable Runnable callback,
                        boolean passive) {
    myProject = project;
    myBunch = bunch;
    myUrl = url;
    myInfoReliability = infoReliability;
    myRoot = root;
    myCallback = callback;
    myPassive = passive;
  }

  public void run() {
    try {
      List<SvnBranchItem> branches = loadBranches();
      myBunch.updateBranches(myRoot, myUrl, new InfoStorage<List<SvnBranchItem>>(branches, myInfoReliability));
    }
    catch (VcsException e) {
      showError(e);
    }
    catch (SVNException e) {
      showError(e);
    }
    finally {
      if (myCallback != null) {
        myCallback.run();
      }
    }
  }

  @NotNull
  public List<SvnBranchItem> loadBranches() throws SVNException, VcsException {
    final SvnConfiguration configuration = SvnConfiguration.getInstance(myProject);
    final SvnVcs vcs = SvnVcs.getInstance(myProject);
    SVNURL branchesUrl = SVNURL.parseURIEncoded(myUrl);
    List<SvnBranchItem> result = new LinkedList<SvnBranchItem>();
    SvnTarget target = SvnTarget.fromURL(branchesUrl);

    if (!myPassive) {
      // TODO: Implement ability to specify interactive/non-interactive auth mode for clients
      DirectoryEntryConsumer handler = createConsumer(branchesUrl, result);
      vcs.getFactory(target).createBrowseClient().list(target, SVNRevision.HEAD, Depth.IMMEDIATES, handler);
    }
    else {
      ISVNDirEntryHandler handler = createHandler(branchesUrl, result);
      SVNLogClient client = vcs.getSvnKitManager().createLogClient(configuration.getPassiveAuthenticationManager(myProject));
      client
        .doList(target.getURL(), target.getPegRevision(), SVNRevision.HEAD, false, SVNDepth.IMMEDIATES, SVNDirEntry.DIRENT_ALL, handler);
    }

    Collections.sort(result);
    return result;
  }

  private void showError(Exception e) {
    // already logged inside
    if (InfoReliability.setByUser.equals(myInfoReliability)) {
      VcsBalloonProblemNotifier.showOverChangesView(myProject, "Branches load error: " + e.getMessage(), MessageType.ERROR);
    }
  }

  @NotNull
  private static ISVNDirEntryHandler createHandler(@NotNull final SVNURL branchesUrl, @NotNull final List<SvnBranchItem> result) {
    return new ISVNDirEntryHandler() {
      public void handleDirEntry(final SVNDirEntry dirEntry) throws SVNException {
        // TODO: Remove equality check with branchesUrl when SVNLogClient will not be used directly, but rather through BrowseClient.
        if (!branchesUrl.equals(dirEntry.getURL()) && dirEntry.getDate() != null) {
          result.add(new SvnBranchItem(dirEntry.getURL().toDecodedString(), dirEntry.getDate(), dirEntry.getRevision()));
        }
      }
    };
  }

  @NotNull
  private static DirectoryEntryConsumer createConsumer(@NotNull final SVNURL branchesUrl, @NotNull final List<SvnBranchItem> result) {
    return new DirectoryEntryConsumer() {

      @Override
      public void consume(final DirectoryEntry entry) throws SVNException {
        // TODO: Remove equality check with branchesUrl when SVNLogClient will not be used directly, but rather through BrowseClient.
        if (!branchesUrl.equals(entry.getUrl()) && entry.getDate() != null) {
          result.add(new SvnBranchItem(entry.getUrl().toDecodedString(), entry.getDate(), entry.getRevision()));
        }
      }
    };
  }
}
