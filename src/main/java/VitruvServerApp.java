package src.main.java;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Functions;
import tools.vitruv.change.interaction.InteractionResultProvider;
import tools.vitruv.change.interaction.InternalUserInteractor;
import tools.vitruv.change.interaction.UserInteractionListener;
import tools.vitruv.change.interaction.builder.*;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.views.*;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import static tools.vitruv.framework.views.ViewTypeFactory.createIdentityMappingViewType;

public class VitruvServerApp {

    public static void main(String[] args) throws IOException {
        System.out.println("Server startet...");

        var server = new VitruvServer(() -> {
            var vsum = new VirtualModelBuilder();

            /* init vsum here */
            Path pathDir = Path.of("StorageFolder");
            vsum.withStorageFolder(pathDir);

            InternalUserInteractor userInteractor = getInternalUserInteractor();
            vsum.withUserInteractor(userInteractor);

            // testing
            vsum.withViewType(createIdentityMappingViewType("MyViewTypeBob"));

            return vsum.buildAndInitialize();
        });
        server.start();


        System.out.println("Vitruv Server gestartet!");
    }


    // TODO
    private static InternalUserInteractor getInternalUserInteractor() {
        return new InternalUserInteractor() {
            @Override
            public NotificationInteractionBuilder getNotificationDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public ConfirmationInteractionBuilder getConfirmationDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public TextInputInteractionBuilder getTextInputDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public MultipleChoiceSingleSelectionInteractionBuilder getSingleSelectionDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public MultipleChoiceMultiSelectionInteractionBuilder getMultiSelectionDialogBuilder() {
                System.out.println("xx");
                return null;
            }

            @Override
            public void registerUserInputListener(UserInteractionListener userInteractionListener) {
                System.out.println("xx");
            }

            @Override
            public void deregisterUserInputListener(UserInteractionListener userInteractionListener) {
                System.out.println("xx");
            }

            @Override
            public AutoCloseable replaceUserInteractionResultProvider(Functions.Function1<? super InteractionResultProvider, ? extends InteractionResultProvider> function1) {
                System.out.println("xx");
                return null;
            }
        };
    }

    private static ViewType<ViewSelector> getNewViewType() {
        return new ViewType<>() {
            @Override
            public String getName() {
                return "ViewTypeNameBob";
            }

            @Override
            public ViewSelector createSelector(ChangeableViewSource viewSource) {
                return new ViewSelector() {
                    @Override
                    public View createView() {
                        return null;
                    }

                    @Override
                    public boolean isValid() {
                        return true;
                    }

                    @Override
                    public ViewSelection getSelection() {
                        return null;
                    }

                    @Override
                    public Collection<EObject> getSelectableElements() {
                        return null;
                    }

                    @Override
                    public boolean isSelected(EObject eObject) {
                        return false;
                    }

                    @Override
                    public boolean isSelectable(EObject eObject) {
                        return false;
                    }

                    @Override
                    public void setSelected(EObject eObject, boolean selected) {

                    }

                    @Override
                    public boolean isViewObjectSelected(EObject eObject) {
                        return false;
                    }
                };
            }
        };
    }

}
