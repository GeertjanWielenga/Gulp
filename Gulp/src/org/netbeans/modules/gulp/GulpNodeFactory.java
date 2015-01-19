package org.netbeans.modules.gulp;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.ExternalProcessBuilder;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

@NodeFactory.Registration(position = 5000, projectType = "org-netbeans-modules-web-clientproject")
public class GulpNodeFactory implements NodeFactory {

    private Project owner;

    @Override
    public NodeList<?> createNodes(Project prjct) {
        this.owner = prjct;
        try {
            DataObject dobj = DataObject.find(prjct.getProjectDirectory().getFileObject("gulpfile.js"));
            GulpfileNode nd = new GulpfileNode(dobj);
            return NodeFactorySupport.fixedNodeList(nd);
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return NodeFactorySupport.fixedNodeList();
    }

    public class GulpfileNode extends FilterNode {

        @StaticResource
        private static final String IMAGE = "org/netbeans/modules/"
                + "gulp/gulp.png";

        public GulpfileNode(DataObject dobj) throws DataObjectNotFoundException {
            super(dobj.getNodeDelegate(), Children.create(new GulpTaskChildFactory(dobj), true));
        }

        @Override
        public String getDisplayName() {
            return "Gulp";
        }

        @Override
        public Image getIcon(int type) {
            return ImageUtilities.loadImage(IMAGE);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return ImageUtilities.loadImage(IMAGE);
        }

    }

    private class GulpTaskChildFactory extends ChildFactory<String> {

        private final DataObject dobj;

        private GulpTaskChildFactory(DataObject dobj) {
            this.dobj = dobj;
        }

        @Override
        protected boolean createKeys(List<String> list) {
            FileObject fo = dobj.getPrimaryFile();
            try {
                List<String> lines = fo.asLines();
                for (String line : lines) {
                    if (line.startsWith("gulp.task")) {
                        int index = line.indexOf(",");
                        list.add(line.substring(0, index - 1).replace("('", " -- "));
                    }
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(final String key) {
            BeanNode node = null;
            try {
                node = new BeanNode(key) {
                    @Override
                    public Action[] getActions(boolean context) {
                        return new Action[]{new AbstractAction("Run") {
                            @Override
                            public void actionPerformed(ActionEvent e) {

                                ExternalProcessBuilder processBuilder = new ExternalProcessBuilder("gulp").
                                        //                                        addArgument("run").
                                        //                                        addArgument("-m").
                                        //                                        addArgument(namespaceName + "/" + methodName).
                                        workingDirectory(FileUtil.toFile(owner.getProjectDirectory()));
                                ExecutionDescriptor descriptor = new ExecutionDescriptor().
                                        frontWindow(true).
                                        showProgress(true).
                                        controllable(true);
                                ExecutionService service = ExecutionService.newService(
                                        processBuilder,
                                        descriptor,
                                        key);
                                service.run();

                            }
                        }};
                    }
                };
                node.setDisplayName(key);
            } catch (IntrospectionException ex) {
                Exceptions.printStackTrace(ex);
            }
            return node;
        }

    }

}
