package saros.project;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import saros.activities.FileActivity;
import saros.activities.IActivity;
import saros.activities.SPath;
import saros.editor.EditorManager;
import saros.filesystem.ResourceAdapterFactory;
import saros.repackaged.picocontainer.Startable;
import saros.session.AbstractActivityConsumer;
import saros.session.ISarosSession;
import saros.util.FileUtils;

public class FileActivityConsumer extends AbstractActivityConsumer implements Startable {

  private static final Logger log = Logger.getLogger(FileActivityConsumer.class);

  private final ISarosSession session;
  private final SharedResourcesManager resourceChangeListener;
  private final EditorManager editorManager;

  public FileActivityConsumer(
      final ISarosSession session,
      final SharedResourcesManager resourceChangeListener,
      final EditorManager editorManager) {

    this.session = session;
    this.resourceChangeListener = resourceChangeListener;
    this.editorManager = editorManager;
  }

  @Override
  public void start() {
    session.addActivityConsumer(this, Priority.ACTIVE);
  }

  @Override
  public void stop() {
    session.removeActivityConsumer(this);
  }

  @Override
  public void exec(IActivity activity) {
    if (!(activity instanceof FileActivity)) return;

    try {
      if (log.isTraceEnabled()) log.trace("executing file activity: " + activity);

      resourceChangeListener.suspend();
      super.exec(activity);
    } finally {
      resourceChangeListener.resume();
    }
  }

  @Override
  public void receive(FileActivity activity) {
    try {
      handleFileActivity(activity);

    } catch (CoreException
        | IOException
        | UnsupportedCharsetException
        | IllegalCharsetNameException e) {

      log.error("failed to execute file activity: " + activity, e);
    }
  }

  private void handleFileActivity(FileActivity activity)
      throws CoreException, IOException, IllegalCharsetNameException, UnsupportedCharsetException {

    if (activity.isRecovery()) {
      handleFileRecovery(activity);
      return;
    }

    // TODO check if we should open / close existing editors here too
    switch (activity.getType()) {
      case CREATED:
        handleFileCreation(activity);
        break;
      case REMOVED:
        handleFileDeletion(activity);
        break;
      case MOVED:
        handleFileMove(activity);
        break;
    }
  }

  private void handleFileRecovery(FileActivity activity)
      throws CoreException, IOException, IllegalCharsetNameException, UnsupportedCharsetException {

    SPath path = activity.getPath();

    log.debug("performing recovery for file: " + activity.getPath().getFullPath());

    /*
     * We have to save the editor or otherwise the internal buffer is not
     * flushed and so replacing the file on disk will have NO impact on the
     * actual content !
     */
    editorManager.saveLazy(path);

    boolean editorWasOpen = editorManager.isOpenEditor(path);

    if (editorWasOpen) editorManager.closeEditor(path);

    FileActivity.Type type = activity.getType();

    try {
      if (type == FileActivity.Type.CREATED) handleFileCreation(activity);
      else if (type == FileActivity.Type.REMOVED) handleFileDeletion(activity);
      else log.warn("performing recovery for type " + type + " is not supported");
    } finally {

      // TODO why does Jupiter not process the activities by itself ?

      /*
       * always reset Jupiter algorithm, because upon receiving that
       * activity, it was already reset on the host side
       */
      session.getConcurrentDocumentClient().reset(path);
    }

    if (editorWasOpen && type != FileActivity.Type.REMOVED) editorManager.openEditor(path, true);
  }

  private void handleFileMove(FileActivity activity)
      throws CoreException, IOException, IllegalCharsetNameException, UnsupportedCharsetException {

    final IFile fileDestination = toEclipseIFile(activity.getPath().getFile());

    final IFile fileToMove = toEclipseIFile(activity.getOldPath().getFile());

    FileUtils.mkdirs(fileDestination);

    FileUtils.move(fileDestination.getFullPath(), fileToMove);

    if (activity.getContent() == null) return;

    handleFileCreation(activity);
  }

  private void handleFileDeletion(FileActivity activity) throws CoreException {
    SPath path = activity.getPath();

    editorManager.closeEditor(path, false);

    final IFile file = toEclipseIFile(path.getFile());

    if (file.exists()) FileUtils.delete(file);
    else log.warn("could not delete file " + file + " because it does not exist");
  }

  private void handleFileCreation(FileActivity activity)
      throws CoreException, IOException, IllegalCharsetNameException, UnsupportedCharsetException {

    saros.filesystem.IFile sarosFile = activity.getPath().getFile();

    final IFile file = toEclipseIFile(sarosFile);

    final String encoding = activity.getEncoding();
    final byte[] newContent = activity.getContent();

    byte[] actualContent = null;

    if (file.exists()) actualContent = FileUtils.getLocalFileContent(file);

    if (!Arrays.equals(newContent, actualContent)) {
      FileUtils.writeFile(new ByteArrayInputStream(newContent), file);
    } else {
      log.debug("FileActivity " + activity + " dropped (same content)");
    }

    if (encoding != null) sarosFile.setCharset(encoding);
  }

  private static IFile toEclipseIFile(saros.filesystem.IFile file) {
    return (IFile) ResourceAdapterFactory.convertBack(file);
  }
}
