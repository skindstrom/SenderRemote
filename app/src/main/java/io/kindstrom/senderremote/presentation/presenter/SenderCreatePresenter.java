package io.kindstrom.senderremote.presentation.presenter;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import io.kindstrom.senderremote.domain.interactor.GetDefaultCommandsInteractor;
import io.kindstrom.senderremote.domain.interactor.GetGroupsInteractor;
import io.kindstrom.senderremote.domain.interactor.ReceiveResponseInteractor;
import io.kindstrom.senderremote.domain.interactor.SendCommandInteractor;
import io.kindstrom.senderremote.domain.interactor.factory.CreateSenderInteractorFactory;
import io.kindstrom.senderremote.domain.model.Command;
import io.kindstrom.senderremote.domain.model.Group;
import io.kindstrom.senderremote.domain.model.Pin;
import io.kindstrom.senderremote.domain.model.Port;
import io.kindstrom.senderremote.domain.model.Response;
import io.kindstrom.senderremote.domain.model.Sender;
import io.kindstrom.senderremote.domain.model.command.StatusCommand;
import io.kindstrom.senderremote.presentation.internal.di.PerActivity;
import io.kindstrom.senderremote.presentation.util.PermissionHandler;
import io.kindstrom.senderremote.presentation.view.SenderCreateView;

@PerActivity
public class SenderCreatePresenter implements Presenter<SenderCreateView>, PermissionPresenter {
    private final int fromGroupId;
    private final GetGroupsInteractor getGroupsInteractor;
    private final CreateSenderInteractorFactory createSenderInteractorFactory;
    private final GetDefaultCommandsInteractor getDefaultCommandsInteractor;
    private final SendCommandInteractor sendCommandInteractor;
    private final ReceiveResponseInteractor receiveResponseInteractor;
    private SenderCreateView view;

    @Inject
    public SenderCreatePresenter(@Named("groupId") int fromGroupId,
                                 GetGroupsInteractor getGroupsInteractor,
                                 CreateSenderInteractorFactory createSenderInteractorFactory, GetDefaultCommandsInteractor getDefaultCommandsInteractor, SendCommandInteractor sendCommandInteractor, ReceiveResponseInteractor receiveResponseInteractor) {
        this.fromGroupId = fromGroupId;
        this.getGroupsInteractor = getGroupsInteractor;
        this.createSenderInteractorFactory = createSenderInteractorFactory;
        this.getDefaultCommandsInteractor = getDefaultCommandsInteractor;
        this.sendCommandInteractor = sendCommandInteractor;
        this.receiveResponseInteractor = receiveResponseInteractor;

        // start listening right away
        receiveResponseInteractor.execute(this::responseReceived);
    }

    private void responseReceived(Response response) {
        view.showPortNamingView(countInputs(response), countOutputs(response));
    }

    private int countInputs(Response response) {
        return countOccurenceInString("IN", response.getResponse());
    }

    private int countOutputs(Response response) {
        return countOccurenceInString("OUT", response.getResponse());
    }

    private int countOccurenceInString(String pattern, String target) {
        return (target.length() - target.replaceAll(pattern, "").length()) / pattern.length();
    }

    @Override
    public void attach(SenderCreateView view) {
        this.view = view;

        List<Group> groups = getGroupsInteractor.execute();
        view.showGroups(groups);
        selectGroup(groups);
    }

    private void selectGroup(List<Group> groups) {
        Group selectGroup = null;
        for (Group g : groups) {
            if (g.getId() == fromGroupId) {
                selectGroup = g;
                break;
            }
        }

        if (selectGroup != null) {
            view.selectGroup(selectGroup);
        }
    }

    @Override
    public void detach() {
        sendCommandInteractor.unsubscribe();
        receiveResponseInteractor.unsubscribe();
        view = null;
    }

    public void createSenderClicked() {
        boolean hasError = false;
        String name = view.getName();
        if (name.isEmpty()) {
            view.showErrorName();
            hasError = true;
        }

        String number = view.getNumber();
        if (number.isEmpty()) {
            view.showErrorNumber();
            hasError = true;
        }

        String pin = view.getPin();
        if (pin.isEmpty()) {
            view.showErrorPinNeeded();
            hasError = true;
        } else if (Pin.create(pin) == null) {
            view.showErrorPinTooShort();
            hasError = true;
        }

        if (!hasError) {
            if (PermissionHandler.hasPermissions(view.getActivity())) {
                sendStatusCommand(Pin.create(pin));
            } else if (PermissionHandler.shouldShowRationale(view.getActivity())) {
                view.showRationale();
            } else {
                PermissionHandler.requestPermissions(view.getActivity());
            }
        }
    }

    private void sendStatusCommand(Pin pin) {
        StatusCommand command = new StatusCommand(-1, "", "");
        sendCommandInteractor.execute((state) -> {
        }, command.commandString(pin));
    }

    public void portNamesReceived(@NonNull String[] inputNames, @NonNull String[] outputNames) {
        String name = view.getName();
        String number = view.getNumber();
        String pin = view.getPin();
        List<Integer> groupIds = view.getGroups();

        List<Port> inputs = createPortFromNames(inputNames);
        List<Port> outputs = createPortFromNames(outputNames);

        List<Command> defaultCommands = getDefaultCommandsInteractor.execute();

        createSender(new Sender(-1, name, number, Pin.create(pin), inputs, outputs, defaultCommands), groupIds);
    }

    private void createSender(Sender sender, List<Integer> groupIds) {
        createSenderInteractorFactory.create(sender, groupIds).execute();

        view.returnToPreviousView();
    }

    private List<Port> createPortFromNames(String[] names) {
        List<Port> ports = new ArrayList<>();
        for (int i = 0; i < names.length; ++i) {
            ports.add(new Port(-1, i + 1, names[i]));
        }
        return ports;
    }

    @Override
    public void rationaleAccepted() {
        PermissionHandler.requestPermissions(view.getActivity());
    }

    @Override
    public void permissionAccepted() {
        sendStatusCommand(Pin.create(view.getPin()));
    }
}
